package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionInfo {
    @JsonProperty("host-name")
    private String hostName;
    @JsonProperty("world-name")
    private String worldName;
    private String version;
    private int protocol;
    private int players;
    @JsonProperty("max-players")
    private int maxPlayers;
    private String ip;
    private int port;

    public SessionInfo() {
    }

    public SessionInfo(String hostName, String worldName, String version, int protocol, int players, int maxPlayers, String ip, int port) {
        this.hostName = hostName;
        this.worldName = worldName;
        this.version = version;
        this.protocol = protocol;
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
        return version;
    }

    public void setVersion(String version) {
        // Parse version codes following these examples, this is because geyser can give us lots of different version formats
        // 1.21.20
        // 1.21.20/1.21.21
        // 1.21.20 - 1.21.22
        if (version.contains("-")) {
            String[] split = version.split("-");
            version = split[split.length - 1].trim();
        } else if (version.contains("/")) {
            String[] split = version.split("/");
            version = split[split.length - 1].trim();
        }

        this.version = version;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
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
        return new SessionInfo(hostName, worldName, version, protocol, players, maxPlayers, ip, port);
    }
}
