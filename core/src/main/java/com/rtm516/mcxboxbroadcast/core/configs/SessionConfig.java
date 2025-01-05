package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;

public record SessionConfig(
    @JsonProperty("update-interval") int updateInterval,
    @JsonProperty("query-server") boolean queryServer,
    @JsonProperty("web-query-fallback") boolean webQueryFallback,
    @JsonProperty("config-fallback") boolean configFallback,
    @JsonProperty("session-info") SessionInfo sessionInfo) {
}
