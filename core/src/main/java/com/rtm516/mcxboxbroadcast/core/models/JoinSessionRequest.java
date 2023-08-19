package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.HashMap;
import java.util.Map;

public class JoinSessionRequest {
    public Map<String, Member> members;

    public JoinSessionRequest(ExpandedSessionInfo sessionInfo) {
        this.members = new HashMap<>() {{
            put("me", new Member(sessionInfo));
        }};
    }

    public static class Member {
        public Map<String, MemberConstantsSystem> constants;
        public Map<String, MemberPropertiesSystem> properties;

        public Member(ExpandedSessionInfo sessionInfo) {
            this.constants = new HashMap<>() {{
                put("system", new MemberConstantsSystem(sessionInfo));
            }};
            this.properties = new HashMap<>() {{
                put("system", new MemberPropertiesSystem(sessionInfo));
            }};
        }
    }

    public static class MemberConstantsSystem {
        public String xuid;
        public boolean initialize;

        public MemberConstantsSystem(ExpandedSessionInfo sessionInfo) {
            this.xuid = sessionInfo.getXuid();
            this.initialize = true;
        }
    }

    public static class MemberPropertiesSystem {
        public boolean active;
        public String connection;
        public MemberSubscription subscription;

        public MemberPropertiesSystem(ExpandedSessionInfo sessionInfo) {
            this.active = true;
            this.connection = sessionInfo.getConnectionId();
            subscription = new MemberSubscription();
        }
    }

    public static class MemberSubscription {
        public String id;
        public String[] changeTypes;

        public MemberSubscription() {
            this.id = "845CC784-7348-4A27-BCDE-C083579DD113";
            this.changeTypes = new String[]{"everything"};
        }
    }
}
