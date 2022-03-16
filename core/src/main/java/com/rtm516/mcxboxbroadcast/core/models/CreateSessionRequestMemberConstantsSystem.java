package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

public final class CreateSessionRequestMemberConstantsSystem {
    public final String xuid;
    public final boolean initialize;

    public CreateSessionRequestMemberConstantsSystem(ExpandedSessionInfo sessionInfo) {
        this.xuid = sessionInfo.getXuid();
        this.initialize = true;
    }
}
