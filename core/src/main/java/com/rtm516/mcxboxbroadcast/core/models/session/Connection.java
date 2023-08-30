package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.Constants;

public record Connection(
    int ConnectionType,
    String HostIpAddress,
    int HostPort,
    String RakNetGUID
) {
    public Connection(String ip, int port) {
        this(Constants.ConnectionTypeUPNP, ip, port, "");
    }
}
