package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.CreateHandleResponse;
import com.rtm516.mcxboxbroadcast.core.models.SISUAuthenticationResponse;
import com.rtm516.mcxboxbroadcast.core.models.SessionRef;
import com.rtm516.mcxboxbroadcast.core.models.SocialSummaryResponse;
import com.rtm516.mcxboxbroadcast.core.models.XboxTokenInfo;
import com.rtm516.mcxboxbroadcast.core.player.Player;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public abstract class SessionManagerCore {
    private final LiveTokenManager liveTokenManager;
    private final XboxTokenManager xboxTokenManager;
    private final FriendManager friendManager;
    protected final HttpClient httpClient;
    protected final Logger logger;
    protected final Logger coreLogger;
    protected final String cache;

    protected RtaWebsocketClient rtaWebsocket;
    protected ExpandedSessionInfo sessionInfo;
    protected String lastSessionResponse;
    private final Map<String, Player> players;
    private Function<String, Player> getPlayerFunction;


    private boolean initialized = false;

    /**
     * Create an instance of SessionManager
     *
     * @param cache The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public SessionManagerCore(String cache, Logger logger) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        this.logger = logger;
        this.coreLogger = logger.prefixed("");
        this.cache = cache;

        this.liveTokenManager = new LiveTokenManager(cache, httpClient, logger);
        this.xboxTokenManager = new XboxTokenManager(cache, httpClient, logger);

        this.friendManager = new FriendManager(httpClient, logger, this);

        File directory = new File(cache);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        this.players = new ConcurrentHashMap<>();
    }

    /**
     * Get the Xbox LIVE friend manager for this session manager
     *
     * @return The friend manager
     */
    public FriendManager friendManager() {
        return friendManager;
    }

    /**
     * Get the scheduled thread pool for this session manager
     *
     * @return The scheduled thread pool
     */
    public abstract ScheduledExecutorService scheduledThread();

    /**
     * Get the session ID for this session manager
     *
     * @return The session ID
     */
    public abstract String getSessionId();

    /**
     * Get the logger for this session manager
     * @return The logger
     */
    public Logger logger() {
        return logger;
    }

    /**
     * Get the MSA token for the cached user or start the authentication process
     *
     * @return The fetched MSA token
     */
    protected String getMsaToken() {
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
    protected XboxTokenInfo getXboxToken() {
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
     * @throws SessionCreationException If the session failed to create either because it already exists or some other reason
     * @throws SessionUpdateException   If the session data couldn't be set due to some issue
     */
    public void init() throws SessionCreationException, SessionUpdateException {
        if (this.initialized) {
            throw new SessionCreationException("Already initialized!");
        }

        logger.info("Starting SessionManager...");

        // Make sure we are logged in
        XboxTokenInfo tokenInfo = getXboxToken();
        logger.info("Successfully authenticated as " + tokenInfo.gamertag() + " (" + tokenInfo.userXUID() + ")");

        logger.info("Creating Xbox LIVE session...");

        // Create the session
        createSession();

        // Update the presence
        updatePresence();

        // Let the user know we are done
        logger.info("Creation of Xbox LIVE session was successful!");

        initialized = true;
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

        // We only need a websocket for the primary session manager
        if (this.sessionInfo != null) {
            // Update the current session XUID
            this.sessionInfo.setXuid(tokenInfo.userXUID());

            // Create the RTA websocket connection
            setupWebsocket(token);

            try {
                // Wait and get the connection ID from the websocket
                String connectionId = waitForConnectionId().get();

                // Update the current session connection ID
                this.sessionInfo.setConnectionId(connectionId);
            } catch (InterruptedException | ExecutionException e) {
                throw new SessionCreationException("Unable to get connectionId for session: " + e.getMessage());
            }
        }

        // Push the session information to the session directory
        updateSession();

        // Create the session handle request
        CreateHandleRequest createHandleContent = new CreateHandleRequest(
            1,
            "activity",
            new SessionRef(
                Constants.SERVICE_CONFIG_ID,
                "MinecraftLobby",
                getSessionId()
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
            if (this.sessionInfo != null) {
                CreateHandleResponse parsedResponse = Constants.OBJECT_MAPPER.readValue(createHandleResponse.body(), CreateHandleResponse.class);
                sessionInfo.setHandleId(parsedResponse.id());
            }
        } catch (IOException | InterruptedException e) {
            throw new SessionCreationException(e.getMessage());
        }

        lastSessionResponse = createHandleResponse.body();

        // Check to make sure the handle was created
        if (createHandleResponse.statusCode() != 200 && createHandleResponse.statusCode() != 201) {
            logger.debug("Failed to create session handle '"  + lastSessionResponse + "' (" + createHandleResponse.statusCode() + ")");
            throw new SessionCreationException("Unable to create session handle, got status " + createHandleResponse.statusCode() + " trying to create");
        }
    }

    /**
     * Update the session information using the stored information
     *
     * @throws SessionUpdateException If the update fails
     */
    protected abstract void updateSession() throws SessionUpdateException;

    /**
     * The internal method for making the web request to update the session
     *
     * @param url The url to send the PUT request containing the session data
     * @param data The data to update the session with
     * @throws SessionUpdateException If the update fails
     */
    protected void updateSessionInternal(String url, Object data) throws SessionUpdateException {
        HttpRequest createSessionRequest;
        try {
            createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .PUT(HttpRequest.BodyPublishers.ofString(Constants.OBJECT_MAPPER.writeValueAsString(data)))
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

        if (createSessionResponse.statusCode() != 200 && createSessionResponse.statusCode() != 201) {
            logger.debug("Got update session response: " + createSessionResponse.body());
            throw new SessionUpdateException("Unable to update session information, got status " + createSessionResponse.statusCode() + " trying to update");
        }
    }

    /**
     * Check the connection to the websocket and if its closed re-open it and re-create the session
     * This should be called before any updates to the session otherwise they might fail
     */
    protected void checkConnection() {
        if (this.rtaWebsocket != null && !rtaWebsocket.isOpen()) {
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
    protected Future<String> waitForConnectionId() {
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
    protected void setupWebsocket(String token) {
        rtaWebsocket = new RtaWebsocketClient(token, logger);
        rtaWebsocket.connect();
    }

    /**
     * Stop the current session and close the websocket
     */
    public void shutdown() {
        if (rtaWebsocket != null) {
            rtaWebsocket.close();
        }
    }

    /**
     * Update the presence of the current user on Xbox LIVE
     */
    protected void updatePresence() {
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
        scheduledThread().schedule(this::updatePresence, heartbeatAfter, TimeUnit.SECONDS);
    }

    /**
     * Get the current follower count for the current user
     * @return The current follower count
     */
    public SocialSummaryResponse socialSummary() {
        HttpRequest socialSummaryRequest = HttpRequest.newBuilder()
            .uri(Constants.SOCIAL_SUMMARY)
            .header("Authorization", getTokenHeader())
            .GET()
            .build();


        try {
            return Constants.OBJECT_MAPPER.readValue(httpClient.send(socialSummaryRequest, HttpResponse.BodyHandlers.ofString()).body(), SocialSummaryResponse.class);
        } catch (IOException | InterruptedException e) {
            logger.error("Unable to get current friend count", e);
        }

        return new SocialSummaryResponse(-1, -1, false, false, false, false, "", -1, -1, "");
    }

    public void setGetPlayerFunction(Function<String, Player> getPlayerFunction) {
        this.getPlayerFunction = getPlayerFunction;
    }

    public Map<String, Player> getPlayers() {
        return this.players;
    }

    public Player getPlayer(String uuid) {
        if (this.players.containsKey(uuid)) {
            return this.players.get(uuid);
        }
        Player player = getPlayerFunction.apply(uuid);
        this.players.put(uuid, player);
        return player;
    }
}
