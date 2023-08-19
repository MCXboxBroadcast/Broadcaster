package com.rtm516.mcxboxbroadcast.core.models;

public record CreateHandleResponse(
    String createTime,
    Object gameTypes,
    String id,
    String inviteProtocol,
    String ownerXuid,
    SessionRef sessionRef,
    String titleId,
    String type,
    int version
) {
}
