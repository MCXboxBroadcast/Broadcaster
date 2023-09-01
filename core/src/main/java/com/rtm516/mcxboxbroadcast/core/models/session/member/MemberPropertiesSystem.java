package com.rtm516.mcxboxbroadcast.core.models.session.member;

public record MemberPropertiesSystem(
    boolean active,
    String connection,
    MemberSubscription subscription
) {
}
