package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtensionConfig {
    @JsonProperty("remote-address")
    public String remoteAddress;

    @JsonProperty("remote-port")
    public String remotePort;

    @JsonProperty("friend-sync")
    public FriendSyncConfig friendSyncConfig;

    @JsonProperty("update-interval")
    public int updateInterval;

    @JsonProperty("whitelist-friends")
    public boolean whitelistFriends;

    public static class FriendSyncConfig {
        @JsonProperty("update-interval")
        public int updateInterval;
        @JsonProperty("auto-follow")
        public boolean autoFollow;
        @JsonProperty("auto-unfollow")
        public boolean autoUnfollow;
    }

}
