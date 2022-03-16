package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.HashMap;
import java.util.Map;

public final class CreateSessionRequestMember {
    public final Map<String, CreateSessionRequestMemberConstantsSystem> constants;
    public final Map<String, CreateSessionRequestMemberPropertiesSystem> properties;

    public CreateSessionRequestMember(ExpandedSessionInfo sessionInfo) {
        this.constants = new HashMap<>(){{
            put("system", new CreateSessionRequestMemberConstantsSystem(sessionInfo));
        }};
        this.properties = new HashMap<>(){{
            put("system", new CreateSessionRequestMemberPropertiesSystem(sessionInfo));
        }};
    }
}
