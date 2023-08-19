package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.SISUAuthenticationResponse;
import com.rtm516.mcxboxbroadcast.core.models.XboxTokenInfo;
import org.java_websocket.util.NamedThreadFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public class SessionManager {
    private final LiveTokenManager liveTokenManager;
    private final XboxTokenManager xboxTokenManager;
    private final FriendManager friendManager;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduledThreadPool;
    private final Logger logger;
    private final String cache;

    private RtaWebsocketClient rtaWebsocket;
    private ExpandedSessionInfo sessionInfo;

    private String lastSessionResponse;

    /**
     * Create an instance of SessionManager
     *
     * @param cache The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public SessionManager(String cache, Logger logger) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        this.logger = logger;
        this.cache = cache;

        this.scheduledThreadPool = Executors.newScheduledThreadPool(5, new NamedThreadFactory("MCXboxBroadcast Thread"));

        this.liveTokenManager = new LiveTokenManager(cache, httpClient, logger);
        this.xboxTokenManager = new XboxTokenManager(cache, httpClient, logger);

        this.friendManager = new FriendManager(httpClient, logger, this);

        File directory = new File(cache);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Get the Xbox LIVE friend manager for this session manager
     * @return The friend manager
     */
    public FriendManager friendManager() {
        return friendManager;
    }

    /**
     * Get the scheduled thread pool for this session manager
     * @return The scheduled thread pool
     */
    public ScheduledExecutorService scheduledThread() {
        return scheduledThreadPool;
    }

    /**
     * Get the MSA token for the cached user or start the authentication process
     *
     * @return The fetched MSA token
     */
    private String getMsaToken() {
        if (liveTokenManager.verifyTokens()) {
            return liveTokenManager.getAccessToken();
        } else {
            try {
                return liveTokenManager.authDeviceCode().get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Failed to get authentication token from device code", e);
                return "";
            }
        }
    }

    /**
     * Get the Xbox token information for the current user
     * If there is no current user then the auto process is started
     *
     * @return The information about the Xbox authentication token including the token itself
     */
    private XboxTokenInfo getXboxToken() {
        if (xboxTokenManager.verifyTokens()) {
            return xboxTokenManager.getCachedXstsToken();
        } else {
            String msaToken = getMsaToken();
            String deviceToken = xboxTokenManager.getDeviceToken();
            SISUAuthenticationResponse sisuAuthenticationResponse =  xboxTokenManager.getSISUToken(msaToken, deviceToken);
            if (sisuAuthenticationResponse == null) {
                logger.info("SISU authentication response is null, please login again");
                liveTokenManager.clearTokenCache();
                return getXboxToken();
            }
            return xboxTokenManager.getXSTSToken(sisuAuthenticationResponse);
        }
    }

    /**
     * Initialize the session manager with the given session information
     *
     * @param sessionInfo The session information to use
     * @throws SessionCreationException If the session failed to create either because it already exists or some other reason
     * @throws SessionUpdateException   If the session data couldn't be set due to some issue
     */
    public void init(SessionInfo sessionInfo) throws SessionCreationException, SessionUpdateException {
        if (this.sessionInfo != null) {
            throw new SessionCreationException("Already initialized!");
        }

        logger.info("Starting MCXboxBroadcast...");

        // Make sure we are logged in
        XboxTokenInfo tokenInfo = getXboxToken();
        logger.info("Successfully authenticated as " + tokenInfo.gamertag() + " (" + tokenInfo.userXUID() + ")");

        logger.info("Creating Xbox LIVE session...");

        // Set the internal session information based on the session info
        this.sessionInfo = new ExpandedSessionInfo("", "", sessionInfo);

        // Create the session
        createSession();

        // Update the presence
        updatePresence();

        // Let the user know we are done
        logger.info("Creation of Xbox LIVE was successful!");
    }

    /**
     * Setup a new session and its prerequisites
     *
     * @throws SessionCreationException If the initial creation of the session fails
     * @throws SessionUpdateException If the updating of the session information fails
     */
    private void createSession() throws SessionCreationException, SessionUpdateException {
        // Get the token for authentication
        XboxTokenInfo tokenInfo = getXboxToken();
        String token = tokenInfo.tokenHeader();

        // Update the current session infos XUID
        this.sessionInfo.setXuid(tokenInfo.userXUID());

        // Create the RTA websocket connection
        setupWebsocket(token);

        // Wait and get the connection ID from the websocket
        String connectionId;
        try {
            connectionId = waitForConnectionId().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SessionCreationException("Unable to get connectionId for session: " + e.getMessage());
        }

        // Update the current session infos connection ID
        this.sessionInfo.setConnectionId(connectionId);

        // Push the session information to the session directory
        updateSession();

        // Create the session handle request
        CreateHandleRequest createHandleContent = new CreateHandleRequest(
            1,
            "activity",
            new CreateHandleRequest.SessionRef(
                Constants.SERVICE_CONFIG_ID,
                "MinecraftLobby",
                this.sessionInfo.getSessionId()
            )
        );

        // Make the request to create the session handle
        HttpRequest createHandleRequest;
        try {
            createHandleRequest = HttpRequest.newBuilder()
                .uri(Constants.CREATE_HANDLE)
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .header("x-xbl-contract-version", "107")
                .POST(HttpRequest.BodyPublishers.ofString(Constants.OBJECT_MAPPER.writeValueAsString(createHandleContent)))
                .build();
        } catch (JsonProcessingException e) {
            throw new SessionCreationException("Unable to create session handle, error parsing json: " + e.getMessage());
        }

        // Read the handle response
        HttpResponse<String> createHandleResponse;
        try {
            createHandleResponse = httpClient.send(createHandleRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new SessionCreationException(e.getMessage());
        }

        // Check to make sure the handle was created
        if (createHandleResponse.statusCode() != 200 && createHandleResponse.statusCode() != 201) {
            logger.debug("Failed to create session handle '"  + createHandleResponse.body() + "' (" + createHandleResponse.statusCode() + ")");
            throw new SessionCreationException("Unable to create session handle, got status " + createHandleResponse.statusCode() + " trying to create");
        }
    }

    /**
     * Update the current session with new information
     *
     * @param sessionInfo The information to update the session with
     * @throws SessionUpdateException If the update failed
     */
    public void updateSession(SessionInfo sessionInfo) throws SessionUpdateException {
        this.sessionInfo.updateSessionInfo(sessionInfo);
        updateSession();
    }

    /**
     * Update the session information using the stored information
     *
     * @throws SessionUpdateException If the update fails
     */
    private void updateSession() throws SessionUpdateException {
        // Make sure the websocket connection is still active
        checkConnection();

        CreateSessionRequest createSessionContent = new CreateSessionRequest(this.sessionInfo);

        HttpRequest createSessionRequest;
        try {
            createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.CREATE_SESSION + this.sessionInfo.getSessionId()))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .PUT(HttpRequest.BodyPublishers.ofString(Constants.OBJECT_MAPPER.writeValueAsString(createSessionContent)))
                .build();
        } catch (JsonProcessingException e) {
            throw new SessionUpdateException("Unable to update session information, error parsing json: " + e.getMessage());
        }

        HttpResponse<String> createSessionResponse;
        try {
            createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new SessionUpdateException(e.getMessage());
        }

        lastSessionResponse = createSessionResponse.body();

        if (createSessionResponse.statusCode() != 200 && createSessionResponse.statusCode() != 201) {
            logger.debug("Got session response: " + lastSessionResponse);
            throw new SessionUpdateException("Unable to update session information, got status " + createSessionResponse.statusCode() + " trying to update");
        }
    }

    /**
     * Check the connection to the websocket and if its closed re-open it and re-create the session
     * This should be called before any updates to the session otherwise they might fail
     */
    public void checkConnection() {
        if (!rtaWebsocket.isOpen()) {
            try {
                logger.info("Connection to websocket lost, re-creating session...");
                createSession();
                logger.info("Re-connected!");
            } catch (SessionCreationException | SessionUpdateException e) {
                logger.error("Session is dead and hit exception trying to re-create it", e);
            }
        }
    }

    /**
     * Use the data in the cache to get the Xbox authentication header
     *
     * @return The formatted XBL3.0 authentication header
     */
    public String getTokenHeader() {
        return getXboxToken().tokenHeader();
    }

    /**
     * Wait for the RTA websocket to receive a connection ID
     *
     * @return The received connection ID
     */
    private Future<String> waitForConnectionId() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            while (rtaWebsocket.getConnectionId() == null) {
                Thread.sleep(100);
            }
            completableFuture.complete(rtaWebsocket.getConnectionId());

            return null;
        });

        return completableFuture;
    }

    /**
     * Setup the RTA websocket connection
     *
     * @param token The authentication token to use
     */
    private void setupWebsocket(String token) {
        rtaWebsocket = new RtaWebsocketClient(token, logger);
        rtaWebsocket.connect();
    }

    /**
     * Stop the current session and close the websocket
     */
    public void shutdown() {
        rtaWebsocket.close();
        this.sessionInfo.setSessionId(null);
        scheduledThreadPool.shutdown();
    }

    /**
     * Dump the current and last session responses to json files
     */
    public void dumpSession() {
        logger.info("Dumping current and last session responses");
        try {
            FileWriter file = new FileWriter(this.cache + "/lastSessionResponse.json");
            file.write(lastSessionResponse);
            file.close();
        } catch (IOException e) {
            logger.error("Error dumping last session: " + e.getMessage());
        }

        HttpRequest createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.CREATE_SESSION + this.sessionInfo.getSessionId()))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .GET()
                .build();

        try {
            HttpResponse<String> createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());

            FileWriter file = new FileWriter(this.cache + "/currentSessionResponse.json");
            file.write(createSessionResponse.body());
            file.close();
        } catch (IOException | InterruptedException e) {
            logger.error("Error dumping current session: " + e.getMessage());
        }

        logger.info("Dumped session responses to 'lastSessionResponse.json' and 'currentSessionResponse.json'");
    }

    /**
     * Update the presence of the current user on Xbox LIVE
     */
    private void updatePresence() {
        HttpRequest updatePresenceRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.USER_PRESENCE.formatted(getXboxToken().userXUID())))
            .header("Content-Type", "application/json")
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "3")
            .POST(HttpRequest.BodyPublishers.ofString("{\"state\": \"active\"}"))
            .build();

        int heartbeatAfter = 300;
        try {
            HttpResponse<Void> updatePresenceResponse = httpClient.send(updatePresenceRequest, HttpResponse.BodyHandlers.discarding());

            if (updatePresenceResponse.statusCode() != 200) {
                logger.error("Failed to update presence, got status " + updatePresenceResponse.statusCode());
            } else {
                // Read X-Heartbeat-After header to get the next time we should update presence
                try {
                    heartbeatAfter = Integer.parseInt(updatePresenceResponse.headers().firstValue("X-Heartbeat-After").orElse("300"));
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse heartbeat after header, using default of 300");
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to update presence", e);
        }

        // Schedule the next presence update
        logger.debug("Presence update successful, scheduling presence update in " + heartbeatAfter + " seconds");
        scheduledThreadPool.schedule(this::updatePresence, heartbeatAfter, TimeUnit.SECONDS);
    }
}
