package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.player.Player;
import org.java_websocket.util.NamedThreadFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public class SessionManager extends SessionManagerCore {
    private final ScheduledExecutorService scheduledThreadPool;
    private final Map<String, SubSessionManager> subSessionManagers;


    /**
     * Create an instance of SessionManager
     *
     * @param cache  The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public SessionManager(String cache, Logger logger) {
        super(cache, logger.prefixed("Primary Session"));
        this.scheduledThreadPool = Executors.newScheduledThreadPool(5, new NamedThreadFactory("MCXboxBroadcast Thread"));
        this.subSessionManagers = new HashMap<>();
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
     * @param sessionInfo The session information to use
     * @throws SessionCreationException If the session failed to create either because it already exists or some other reason
     * @throws SessionUpdateException   If the session data couldn't be set due to some issue
     */
    public void init(SessionInfo sessionInfo) throws SessionCreationException, SessionUpdateException {
        // Set the internal session information based on the session info
        this.sessionInfo = new ExpandedSessionInfo("", "", sessionInfo);

        super.init();

        // Load sub-sessions from cache
        List<String> subSessions = new ArrayList<>();
        try {
            subSessions = Arrays.asList(Constants.OBJECT_MAPPER.readValue(Paths.get(cache, "sub_sessions.json").toFile(), String[].class));
        } catch (IOException ignored) { }

        // Create the sub-session manager for each sub-session
        for (String subSession : subSessions) {
            SubSessionManager subSessionManager = new SubSessionManager(subSession, this, Paths.get(cache, subSession).toString(), logger);
            subSessionManager.init();
            subSessionManagers.put(subSession, subSessionManager);
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

    @Override
    protected void updateSession() throws SessionUpdateException {
        // Make sure the websocket connection is still active
        checkConnection();

        super.updateSessionInternal(Constants.CREATE_SESSION.formatted(this.sessionInfo.getSessionId()), new CreateSessionRequest(this.sessionInfo));
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
     * Create a sub-session for the given ID
     *
     * @param id The ID of the sub-session to create
     */
    public void addSubSession(String id) {
        // Make sure we don't already have that ID
        if (subSessionManagers.containsKey(id)) {
            coreLogger.error("Sub-session already exists with that ID");
            return;
        }

        // Create the sub-session manager
        try {
            SubSessionManager subSessionManager = new SubSessionManager(id, this, Paths.get(cache, id).toString(), logger);
            subSessionManager.init();
            subSessionManagers.put(id, subSessionManager);
        } catch (SessionCreationException | SessionUpdateException e) {
            coreLogger.error("Failed to create sub-session", e);
            return;
        }

        // Update the list of sub-sessions
        try {
            Files.write(Paths.get(cache, "sub_sessions.json"), Constants.OBJECT_MAPPER.writeValueAsBytes(subSessionManagers.keySet()));
        } catch (IOException e) {
            coreLogger.error("Failed to update sub-session list", e);
        }
    }

    /**
     * Remove a sub-session for the given ID
     *
     * @param id The ID of the sub-session to remove
     */
    public void removeSubSession(String id) {
        // Make sure we have that ID
        if (!subSessionManagers.containsKey(id)) {
            coreLogger.error("Sub-session does not exist with that ID");
            return;
        }

        // Remove the sub-session manager
        subSessionManagers.get(id).shutdown();
        subSessionManagers.remove(id);

        // Delete the sub-session cache folder and its contents
        try (Stream<Path> files = Files.walk(Paths.get(cache, id))) {
            files.map(Path::toFile)
                .forEach(File::delete);
            Paths.get(cache, id).toFile().delete();
        } catch (IOException e) {
            coreLogger.error("Failed to delete sub-session cache folder", e);
        }

        // Update the list of sub-sessions
        try {
            Files.write(Paths.get(cache, "sub_sessions.json"), Constants.OBJECT_MAPPER.writeValueAsBytes(subSessionManagers.keySet()));
        } catch (IOException e) {
            coreLogger.error("Failed to update sub-session list", e);
        }

        coreLogger.info("Removed sub-session with ID " + id);
    }

    /**
     * List all sessions and their information
     */
    public void listSessions() {
        List<String> messages = new ArrayList<>();
        coreLogger.info("Loading status of sessions...");

        messages.add("Primary Session:");
        messages.add(" - Gamertag: " + getXboxToken().gamertag());
        messages.add("   Following: " + socialSummary().targetFollowingCount() + "/1000");

        if (subSessionManagers.isEmpty()) {
            messages.add("Sub-sessions:");
            for (Map.Entry<String, SubSessionManager> subSession : subSessionManagers.entrySet()) {
                messages.add(" - ID: " + subSession.getKey());
                messages.add("   Gamertag: " + subSession.getValue().getXboxToken().gamertag());
                messages.add("   Following: " + subSession.getValue().socialSummary().targetFollowingCount() + "/1000");
            }
        } else {
            messages.add("No sub-sessions");
        }

        for (String message : messages) {
            coreLogger.info(message);
        }
    }
}
