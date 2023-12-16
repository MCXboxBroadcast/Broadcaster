package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExtensionConfig(
    @JsonProperty("remote-address") String remoteAddress,
    @JsonProperty("remote-port") String remotePort,
    @JsonProperty("update-interval") int updateInterval,
    @JsonProperty("friend-sync") FriendSyncConfig friendSync) {
}
