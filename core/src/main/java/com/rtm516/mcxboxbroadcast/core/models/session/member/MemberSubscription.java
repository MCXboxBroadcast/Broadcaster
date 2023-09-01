package com.rtm516.mcxboxbroadcast.core.models.session.member;

import java.util.UUID;

public record MemberSubscription(
    String id,
    String[] changeTypes
) {
    public MemberSubscription() {
        this(UUID.randomUUID().toString(), new String[]{"everything"});
    }
}
