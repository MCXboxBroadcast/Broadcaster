package com.rtm516.mcxboxbroadcast.core;

import java.util.Random;
import java.util.UUID;

public class ExpandedSessionInfo extends SessionInfo {
    private String connectionId;
    private String xuid;
    private String rakNetGUID;
    private String sessionId;

    ExpandedSessionInfo(String connectionId, String xuid, SessionInfo sessionInfo) {
        this.connectionId = connectionId;
        this.xuid = xuid;

        StringBuilder str = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            str.append(random.nextInt(10));
        }
        this.rakNetGUID = str.toString();

        this.sessionId = UUID.randomUUID().toString();

        setHostName(sessionInfo.getHostName());
        setWorldName(sessionInfo.getWorldName());
        setVersion(sessionInfo.getVersion());
        setProtocol(sessionInfo.getProtocol());
        setPlayers(sessionInfo.getPlayers());
        setMaxPlayers(sessionInfo.getMaxPlayers());
        setIp(sessionInfo.getIp());
        setPort(sessionInfo.getPort());
    }

    public void updateSessionInfo(SessionInfo sessionInfo) {
        setHostName(sessionInfo.getHostName());
        setWorldName(sessionInfo.getWorldName());
        setVersion(sessionInfo.getVersion());
        setProtocol(sessionInfo.getProtocol());
        setPlayers(sessionInfo.getPlayers());
        setMaxPlayers(sessionInfo.getMaxPlayers());
        setIp(sessionInfo.getIp());
        setPort(sessionInfo.getPort());
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getXuid() {
        return xuid;
    }

    public void setXuid(String xuid) {
        this.xuid = xuid;
    }

    public String getRakNetGUID() {
        return rakNetGUID;
    }

    public void setRakNetGUID(String rakNetGUID) {
        this.rakNetGUID = rakNetGUID;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
