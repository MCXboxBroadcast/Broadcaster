package com.rtm516.mcxboxbroadcast.core.models;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;

import java.util.Collections;
import java.util.List;

public class CreateSessionRequest extends JoinSessionRequest {
    public Properties properties;

    public CreateSessionRequest(ExpandedSessionInfo sessionInfo) {
        super(sessionInfo);
        this.properties = new Properties(sessionInfo);
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
            this.ConnectionType = Constants.ConnectionTypeUPNP;
            this.HostIpAddress = ip;
            this.HostPort = port;
            this.RakNetGUID = "";
        }
    }
}
