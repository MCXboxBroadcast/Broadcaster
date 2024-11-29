package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FriendSyncConfig(
    @JsonProperty("update-interval") int updateInterval,
    @JsonProperty("auto-follow") boolean autoFollow,
    @JsonProperty("auto-unfollow") boolean autoUnfollow,
    @JsonProperty("initial-invite") boolean initialInvite) {
}

