package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StandaloneConfig {
    @JsonProperty("session")
    public SessionConfig sessionConfig;
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
}
