package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.Collections;

public class CreateSessionRequest extends JoinSessionRequest {
    public SessionProperties properties;

    public CreateSessionRequest(ExpandedSessionInfo sessionInfo) {
        super(sessionInfo);
        this.properties = new SessionProperties(new SessionSystemProperties(), new SessionCustomProperties(
            3,
            false,
            "joinable_by_friends",
            false,
            sessionInfo.getMaxPlayers(),
            sessionInfo.getPlayers(),
            true,
            Collections.singletonList(new Connection(sessionInfo.getWebrtcNetworkId())),
            0,
            2,
            "level",
            sessionInfo.getHostName(),
            sessionInfo.getXuid(),
            "",
            sessionInfo.getWebrtcNetworkId(),
            sessionInfo.getWorldName(),
            "Survival",
            sessionInfo.getProtocol(),
            sessionInfo.getVersion(),
            false
        ));
    }
}
