package com.rtm516.mcxboxbroadcast.core.models;

public final class CreateSessionRequestCustomPropertiesConnection {
    public final int ConnectionType;
    public final String HostIpAddress;
    public final int HostPort;
    public final String RakNetGUID;

    public CreateSessionRequestCustomPropertiesConnection(String ip, int port) {
        this.ConnectionType = 7;
        this.HostIpAddress = ip;
        this.HostPort = port;
        this.RakNetGUID = "";
    }
}
