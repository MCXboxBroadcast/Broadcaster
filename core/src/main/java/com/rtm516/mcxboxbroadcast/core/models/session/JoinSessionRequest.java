package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import com.rtm516.mcxboxbroadcast.core.models.session.member.MemberConstantsSystem;
import com.rtm516.mcxboxbroadcast.core.models.session.member.MemberPropertiesSystem;
import com.rtm516.mcxboxbroadcast.core.models.session.member.MemberSubscription;
import com.rtm516.mcxboxbroadcast.core.models.session.member.SessionMember;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JoinSessionRequest {
    public Map<String, SessionMember> members;

    public JoinSessionRequest(ExpandedSessionInfo sessionInfo) {
        Map<String, MemberConstantsSystem> constants = new HashMap<>() {{
            put("system", new MemberConstantsSystem(sessionInfo.getXuid(), true));
        }};
        Map<String, MemberPropertiesSystem> properties = new HashMap<>() {{
            put("system", new MemberPropertiesSystem(true, sessionInfo.getConnectionId(), new MemberSubscription()));
        }};

        this.members = new HashMap<>() {{
            put("me", new SessionMember(null, constants, null, properties));
        }};
    }
}
