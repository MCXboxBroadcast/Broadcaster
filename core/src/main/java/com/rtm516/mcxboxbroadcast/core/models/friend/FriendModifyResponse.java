package com.rtm516.mcxboxbroadcast.core.models.friend;

public record FriendModifyResponse(
    int code,
    String description,
    String source,
    Object traceInformation
) {
}
