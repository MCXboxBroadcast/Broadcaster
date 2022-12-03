package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;

public class StandaloneConfig {
    @JsonProperty("session")
    public SessionConfig sessionConfig;
    @JsonProperty("friend-sync")
    public FriendSyncConfig friendSyncConfig;
    @JsonProperty("debug-log")
    public boolean debugLog;

    public static class SessionConfig {
        @JsonProperty("update-interval")
        public int updateInterval;
        @JsonProperty("query-server")
        public boolean queryServer;
        @JsonProperty("session-info")
        public SessionInfo sessionInfo;
    }

    public static class FriendSyncConfig {
        @JsonProperty("update-interval")
        public int updateInterval;
        @JsonProperty("auto-follow")
        public boolean autoFollow;
        @JsonProperty("auto-unfollow")
        public boolean autoUnfollow;
        @JsonProperty("auto-remove")
        public boolean autoRemove;
    }
}
