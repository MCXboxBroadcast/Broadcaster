package com.rtm516.mcxboxbroadcast.core;

import com.github.mizosoft.methanol.Methanol;
import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.auth.SessionStartBody;
import com.rtm516.mcxboxbroadcast.core.models.auth.SessionStartResponse;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;
import com.rtm516.mcxboxbroadcast.core.models.other.ProfileSettingsResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.SessionRef;
import com.rtm516.mcxboxbroadcast.core.models.session.SocialSummaryResponse;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import com.rtm516.mcxboxbroadcast.core.webrtc.RtcWebsocketClient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public abstract class SessionManagerCore {
    private final AuthManager authManager;
    private final FriendManager friendManager;
    protected final HttpClient httpClient;
    protected final Logger logger;
    protected final Logger coreLogger;
    private final StorageManager storageManager;
    private final NotificationManager notificationManager;
    private final GalleryManager galleryManager;

    protected RtaWebsocketClient rtaWebsocket;
    protected ExpandedSessionInfo sessionInfo;
    protected String lastSessionResponse;

    protected boolean initialized = false;
    private RtcWebsocketClient rtcWebsocket;
    private String mcToken;

    /**
     * Create an instance of SessionManager
     *
     * @param storageManager The storage manager to use for storing data
     * @param notificationManager The notification manager to use for sending messages
     * @param logger The logger to use for outputting messages
     */
    public SessionManagerCore(StorageManager storageManager, NotificationManager notificationManager, Logger logger) {
        this.httpClient = Methanol.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .requestTimeout(Duration.ofMillis(5000))
            .build();

        this.logger = logger;
        this.coreLogger = logger.prefixed("");
        this.storageManager = storageManager;
        this.notificationManager = notificationManager;

        this.authManager = new AuthManager(notificationManager, storageManager, logger);

        this.friendManager = new FriendManager(httpClient, logger, this);

        this.galleryManager = new GalleryManager(httpClient, logger, this);

        // Disable the Jitsi logger to prevent spam
        IceLoggerDisabler.disable(logger);
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
     * Get the notification manager for this session manager
     *
     * @return The notification manager
     */
    public NotificationManager notificationManager() {
        return notificationManager;
    }

    /**
     * Get the gallery manager for this session manager
     *
     * @return The gallery manager
     */
    public GalleryManager galleryManager() {
        return galleryManager;
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
     * Get the Xbox token information for the current user
     * If there is no current user then the auto process is started
     *
     * @return The information about the Xbox authentication token including the token itself
     */
    protected XboxTokenInfo getXboxToken() {
        return authManager.getXboxToken();
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

        // Check if the gamertag has been updated
        checkGamertagUpdate(tokenInfo);

        if (handleFriendship()) {
            logger.info("Waiting for friendship to be processed...");
            try {
                Thread.sleep(5000); // TODO Do a real callback not just wait
            } catch (InterruptedException e) {
                logger.error("Failed to wait for friendship to be processed", e);
            }
        }

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
     * Handle the friendship of the current user to the main session if needed
     *
     * @return True if the friendship is being handled, false otherwise
     */
    protected abstract boolean handleFriendship();

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

            mcToken = setupSession(this.sessionInfo.getDeviceId());

            // Create the RTA websocket connection
            setupRtaWebsocket();

            try {
                // Wait and get the connection ID from the websocket
                String connectionId = waitForConnectionId().get();

                // Update the current session connection ID
                this.sessionInfo.setConnectionId(connectionId);
            } catch (InterruptedException | ExecutionException e) {
                throw new SessionCreationException("Unable to get connectionId for session: " + e.getMessage());
            }

            setupRtcWebsocket(mcToken);

            try {
                // Wait for the RTC websocket to connect
                waitForRTCConnection().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new SessionCreationException("Unable to connect to WebRTC for session: " + e.getMessage());
            }
        } else {
            mcToken = setupSession(UUID.randomUUID().toString());
        }

        // Set the showcase image to the current screenshot
        File imageFile = storageManager.screenshot();
        if (imageFile.exists()) {
            logger.info("Setting showcase image");
            if (galleryManager.setShowcase(imageFile)) {
                logger.info("Successfully set showcase image");
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
                Constants.TEMPLATE_NAME,
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
                .POST(HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(createHandleContent)))
                .build();
        } catch (JsonParseException e) {
            throw new SessionCreationException("Unable to create session handle, error parsing json: " + e.getMessage());
        }

        // Read the handle response
        HttpResponse<String> createHandleResponse;
        try {
            createHandleResponse = httpClient.send(createHandleRequest, HttpResponse.BodyHandlers.ofString());
            if (this.sessionInfo != null) {
                CreateHandleResponse parsedResponse = Constants.GSON.fromJson(createHandleResponse.body(), CreateHandleResponse.class);
                sessionInfo.setHandleId(parsedResponse.id());
            }
        } catch (JsonParseException | IOException | InterruptedException e) {
            throw new SessionCreationException(e.getMessage());
        }

        lastSessionResponse = createHandleResponse.body();

        // Check to make sure the handle was created
        if (createHandleResponse.statusCode() != 200 && createHandleResponse.statusCode() != 201) {
            logger.debug("Failed to create session handle '"  + lastSessionResponse + "' (" + createHandleResponse.statusCode() + ")");
            throw new SessionCreationException("Unable to create session handle, got status " + createHandleResponse.statusCode() + " trying to create: " + createHandleResponse.body());
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
     * @return The response body from the request
     * @throws SessionUpdateException If the update fails
     */
    protected String updateSessionInternal(String url, Object data) throws SessionUpdateException {
        HttpRequest createSessionRequest;
        try {
            createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .PUT(HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(data)))
                .build();
        } catch (JsonParseException e) {
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
            throw new SessionUpdateException("Unable to update session information, got status " + createSessionResponse.statusCode() + " trying to update: " + createSessionResponse.body());
        }

        return createSessionResponse.body();
    }

    /**
     * Check the connection to the websocket and if its closed re-open it and re-create the session
     * This should be called before any updates to the session otherwise they might fail
     */
    protected void checkConnection() {
        if ((this.rtaWebsocket != null && !rtaWebsocket.isOpen()) || (this.rtcWebsocket != null && !rtcWebsocket.isOpen())) {
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
        return this.rtaWebsocket.getConnectionIdFuture();
    }

    protected Future<Void> waitForRTCConnection() {
        return this.rtcWebsocket.onOpenFuture();
    }

    protected String setupSession(String deviceId) {
        String playfabTicket = this.authManager.getPlayfabSessionTicket();

        HttpRequest request = HttpRequest.newBuilder(Constants.START_SESSION)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(SessionStartBody.create(deviceId, playfabTicket)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Unable to start session", e);
        }

        if (response.statusCode() != 200) {
            logger.debug(response.body());
            throw new IllegalStateException("Unable to start session!");
        }
        return Constants.GSON.fromJson(response.body(), SessionStartResponse.class).result().authorizationHeader();
    }

    /**
     * Setup the RTA websocket connection
     */
    protected void setupRtaWebsocket() {
        if (rtaWebsocket != null) {
            rtaWebsocket.close();
        }
        rtaWebsocket = new RtaWebsocketClient(this);
        rtaWebsocket.connect();
    }

    protected void setupRtcWebsocket(String token) {
        if (rtcWebsocket != null) {
            rtcWebsocket.close();
        }
        rtcWebsocket = new RtcWebsocketClient(token, sessionInfo, logger, scheduledThread(), this);
        rtcWebsocket.connect();
    }

    /**
     * Stop the current session and close the websocket
     */
    public void shutdown() {
        if (rtaWebsocket != null) {
            rtaWebsocket.close();
        }
        if (rtcWebsocket != null) {
            rtcWebsocket.close();
        }
        this.initialized = false;
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
            return Constants.GSON.fromJson(httpClient.send(socialSummaryRequest, HttpResponse.BodyHandlers.ofString()).body(), SocialSummaryResponse.class);
        } catch (JsonParseException | IOException | InterruptedException e) {
            logger.error("Unable to get current friend count", e);
        }

        return new SocialSummaryResponse(-1, -1, false, false, false, false, "", -1, -1, "");
    }

    /**
     * Check if the gamertag has been updated and update it if needed
     *
     * @param tokenInfo The token information to check the gamertag for
     */
    private void checkGamertagUpdate(XboxTokenInfo tokenInfo) {
        try {
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create(Constants.PROFILE_SETTINGS.formatted(tokenInfo.userXUID())))
                .header("Content-Type", "application/json")
                .header("Authorization", tokenInfo.tokenHeader())
                .header("x-xbl-contract-version", "3")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());

            ProfileSettingsResponse profileSettingsResponse = Constants.GSON.fromJson(response.body(), ProfileSettingsResponse.class);

            if (profileSettingsResponse == null) {
                logger.error("Unable to get profile settings (" + response.statusCode() + "): " + response.body());
                return;
            }

            String newGamertag = profileSettingsResponse.profileUsers().get(0).settings().get(0).value();
            if (!newGamertag.equals(tokenInfo.gamertag())) {
                logger.info("Gamertag changed from " + tokenInfo.gamertag() + " to " + newGamertag);
                authManager.updateGamertag(newGamertag);
            }
        } catch (IOException | InterruptedException | NullPointerException e) {
            logger.error("Failed to check profile settings", e);
        }
    }

    /**
     * Get the XUID of the current user
     *
     * @return The XUID of the current user
     */
    public String userXUID() {
        return getXboxToken().userXUID();
    }

    /**
     * Get the current MC token for the session
     *
     * @return The current MC token
     */
    public String getMCTokenHeader() {
        return mcToken;
    }

    /**
     * Get the storage manager for this session manager
     *
     * @return The storage manager
     */
    public StorageManager storageManager() {
        return storageManager;
    }
}
