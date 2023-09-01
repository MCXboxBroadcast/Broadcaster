package com.rtm516.mcxboxbroadcast.core.models.session.member;

import java.time.Instant;
import java.util.Map;

public record SessionMember(
    Instant joinTime,
    Map<String, MemberConstantsSystem> constants,
    String gamertag,
    Map<String, MemberPropertiesSystem> properties
) {
}
