package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.models.session.member.SessionMember;

import java.util.Map;

public record CreateSessionResponse(
    String branch,
    int changeNumber,
    Object constants,
    int contractVersion,
    String correlationId,
    Map<String, SessionMember> members,
    Object membersInfo,
    SessionProperties properties,
    Object servers,
    String startTime
) {
}
