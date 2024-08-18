package com.rtm516.mcxboxbroadcast.core.models.session;

import com.rtm516.mcxboxbroadcast.core.Constants;

public record Connection(
    int ConnectionType,
    String HostIpAddress,
    int HostPort,
    long NetherNetId,
    long WebRTCNetworkId
) {
    public Connection(long webrtcNetworkId) {
        this(Constants.ConnectionTypeWebRTC, "", 0, webrtcNetworkId, webrtcNetworkId);
    }
}
