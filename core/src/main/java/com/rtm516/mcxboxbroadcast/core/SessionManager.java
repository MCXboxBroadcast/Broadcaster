package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.JoinSessionRequest;
import org.java_websocket.util.NamedThreadFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public class SessionManager extends SessionManagerCore {
    private final ScheduledExecutorService scheduledThreadPool;

    /**
     * Create an instance of SessionManager
     *
     * @param cache The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public SessionManager(String cache, Logger logger) {
        super(cache, logger.prefixed("Primary Session"));
        this.scheduledThreadPool = Executors.newScheduledThreadPool(5, new NamedThreadFactory("MCXboxBroadcast Thread"));
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
        super.shutdown();
        this.sessionInfo.setSessionId(null); // TODO I don't think this is needed
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
}
