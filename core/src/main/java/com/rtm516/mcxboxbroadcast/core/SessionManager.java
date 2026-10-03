package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.configs.CoreConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.friend.PeopleResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateSessionResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.SessionSystemProperties;
import com.rtm516.mcxboxbroadcast.core.models.session.member.SessionMember;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import org.java_websocket.util.NamedThreadFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public class SessionManager extends SessionManagerCore {
    private static final SecureRandom NONCE_RANDOM = new SecureRandom();

    private final ScheduledExecutorService scheduledThreadPool;
    private final ExecutorService loginExecutor;
    private final Map<String, SubSessionManager> subSessionManagers;
    private final Map<String, PendingLogin> pendingSubSessions;

    private CoreConfig.FriendSyncConfig friendSyncConfig;
    private Runnable restartCallback;

    private final Map<String, String> nonces;

    private final AtomicBoolean rotating = new AtomicBoolean(false);
    private boolean hideOldSessions;

    /**
     * Create an instance of SessionManager
     *
     * @param storageManager The storage manager to use for storing data
     * @param notificationManager The notification manager to use for sending messages
     * @param logger The logger to use for outputting messages
     */
    public SessionManager(StorageManager storageManager, NotificationManager notificationManager, Logger logger) {
        super(storageManager, notificationManager, logger.prefixed("Primary Session"));
        this.scheduledThreadPool = Executors.newScheduledThreadPool(5, new NamedThreadFactory("MCXboxBroadcast Thread"));
        // Logins block so we have a separate executor for them
        this.loginExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("MCXboxBroadcast Login"));
        this.subSessionManagers = new ConcurrentHashMap<>();
        this.pendingSubSessions = new ConcurrentHashMap<>();
        this.nonces = new ConcurrentHashMap<>();
    }

    @Override
    public ScheduledExecutorService scheduledThread() {
        return scheduledThreadPool;
    }

    @Override
    public String getSessionId() {
        return sessionInfo.getSessionId();
    }

    /**
     * Get the current session information
     *
     * @return The current session information
     */
    public ExpandedSessionInfo sessionInfo() {
        return sessionInfo;
    }

    /**
     * Initialize the session manager with the given session information
     *
     * @param sessionInfo      The session information to use
     * @param friendSyncConfig The friend sync configuration to use
     * @throws SessionCreationException If the session failed to create either because it already exists or some other reason
     * @throws SessionUpdateException   If the session data couldn't be set due to some issue
     */
    public boolean init(SessionInfo sessionInfo, CoreConfig.FriendSyncConfig friendSyncConfig) throws SessionCreationException, SessionUpdateException {
        // Set the internal session information based on the session info
        this.sessionInfo = new ExpandedSessionInfo("", "", sessionInfo);

        super.init();

        // If we failed to initialize, don't continue with the rest of the setup
        if (!this.initialized) {
            return this.initialized;
        }

        // Set up the auto friend sync
        this.friendSyncConfig = friendSyncConfig;
        friendManager().init(this.friendSyncConfig);

        // Create the sub-sessions in parallel
        for (String subSession : savedSubSessions()) {
            startSubSession(subSession, false);
        }

        if (friendSyncConfig.expiry().enabled()) {
            scheduledThreadPool.scheduleWithFixedDelay(this::expireFriends, 10, friendSyncConfig.expiry().check(), TimeUnit.SECONDS);
        }

        return this.initialized;
    }

    /**
     * A sub-session that is still logging in
     *
     * @param login The running login task
     * @param isNew If the sub-session was just added, so isn't saved yet
     */
    private record PendingLogin(Future<?> login, boolean isNew) { }

    /**
     * Log in and create a sub-session on the login executor
     * New sub-sessions are only saved once logged in, and are deleted if the login doesn't finish
     *
     * @param id The ID of the sub-session to create
     * @param isNew If the sub-session was just added, so isn't saved yet
     */
    private void startSubSession(String id, boolean isNew) {
        FutureTask<Void> login = new FutureTask<>(() -> {
            SubSessionManager subSessionManager;
            try {
                subSessionManager = new SubSessionManager(id, this, storageManager().subSession(id), notificationManager(), logger);
                subSessionManager.init();
                subSessionManager.friendManager().init(this.friendSyncConfig);
            } catch (Exception e) {
                // Cancelled by removing the sub-session or shutting down, which handle the cleanup
                if (pendingSubSessions.remove(id) == null) {
                    return;
                }

                if (isNew) {
                    logger.warn("Sub-session " + id + " didn't finish logging in, removing it");
                    deleteSubSessionCache(id);
                } else {
                    logger.error("Failed to create sub-session " + id, e);
                    // TODO Retry creation after 30s or so
                }
                return;
            }

            synchronized (pendingSubSessions) {
                // The sub-session was removed while we were finishing the login
                if (pendingSubSessions.remove(id) == null) {
                    subSessionManager.shutdown();
                    return;
                }

                subSessionManagers.put(id, subSessionManager);

                if (isNew) {
                    updateSavedSubSessions(ids -> ids.add(id));
                }
            }
        }, null);

        pendingSubSessions.put(id, new PendingLogin(login, isNew));
        loginExecutor.execute(login);
    }

    /**
     * Delete the cache for a sub-session
     *
     * @param id The ID of the sub-session
     */
    private void deleteSubSessionCache(String id) {
        try {
            storageManager().subSession(id).cleanup();
        } catch (IOException | RuntimeException e) {
            coreLogger.error("Failed to delete sub-session cache file", e);
        }
    }

    /**
     * Load the list of saved sub-session IDs from storage
     *
     * @return The saved sub-session IDs
     */
    private Set<String> savedSubSessions() {
        Set<String> ids = new LinkedHashSet<>();
        try {
            String subSessionsJson = storageManager().subSessions();
            if (!subSessionsJson.isBlank()) {
                ids.addAll(Arrays.asList(Constants.GSON.fromJson(subSessionsJson, String[].class)));
            }
        } catch (JsonParseException | IOException e) {
            coreLogger.error("Failed to load sub-session list", e);
        }
        return ids;
    }

    /**
     * Modify and save the list of saved sub-session IDs
     *
     * @param update The change to make to the list
     * @return If the update changed the list
     */
    private synchronized boolean updateSavedSubSessions(Predicate<Set<String>> update) {
        Set<String> ids = savedSubSessions();
        if (!update.test(ids)) {
            return false;
        }

        try {
            storageManager().subSessions(Constants.GSON.toJson(ids));
        } catch (JsonParseException | IOException e) {
            coreLogger.error("Failed to update sub-session list", e);
        }
        return true;
    }

    @Override
    protected boolean handleFriendship() {
        // Don't do anything as we are the main session
        return false;
    }

    /**
     * Remove friends that haven't joined in a while from all accounts
     * Joins are only seen by the primary session, so it handles expiry for the sub-sessions too
     */
    private void expireFriends() {
        // Wait until every sub-session has logged in, otherwise we can't tell which friends are our own accounts
        if (!pendingSubSessions.isEmpty()) {
            return;
        }

        List<SessionManagerCore> accounts = new ArrayList<>();
        accounts.add(this);
        accounts.addAll(subSessionManagers.values());

        // Our own accounts never join, so don't expire them
        Set<String> ownXuids = new HashSet<>();
        for (SessionManagerCore account : accounts) {
            ownXuids.add(account.getXuid());
        }

        StorageManager.PlayerHistoryStorage playerHistory = storageManager().playerHistory();
        Instant expiry = Instant.now().minusSeconds(TimeUnit.DAYS.toSeconds(friendSyncConfig.expiry().days()));

        // Find the expired players before removing any, as removing a friend clears their history
        // If any account fails we skip removing anyone, so they are removed from all accounts together on the next check
        Map<SessionManagerCore, List<PeopleResponse.Person>> friends = new HashMap<>();
        Set<String> expired = new HashSet<>();
        try {
            for (SessionManagerCore account : accounts) {
                List<PeopleResponse.Person> accountFriends = account.friendManager().get();
                friends.put(account, accountFriends);

                for (PeopleResponse.Person friend : accountFriends) {
                    // Ignore accounts managed by us and already flagged as expired accounts
                    if (ownXuids.contains(friend.xuid) || expired.contains(friend.xuid)) {
                        continue;
                    }

                    // Start tracking new friends and check if existing friends have expired
                    Instant lastSeen = playerHistory.lastSeen(friend.xuid);
                    if (lastSeen == null) {
                        playerHistory.lastSeen(friend.xuid, Instant.now());
                    } else if (lastSeen.isBefore(expiry)) {
                        expired.add(friend.xuid);
                    }
                }
            }
        } catch (XboxFriendsException | IOException e) {
            logger.error("Failed to check friends for expiry", e);
            return;
        }

        // Remove them from every account they are friends with
        friends.forEach((account, accountFriends) -> {
            for (PeopleResponse.Person friend : accountFriends) {
                if (expired.contains(friend.xuid)) {
                    account.logger.info("Removing player " + friend.gamertag + " (" + friend.xuid + ") from friends due to inactivity");
                    account.friendManager().remove(friend.xuid, friend.gamertag);
                }
            }
        });
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

    @Override
    public void updateNonces() throws SessionUpdateException {
        // Get session
        HttpRequest createSessionRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.CREATE_SESSION.formatted(this.sessionInfo.getSessionId())))
            .header("Content-Type", "application/json")
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "107")
            .GET()
            .build();

        try {
            HttpResponse<String> createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());
            CreateSessionResponse sessionResponse = Constants.GSON.fromJson(createSessionResponse.body(), CreateSessionResponse.class);

            if (sessionResponse == null) {
                throw new SessionUpdateException("Failed to get session for nonces, joining will not work: sessionResponse is null");
            }

            boolean hasChanges = false;

            // Collect active XUIDs from the session
            Set<String> activeXuids = new HashSet<>();
            for (SessionMember member : sessionResponse.members().values()) {
                activeXuids.add(member.constants().get("system").xuid());
            }

            // Remove our own xuid
            activeXuids.remove(sessionInfo.getXuid());

            // Remove stale nonces
            hasChanges = nonces.keySet().retainAll(activeXuids);

            for (String xuid : activeXuids) {
                if (!nonces.containsKey(xuid)) {
                    // Generate a nonce
                    byte[] bytes = new byte[8];
                    NONCE_RANDOM.nextBytes(bytes);
                    StringBuilder hex = new StringBuilder(16);
                    for (byte b : bytes) {
                        hex.append(String.format("%02x", b));
                    }

                    // Put the nonce
                    nonces.put(xuid, hex.toString());

                    logger.debug("Generated nonce for XUID " + xuid + ": " + hex);

                    hasChanges = true;
                }
            }

            // Only update the session properties if something changed
            if (hasChanges) {
                updateSession();
            } else {
                // Check member count even if nothing changed, so we can rotate if needed
                checkMemberCount(sessionResponse.members().size());
            }
        } catch (IOException | InterruptedException e) {
            throw new SessionUpdateException("Failed to get session for nonces, joining will not work: " + e.getMessage());
        }
    }

    @Override
    protected void updateSession() throws SessionUpdateException {
        // Make sure the websocket connection is still active
        checkConnection();

        String responseBody = super.updateSessionInternal(Constants.CREATE_SESSION.formatted(this.sessionInfo.getSessionId()), new CreateSessionRequest(this.sessionInfo, nonces));
        try {
            CreateSessionResponse sessionResponse = Constants.GSON.fromJson(responseBody, CreateSessionResponse.class);

            checkMemberCount(sessionResponse.members().size());
        } catch (JsonParseException e) {
            throw new SessionUpdateException("Failed to parse session response: " + e.getMessage());
        }
    }

    /**
     * Queue a session rotation if the session is close to the member limit
     *
     * @param members The current number of session members
     */
    private void checkMemberCount(int members) {
        if (members < Constants.ROTATE_MEMBER_COUNT || !rotating.compareAndSet(false, true)) {
            return;
        }

        logger.info("Rotating session due to " + members + "/30 members");
        scheduledThreadPool.execute(this::rotateSession);
    }

    /**
     * Swap to a new session and activity handle without recreating the websocket or NetherNet connections
     * Falls back to a full restart if this fails
     */
    private void rotateSession() {
        try {
            String oldSessionId = sessionInfo.getSessionId();
            sessionInfo.setSessionId(UUID.randomUUID().toString());

            // Nonces belong to the members of the old session
            nonces.clear();

            publishSession();

            logger.info("Rotated session " + oldSessionId + " -> " + sessionInfo.getSessionId());

            // Point the sub-sessions at the new session
            for (SubSessionManager subSessionManager : subSessionManagers.values()) {
                try {
                    subSessionManager.publishSession();
                } catch (SessionCreationException | SessionUpdateException e) {
                    logger.error("Failed to move sub-session to the new session", e);
                }
            }

            // Give players that are mid-join time to connect before closing the old session
            scheduledThreadPool.schedule(() -> closeSession(oldSessionId), Constants.ROTATE_GRACE_PERIOD.toSeconds(), TimeUnit.SECONDS);
        } catch (SessionCreationException | SessionUpdateException e) {
            logger.error("Failed to rotate session, restarting instead", e);
            restart();
        } finally {
            rotating.set(false);
        }
    }

    /**
     * Close an old session so no new members can join it, as its members still show it to their friends
     *
     * @param sessionId The id of the session to close
     */
    private void closeSession(String sessionId) {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("system", new SessionSystemProperties(true));

            if (hideOldSessions) {
                properties.put("custom", Map.of(
                    "BroadcastSetting", 1,
                    "Joinability", "invite_only"
                ));
            }

            super.updateSessionInternal(Constants.CREATE_SESSION.formatted(sessionId), Map.of("properties", properties));
            logger.debug("Closed old session " + sessionId);
        } catch (SessionUpdateException e) {
            logger.warn("Failed to close old session " + sessionId + ": " + e.getMessage());
        }
    }

    /**
     * Stop the current session and close the websocket
     */
    public void shutdown() {
        // Shutdown all sub-sessions
        for (SubSessionManager subSessionManager : subSessionManagers.values()) {
            subSessionManager.shutdown();
        }

        // Shutdown self
        super.shutdown();
        scheduledThreadPool.shutdownNow();

        // Cancel pending logins, clearing them first so cancelling isn't logged as a failure
        Map<String, PendingLogin> pendingLogins = new HashMap<>(pendingSubSessions);
        pendingSubSessions.clear();
        loginExecutor.shutdownNow();

        // New sub-sessions that didn't finish logging in were never saved, so remove their cache
        pendingLogins.forEach((id, pendingLogin) -> {
            if (pendingLogin.isNew()) {
                logger.warn("Sub-session " + id + " didn't finish logging in, removing it");
                deleteSubSessionCache(id);
            }
        });
    }

    /**
     * Dump the current and last session responses to json files
     */
    public void dumpSession() {
        // Run off the calling thread as this makes web requests
        scheduledThreadPool.execute(() -> {
            try {
                storageManager().lastSessionResponse(lastSessionResponse);
            } catch (IOException e) {
                logger.error("Error dumping last session: " + e.getMessage());
            }

            HttpRequest createSessionRequest = HttpRequest.newBuilder()
                    .uri(URI.create(Constants.CREATE_SESSION.formatted(this.sessionInfo.getSessionId())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", getTokenHeader())
                    .header("x-xbl-contract-version", "107")
                    .GET()
                    .build();

            try {
                HttpResponse<String> createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());

                storageManager().currentSessionResponse(createSessionResponse.body());
            } catch (IOException | InterruptedException e) {
                logger.error("Error dumping current session: " + e.getMessage());
            }
        });
    }

    /**
     * Create a sub-session for the given ID
     * The login happens in the background so this doesn't block the calling thread
     *
     * @param id The ID of the sub-session to create
     */
    public void addSubSession(String id) {
        // Make sure we don't already have that ID
        if (subSessionManagers.containsKey(id)) {
            coreLogger.error("Sub-session already exists with that ID");
            return;
        }

        if (pendingSubSessions.containsKey(id)) {
            coreLogger.error("Sub-session with that ID is already being created");
            return;
        }

        coreLogger.info("Starting login for sub-session " + id + "...");
        startSubSession(id, true);
    }

    /**
     * Remove a sub-session for the given ID
     * This also cancels the login if the sub-session hasn't finished logging in
     *
     * @param id The ID of the sub-session to remove
     */
    public void removeSubSession(String id) {
        PendingLogin pendingLogin;
        SubSessionManager subSessionManager;
        synchronized (pendingSubSessions) {
            pendingLogin = pendingSubSessions.remove(id);
            subSessionManager = subSessionManagers.remove(id);
        }

        // Stop the login so it doesn't keep waiting for the user to sign in
        if (pendingLogin != null) {
            pendingLogin.login().cancel(true);
        }

        // Run off the calling thread as shutting down closes connections
        scheduledThreadPool.execute(() -> {
            if (subSessionManager != null) {
                subSessionManager.shutdown();
            }

            // Also check the saved list, as a sub-session that failed to log in won't be loaded
            boolean wasSaved = updateSavedSubSessions(ids -> ids.remove(id));
            if (pendingLogin == null && subSessionManager == null && !wasSaved) {
                coreLogger.error("Sub-session does not exist with that ID");
                return;
            }

            deleteSubSessionCache(id);

            coreLogger.info("Removed sub-session with ID " + id);
        });
    }

    /**
     * List all sessions and their information
     */
    public void listSessions() {
        coreLogger.info("Loading status of sessions...");

        // Run off the calling thread as this makes web requests
        scheduledThreadPool.execute(() -> {
            List<String> messages = new ArrayList<>();

            messages.add("Primary Session:");
            messages.add(" - Gamertag: " + getGamertag());
            messages.add("   Friends: " + socialSummary().targetFriendCount() + "/" + Constants.MAX_FRIENDS);

            if (!subSessionManagers.isEmpty()) {
                messages.add("Sub-sessions: (" + subSessionManagers.size() + ")");
                for (Map.Entry<String, SubSessionManager> subSession : subSessionManagers.entrySet()) {
                    messages.add(" - ID: " + subSession.getKey());
                    messages.add("   Gamertag: " + subSession.getValue().getGamertag());
                    messages.add("   Friends: " + subSession.getValue().socialSummary().targetFriendCount() + "/" + Constants.MAX_FRIENDS);
                }
            } else {
                messages.add("No sub-sessions");
            }

            for (String message : messages) {
                coreLogger.info(message);
            }
        });
    }

    /**
     * Apply the session settings from the config
     *
     * @param sessionConfig The session config to apply
     */
    public void setSessionConfig(CoreConfig.SessionConfig sessionConfig) {
        this.hideOldSessions = sessionConfig.hideOldSessions();
    }

    /**
     * Set the callback to run when the session manager needs to be restarted
     *
     * @param restart The callback to run
     */
    public void restartCallback(Runnable restart) {
        this.restartCallback = restart;
    }

    /**
     * Restart the session manager
     */
    public void restart() {
        if (restartCallback != null) {
            restartCallback.run();
        } else {
            logger.error("No restart callback set");
        }
    }
}
