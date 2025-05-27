package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionInfo {
    @JsonProperty("host-name")
    private String hostName;
    @JsonProperty("world-name")
    private String worldName;
    private int players;
    @JsonProperty("max-players")
    private int maxPlayers;
    private String ip;
    private int port;

    public SessionInfo() {
    }

    public SessionInfo(String hostName, String worldName, int players, int maxPlayers, String ip, int port) {
        this.hostName = hostName;
        this.worldName = worldName;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.ip = ip;
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getVersion() {
        return Constants.BEDROCK_CODEC.getMinecraftVersion();
    }

    public int getProtocol() {
        return Constants.BEDROCK_CODEC.getProtocolVersion();
    }

    public int getPlayers() {
        // Allows the join button on 1.21.70 to show up
        if (players <= 0) {
            return 1;
        }
        return players;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

    public int getMaxPlayers() {
        // Prevents the server from showing as full
        if (maxPlayers <= getPlayers()) {
            return getPlayers() + 1;
        }
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public SessionInfo copy() {
        return new SessionInfo(hostName, worldName, players, maxPlayers, ip, port);
    }
}
