package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateSessionRequest {
    public Properties properties;
    public Map<String, Member> members;

    public CreateSessionRequest(ExpandedSessionInfo sessionInfo) {
        this.properties = new Properties(sessionInfo);
        this.members = new HashMap<>() {{
            put("me", new Member(sessionInfo));
        }};
    }

    public static class Properties {
        public SystemProperties system;
        public CustomProperties custom;

        public Properties(ExpandedSessionInfo sessionInfo) {
            this.system = new SystemProperties("followed", "followed", false);
            this.custom = new CustomProperties(sessionInfo);
        }
    }

    public static class SystemProperties {
        public String joinRestriction;
        public String readRestriction;
        public boolean closed;

        public SystemProperties(String joinRestriction, String readRestriction, boolean closed) {
            this.joinRestriction = joinRestriction;
            this.readRestriction = readRestriction;
            this.closed = closed;
        }
    }

    public static class CustomProperties {
        public int BroadcastSetting;
        public boolean CrossPlayDisabled;
        public String Joinability;
        public boolean LanGame;
        public int MaxMemberCount;
        public int MemberCount;
        public boolean OnlineCrossPlatformGame;
        public List<Connection> SupportedConnections;
        public int TitleId;
        public int TransportLayer;
        public String levelId;
        public String hostName;
        public String ownerId;
        public String rakNetGUID;
        public String worldName;
        public String worldType;
        public int protocol;
        public String version;

        public CustomProperties(ExpandedSessionInfo sessionInfo) {
            this.BroadcastSetting = 3;
            this.CrossPlayDisabled = false;
            this.Joinability = "joinable_by_friends";
            this.LanGame = true;
            this.MaxMemberCount = sessionInfo.getMaxPlayers();
            this.MemberCount = sessionInfo.getPlayers();
            this.OnlineCrossPlatformGame = true;
            this.SupportedConnections = Collections.singletonList(new Connection(sessionInfo.getIp(), sessionInfo.getPort()));
            this.TitleId = 0;
            this.TransportLayer = 0;
            this.levelId = "level";
            this.hostName = sessionInfo.getHostName();
            this.ownerId = sessionInfo.getXuid();
            this.rakNetGUID = sessionInfo.getRakNetGUID();
            this.worldName = sessionInfo.getWorldName();
            this.worldType = "Survival";
            this.protocol = sessionInfo.getProtocol();
            this.version = sessionInfo.getVersion();
        }
    }

    public static class Connection {
        public int ConnectionType;
        public String HostIpAddress;
        public int HostPort;
        public String RakNetGUID;

        public Connection(String ip, int port) {
            this.ConnectionType = 7;
            this.HostIpAddress = ip;
            this.HostPort = port;
            this.RakNetGUID = "";
        }
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
