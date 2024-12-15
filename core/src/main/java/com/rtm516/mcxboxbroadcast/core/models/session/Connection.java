package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.Constants;

import java.math.BigInteger;

public record Connection(
    int ConnectionType,
    String HostIpAddress,
    int HostPort,
    BigInteger NetherNetId
) {
    public Connection(BigInteger netherNetId) {
        this(Constants.ConnectionTypeWebRTC, "", 0, netherNetId);
    }
}
