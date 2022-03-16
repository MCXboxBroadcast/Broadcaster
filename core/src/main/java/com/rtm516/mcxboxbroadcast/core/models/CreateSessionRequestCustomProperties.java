package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.Collections;
import java.util.List;

public final class CreateSessionRequestCustomProperties {
    public final int BroadcastSetting;
    public final boolean CrossPlayDisabled;
    public final String Joinability;
    public final boolean LanGame;
    public final int MaxMemberCount;
    public final int MemberCount;
    public final boolean OnlineCrossPlatformGame;
    public final List<CreateSessionRequestCustomPropertiesConnection> SupportedConnections;
    public final int TitleId;
    public final int TransportLayer;
    public final String levelId;
    public final String hostName;
    public final String ownerId;
    public final String rakNetGUID;
    public final String worldName;
    public final String worldType;
    public final int protocol;
    public final String version;

    public CreateSessionRequestCustomProperties(ExpandedSessionInfo sessionInfo) {
        this.BroadcastSetting = 3;
        this.CrossPlayDisabled = false;
        this.Joinability = "joinable_by_friends";
        this.LanGame = true;
        this.MaxMemberCount = sessionInfo.getMaxPlayers();
        this.MemberCount = sessionInfo.getPlayers();
        this.OnlineCrossPlatformGame = true;
        this.SupportedConnections = Collections.singletonList(new CreateSessionRequestCustomPropertiesConnection(sessionInfo.getIp(), sessionInfo.getPort()));
        this.TitleId = 0;
        this.TransportLayer = 0;
        this.levelId = "level";
        this.hostName = sessionInfo.getHostName();
        this.ownerId = sessionInfo.getXuid();
        this.rakNetGUID = sessionInfo.getRakNetGUID();
        this.worldName = sessionInfo.getWorldName();
        this.worldType = "Survival";
        this.protocol = sessionInfo.getProtocol();
        this.version = sessionInfo.getVersion();
    }
}
