package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

public final class CreateSessionRequestProperties {
    public final CreateSessionRequestSystemProperties system;
    public final CreateSessionRequestCustomProperties custom;

    public CreateSessionRequestProperties(ExpandedSessionInfo sessionInfo) {
        this.system = new CreateSessionRequestSystemProperties("followed", "followed", false);
        this.custom = new CreateSessionRequestCustomProperties(sessionInfo);
    }
}
