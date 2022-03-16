package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.HashMap;
import java.util.Map;

public final class CreateSessionRequest {
    public final CreateSessionRequestProperties properties;
    public final Map<String, CreateSessionRequestMember> members;

    public CreateSessionRequest(ExpandedSessionInfo sessionInfo) {
        this.properties = new CreateSessionRequestProperties(sessionInfo);
        this.members = new HashMap<>(){{
            put("me", new CreateSessionRequestMember(sessionInfo));
        }};
    }
}
