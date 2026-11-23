package com.rtm516.mcxboxbroadcast.core;

import com.github.mizosoft.methanol.Methanol;
import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.exceptions.AgeVerificationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.JoinSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.session.SessionRef;
import com.rtm516.mcxboxbroadcast.core.models.session.SocialSummaryResponse;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import com.rtm516.mcxboxbroadcast.core.nethernet.HttpRelaySignaling;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public abstract class SessionManagerCore {
    private static final Duration RTA_RENEWAL_INTERVAL = Duration.ofMinutes(90);
    private static final Duration RTA_RENEWAL_RETRY = Duration.ofMinutes(1);
    private static final long MAX_RECOVERY_DELAY_SECONDS = 60;

    private final AuthManager authManager;
    private final FriendManager friendManager;
    protected final HttpClient httpClient;
    protected final Logger logger;
    protected final Logger coreLogger;
    private final StorageManager storageManager;
    private final NotificationManager notificationManager;
    private final GalleryManager galleryManager;

    private RtaWebsocketClient rtaWebsocket;
    protected ExpandedSessionInfo sessionInfo;
    protected String lastSessionResponse;

    protected volatile boolean initialized;

    /**
     * Whether IPv6 candidates are relayed to the target server, or stripped so players connect over IPv4
     */
    protected boolean allowIpv6 = false;

    private HttpRelaySignaling signaling;
    private ScheduledFuture<?> rtaRenewal;
    private ScheduledFuture<?> recoveryRetry;
    private long recoveryAttempt;
    private long recoveryDelaySeconds = 1;
    private boolean stopped;

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
            .requestTimeout(Duration.ofMillis(Integer.getInteger("http.request.timeout", 5000)))
            .build();

        this.logger = logger;
        this.coreLogger = logger.prefixed("");
        this.storageManager = storageManager;
        this.notificationManager = notificationManager;

        this.authManager = new AuthManager(notificationManager, storageManager, logger);

        this.friendManager = new FriendManager(httpClient, logger, this);
        this.galleryManager = new GalleryManager(httpClient, logger, this);
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
     * Get the Bedrock Auth Manager for the current user.
     * Starts the auto auth process if not logged in.
     *
     * @return The authenticated BedrockAuthManager
     */
    protected BedrockAuthManager getAuthManager() {
        return authManager.getManager();
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

        // Make sure we are logged in and get info
        try {
            BedrockAuthManager manager = getAuthManager();
        } catch (AgeVerificationException e) {
            logger.error("Authentication failed due to the account requiring age verification. Please login to xbox.com and complete the age verification process, then try again.");
            logger.error("You can skip it/opt out and continue using the tool, but some features may not work correctly.");
            shutdown();
            return;
        }

        logger.info("Successfully authenticated as " + getGamertag() + " (" + getXuid() + ") with " + socialSummary().targetFriendCount() + "/" + Constants.MAX_FRIENDS + " friends");

        if (handleFriendship()) {
            logger.info("Waiting for friendship to be processed...");
            try {
                Thread.sleep(5000); // TODO Do a real callback not just wait
            } catch (InterruptedException e) {
                logger.error("Failed to wait for friendship to be processed", e);
            }
        }

        logger.info("Creating Xbox LIVE session...");

        synchronized (this) {
            if (stopped) {
                return;
            }
            if (initialized) {
                throw new SessionCreationException("Already initialized!");
            }

            try {
                createSession();
                updatePresence();
            } catch (SessionCreationException | SessionUpdateException | RuntimeException e) {
                closeRtaWebsocket();
                shutdownNetherNet();
                throw e;
            }

            logger.info("Creation of Xbox LIVE session was successful!");
            initialized = true;
            scheduleRtaRenewal(RTA_RENEWAL_INTERVAL);
            authManager.setOnDeviceTokenRefreshCallback(() -> executeSessionTask(this::refreshSession));
        }
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
        cancelRtaRenewal();
        // We only need a websocket for the primary session manager
        if (this.sessionInfo != null) {
            // Update the current session XUID
            this.sessionInfo.setXuid(getXuid());

            closeRtaWebsocket();
            rtaWebsocket = connectRtaWebsocket();
            this.sessionInfo.setConnectionId(rtaWebsocket.getConnectionId());

            setupNetherNet();

            if (this.signaling == null || !this.signaling.isActive()) {
                throw new SessionCreationException("Unable to start NetherNet signaling");
            }
        }

        // Set the showcase image to the current screenshot
        File imageFile = storageManager.screenshot();
        if (imageFile.exists()) {
            logger.info("Setting showcase image");
            if (galleryManager.setShowcase(imageFile)) {
                logger.info("Successfully set showcase image");
            }
        }

        publishSession();

        onSessionCreated();
    }

    /**
     * Called after session publication or after a replacement RTA connection ID has been written
     */
    protected void onSessionCreated() {
    }

    /**
     * Push the session to the session directory and create an activity handle for it
     * This is safe to call again without recreating the websocket or NetherNet connections
     *
     * @throws SessionCreationException If the creation of the session handle fails
     * @throws SessionUpdateException If the updating of the session information fails
     */
    protected void publishSession() throws SessionCreationException, SessionUpdateException {
        // Get the token for authentication
        String token;
        try {
            token = getAuthManager().getXboxLiveXstsToken().getUpToDate().getAuthorizationHeader();
        } catch (Exception e) {
            throw new SessionCreationException("Failed to get authorization headers: " + e.getMessage());
        }

        // Push the session information to the session directory
        writeSession();

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
    protected abstract void writeSession() throws SessionUpdateException;

    /**
     * Update the nonces in the session based on the current players
     *
     * @throws SessionUpdateException If the update fails
     */
    public void updateNonces() throws SessionUpdateException {
        // Nothing by default
    }

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
            logger.info("Got update session response: " + createSessionResponse.body());
            throw new SessionUpdateException("Unable to update session information, got status " + createSessionResponse.statusCode() + " trying to update: " + createSessionResponse.body());
        }

        return createSessionResponse.body();
    }

    /**
     * Check the session connections before writing regular updates. An RTA close event usually
     * starts recovery first, but this also detects missed close events and NetherNet failures.
     */
    protected synchronized boolean checkConnection() {
        if (!initialized || recoveryRetry != null) {
            return false;
        }

        boolean rtaIsOpen = rtaWebsocket != null && rtaWebsocket.isOpen();
        boolean signalingIsOpen = signaling != null && signaling.isActive();
        if (!rtaIsOpen || !signalingIsOpen) {
            logger.warn("Session connection lost (RTA open: " + rtaIsOpen + ", NetherNet active: " + signalingIsOpen + "), re-creating session...");
            recoverSession();
            // Recovery writes the session itself, or has scheduled a retry.
            return false;
        }
        return true;
    }

    void onRtaClosed(RtaWebsocketClient websocket, int code, String reason) {
        executeSessionTask(() -> {
            synchronized (this) {
                if (!initialized || websocket != rtaWebsocket || recoveryRetry != null) {
                    return;
                }

                logger.warn("RTA WebSocket disconnected (" + code + ")"
                    + (reason.isEmpty() ? "" : ": " + reason) + ", re-creating session...");
                recoverSession();
            }
        });
    }

    void onRtaSessionChanged() {
        executeSessionTask(() -> {
            synchronized (this) {
                if (!initialized || recoveryRetry != null) {
                    return;
                }

                try {
                    // Read current membership even if RTA was renewed after the notification arrived.
                    updateNonces();
                } catch (SessionUpdateException e) {
                    logger.error("Failed to update session nonces", e);
                }
            }
        });
    }

    private void executeSessionTask(Runnable task) {
        try {
            scheduledThread().execute(task);
        } catch (RejectedExecutionException ignored) {
            // The session's executor has been shut down.
        }
    }

    private synchronized void refreshSession() {
        if (!initialized) {
            return;
        }

        try {
            logger.debug("Device token refreshed, recreating session...");
            createSession();
            connectionRestored();
            logger.debug("Session recreated after device token refresh");
        } catch (SessionCreationException | SessionUpdateException | RuntimeException e) {
            logger.error("Failed to recreate session after device token refresh", e);
            if (sessionInfo != null && recoveryRetry == null) {
                scheduleRecoveryRetry();
            }
        }
    }

    private void recoverSession() {
        try {
            createSession();
            connectionRestored();
            logger.info("WebSocket session reconnected");
        } catch (SessionCreationException | SessionUpdateException | RuntimeException e) {
            logger.error("Failed to re-create session, retrying in " + recoveryDelaySeconds + " seconds", e);
            scheduleRecoveryRetry();
        }
    }

    private void scheduleRecoveryRetry() {
        long attempt = ++recoveryAttempt;
        recoveryRetry = scheduledThread().schedule(() -> retryRecovery(attempt), recoveryDelaySeconds, TimeUnit.SECONDS);
        recoveryDelaySeconds = Math.min(recoveryDelaySeconds * 2, MAX_RECOVERY_DELAY_SECONDS);
    }

    private synchronized void retryRecovery(long attempt) {
        if (initialized && recoveryRetry != null && attempt == recoveryAttempt) {
            recoveryRetry = null;
            recoverSession();
        }
    }

    private void connectionRestored() {
        if (recoveryRetry != null) {
            recoveryRetry.cancel(false);
            recoveryRetry = null;
        }
        recoveryDelaySeconds = 1;
        scheduleRtaRenewal(RTA_RENEWAL_INTERVAL);
    }

    private void scheduleRtaRenewal(Duration delay) {
        if (rtaWebsocket != null) {
            cancelRtaRenewal();
            RtaWebsocketClient websocket = rtaWebsocket;
            rtaRenewal = scheduledThread().schedule(() -> renewRtaWebsocket(websocket), delay.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private void cancelRtaRenewal() {
        if (rtaRenewal != null) {
            rtaRenewal.cancel(false);
            rtaRenewal = null;
        }
    }

    private synchronized void renewRtaWebsocket(RtaWebsocketClient previous) {
        if (previous != rtaWebsocket || !checkConnection()) {
            return;
        }

        RtaWebsocketClient replacement = null;
        try {
            replacement = connectRtaWebsocket();
            updateSessionInternal(Constants.CREATE_SESSION.formatted(getSessionId()), new JoinSessionRequest(sessionInfo, replacement.getConnectionId()));
        } catch (SessionCreationException | SessionUpdateException | RuntimeException e) {
            if (replacement != null) {
                replacement.retire();
            }
            logger.error("Failed to renew the RTA WebSocket, retrying in one minute", e);
            scheduleRtaRenewal(RTA_RENEWAL_RETRY);
            return;
        }

        sessionInfo.setConnectionId(replacement.getConnectionId());
        rtaWebsocket = replacement;
        scheduleRtaRenewal(RTA_RENEWAL_INTERVAL);
        logger.debug("Renewed the RTA WebSocket");
        try {
            onSessionCreated();
        } finally {
            previous.retire();
        }
    }

    /**
     * Use the data in the cache to get the Xbox authentication header
     *
     * @return The formatted XBL3.0 authentication header
     */
    public String getTokenHeader() {
        try {
            return getAuthManager().getXboxLiveXstsToken().getUpToDate().getAuthorizationHeader();
        } catch (Exception e) {
            logger.error("Failed to get auth header", e);
            return "";
        }
    }

    /**
     * Open and subscribe a new RTA WebSocket before using its connection ID in a session write.
     */
    private RtaWebsocketClient connectRtaWebsocket() throws SessionCreationException {
        RtaWebsocketClient websocket = new RtaWebsocketClient(this);
        try {
            websocket.connect();
            websocket.getConnectionIdFuture().get(Constants.WEBSOCKET_CONNECTION_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            websocket.retire();
            throw new SessionCreationException("Interrupted while connecting the RTA WebSocket");
        } catch (TimeoutException e) {
            websocket.retire();
            throw new SessionCreationException("Timed out waiting for an RTA connection ID");
        } catch (ExecutionException | RuntimeException e) {
            websocket.retire();
            throw new SessionCreationException("Unable to connect the RTA WebSocket: " + e);
        }
        if (!websocket.isOpen()) {
            websocket.retire();
            throw new SessionCreationException("RTA WebSocket closed after receiving its connection ID");
        }
        return websocket;
    }

    private void closeRtaWebsocket() {
        RtaWebsocketClient websocket = rtaWebsocket;
        rtaWebsocket = null;
        if (websocket != null) {
            websocket.retire();
        }
    }

    protected void setupNetherNet() {
        shutdownNetherNet();

        long netherNetId = this.sessionInfo.getNetherNetId().longValue();

        this.signaling = new HttpRelaySignaling(netherNetId, getMCTokenHeader(), InetSocketAddress.createUnresolved(sessionInfo.getIp(), sessionInfo.getPort()), storageManager.playerHistory(), allowIpv6, logger);

        this.sessionInfo.setPmsgId(getAuthManager().getMinecraftSession().getCached().getParsedToken().getPayload().reqString("pmid"));

        try {
            this.signaling.connect(null).get(Constants.WEBSOCKET_CONNECTION_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            logger.info("NetherNet signaling started on ID: " + netherNetId);
        } catch (Exception e) {
            logger.error("Failed to start NetherNet signaling", e);
        }
    }

    /**
     * Stop the current session and close the websocket
     */
    public synchronized void shutdown() {
        stopped = true;
        initialized = false;
        cancelRtaRenewal();
        if (recoveryRetry != null) {
            recoveryRetry.cancel(false);
            recoveryRetry = null;
        }
        closeRtaWebsocket();
        shutdownNetherNet();
    }

    private void shutdownNetherNet() {
        if (signaling != null) {
            signaling.close();
            signaling = null;
        }
    }

    /**
     * Update the presence of the current user on Xbox LIVE
     */
    protected void updatePresence() {
        HttpRequest updatePresenceRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.USER_PRESENCE.formatted(getXuid())))
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
     * Get the social summary for the current user
     * @return The current social summary
     */
    public SocialSummaryResponse socialSummary() {
        HttpRequest socialSummaryRequest = HttpRequest.newBuilder()
            .uri(Constants.SOCIAL_SUMMARY)
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "3")
            .GET()
            .build();

        try {
            return Constants.GSON.fromJson(httpClient.send(socialSummaryRequest, HttpResponse.BodyHandlers.ofString()).body(), SocialSummaryResponse.class);
        } catch (JsonParseException | IOException | InterruptedException e) {
            logger.error("Unable to get current friend count", e);
        }

        return new SocialSummaryResponse(-1, -1, -1, false, false, false, false, "", -1, false, -1, -1, -1, "");
    }

    /**
     * Get the XUID of the current user
     *
     * @return The XUID of the current user
     */
    public String getXuid() {
        return authManager.getXuid();
    }

    /**
     * Get the Gamertag of the current user
     *
     * @return The Gamertag of the current user
     */
    public String getGamertag() {
        return authManager.getGamertag();
    }

    /**
     * Get the current MC token for the session
     *
     * @return The current MC token
     */
    public String getMCTokenHeader() {
        try {
            return getAuthManager().getMinecraftSession().getUpToDate().getAuthorizationHeader();
        } catch (Exception e) {
            logger.error("Failed to get MC token header", e);
            return null;
        }
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
