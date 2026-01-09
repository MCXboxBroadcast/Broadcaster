package com.rtm516.mcxboxbroadcast.core.nethernet;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;
import com.rtm516.mcxboxbroadcast.core.nethernet.initializer.NetherNetBedrockChannelInitializer;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;

public class BroadcasterChannelInitializer extends NetherNetBedrockChannelInitializer<BedrockServerSession> {

    private final SessionInfo sessionInfo;
    private final SessionManagerCore sessionManager;
    private final Logger logger;

    public BroadcasterChannelInitializer(SessionInfo sessionInfo, SessionManagerCore sessionManager, Logger logger) {
        this.sessionInfo = sessionInfo;
        this.sessionManager = sessionManager;
        this.logger = logger;
    }

    @Override
    protected BedrockServerSession createSession0(BedrockPeer peer, int subClientId) {
        return new BedrockServerSession(peer, subClientId);
    }

    @Override
    protected void initSession(BedrockServerSession session) {
        session.setLogging(true);
        session.setPacketHandler(new RedirectPacketHandler(session, sessionInfo, sessionManager, logger));
    }
}