package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StandaloneConfig(
    @JsonProperty("session") SessionConfig session,
    @JsonProperty("debug-log") boolean debugLog,
    @JsonProperty("friend-sync") FriendSyncConfig friendSync) {
}
