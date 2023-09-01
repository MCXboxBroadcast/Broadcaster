package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.Collections;
import java.util.List;

public record SessionCustomProperties(
    int BroadcastSetting,
    boolean CrossPlayDisabled,
    String Joinability,
    boolean LanGame,
    int MaxMemberCount,
    int MemberCount,
    boolean OnlineCrossPlatformGame,
    List<Connection>SupportedConnections,
    int TitleId,
    int TransportLayer,
    String levelId,
    String hostName,
    String ownerId,
    String rakNetGUID,
    String worldName,
    String worldType,
    int protocol,
    String version
) {
}
