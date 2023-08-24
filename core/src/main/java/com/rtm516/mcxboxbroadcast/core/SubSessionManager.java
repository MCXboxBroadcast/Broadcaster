package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.JoinSessionRequest;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public class SubSessionManager extends SessionManagerCore {
    private final SessionManager parent;

    /**
     * Create a new session manager for a sub-session
     *
     * @param id The id of the sub-session
     * @param parent The parent session manager
     * @param cache The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public SubSessionManager(String id, SessionManager parent, String cache, Logger logger) {
        super(cache, logger.prefixed("Sub-Session " + id));
        this.parent = parent;
    }

    @Override
    public ScheduledExecutorService scheduledThread() {
        return parent.scheduledThread();
    }

    @Override
    public String getSessionId() {
        return parent.sessionInfo().getSessionId();
    }

    @Override
    protected void updateSession() throws SessionUpdateException {
        super.updateSessionInternal(Constants.JOIN_SESSION.formatted(parent.sessionInfo().getHandleId()), new JoinSessionRequest(parent.sessionInfo()));
    }
}
