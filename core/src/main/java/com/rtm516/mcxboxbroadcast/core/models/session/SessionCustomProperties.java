package com.rtm516.mcxboxbroadcast.core.models.session;

import java.util.List;
import java.util.Map;

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
    String worldName,
    String worldType, // Survival, Creative, Adventure
    int protocol,
    String version,
    boolean isEditorWorld,
    boolean isHardcore, // If true then shows as hardcore
    Map<String, String> nonces
) {
}
