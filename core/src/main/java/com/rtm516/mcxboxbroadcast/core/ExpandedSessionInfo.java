package com.rtm516.mcxboxbroadcast.core;

import java.math.BigInteger;
import java.util.Random;
import java.util.UUID;

public class ExpandedSessionInfo extends SessionInfo {
    private static final Random RANDOM = new Random();

    private String connectionId;
    private String xuid;
    private String rakNetGUID;
    private String sessionId;
    private String handleId;

    private BigInteger netherNetId;
    private String deviceId;

    public ExpandedSessionInfo(String connectionId, String xuid, SessionInfo sessionInfo) {
        this.connectionId = connectionId;
        this.xuid = xuid;
        this.rakNetGUID = "";

        this.sessionId = UUID.randomUUID().toString();
        this.netherNetId = BigInteger.valueOf(Math.abs(RANDOM.nextLong()));
        this.deviceId = UUID.randomUUID().toString();

        setHostName(sessionInfo.getHostName().isEmpty() ? "MCXboxBroadcast" : sessionInfo.getHostName());
        setWorldName(sessionInfo.getWorldName().isEmpty() ? getHostName() : sessionInfo.getWorldName());
        setPlayers(sessionInfo.getPlayers());
        setMaxPlayers(sessionInfo.getMaxPlayers());
        setIp(sessionInfo.getIp());
        setPort(sessionInfo.getPort());
    }

    public void updateSessionInfo(SessionInfo sessionInfo) {
        setHostName(sessionInfo.getHostName().isEmpty() ? "MCXboxBroadcast" : sessionInfo.getHostName());
        setWorldName(sessionInfo.getWorldName().isEmpty() ? getHostName() : sessionInfo.getWorldName());
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

    public BigInteger getNetherNetId() {
        return netherNetId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHandleId() {
        return handleId;
    }

    public void setHandleId(String handleId) {
        this.handleId = handleId;
    }
}
