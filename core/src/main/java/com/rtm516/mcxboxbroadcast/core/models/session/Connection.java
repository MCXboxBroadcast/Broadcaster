package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.Constants;

import java.math.BigInteger;

public record Connection(
    int ConnectionType,
    String HostIpAddress,
    int HostPort,
    BigInteger NetherNetId,
    String PmsgId
) {
    public Connection(BigInteger netherNetId, String pmsgId) {
        this(Constants.ConnectionTypeJsonRpc, "", 0, netherNetId, pmsgId);
    }
}
