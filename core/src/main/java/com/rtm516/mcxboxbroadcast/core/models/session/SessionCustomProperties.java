package com.rtm516.mcxboxbroadcast.core.models.session;

import java.util.List;

public record SessionCustomProperties(
    int BroadcastSetting,
    boolean CrossPlayDisabled,
    String Joinability,
    boolean LanGame,
    int MaxMemberCount,
    int MemberCount,
    boolean OnlineCrossPlatformGame,
    List<Connection> SupportedConnections,
    int TitleId,
    int TransportLayer,
    String levelId,
    String hostName,
    String ownerId,
    String rakNetGUID,
    long WebRTCNetworkId,
    String worldName,
    String worldType,
    int protocol,
    String version,
    boolean isEditorWorld
) {
}
