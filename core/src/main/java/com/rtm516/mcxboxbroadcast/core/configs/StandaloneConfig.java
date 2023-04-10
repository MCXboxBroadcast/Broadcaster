package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;

public class StandaloneConfig {
    @JsonProperty("session")
    public SessionConfig sessionConfig;
    @JsonProperty("debug-log")
    public boolean debugLog;
    @JsonProperty("friend-sync")
    public FriendSyncConfig friendSyncConfig;

    public static class SessionConfig {
        @JsonProperty("update-interval")
        public int updateInterval;
        @JsonProperty("query-server")
        public boolean queryServer;
        @JsonProperty("session-info")
        public SessionInfo sessionInfo;
    }
}
