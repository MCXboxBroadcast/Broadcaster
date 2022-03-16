package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

public final class CreateSessionRequestMemberPropertiesSystem {
    public final boolean active;
    public final String connection;
    public final CreateSessionRequestMemberSubscription subscription;

    public CreateSessionRequestMemberPropertiesSystem(ExpandedSessionInfo sessionInfo) {
        this.active = true;
        this.connection = sessionInfo.getConnectionId();
        subscription = new CreateSessionRequestMemberSubscription();
    }
}
