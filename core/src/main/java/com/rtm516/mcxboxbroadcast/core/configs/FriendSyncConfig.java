package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FriendSyncConfig {
    @JsonProperty("update-interval")
    public int updateInterval;
    @JsonProperty("auto-follow")
    public boolean autoFollow;
    @JsonProperty("auto-unfollow")
    public boolean autoUnfollow;

}

