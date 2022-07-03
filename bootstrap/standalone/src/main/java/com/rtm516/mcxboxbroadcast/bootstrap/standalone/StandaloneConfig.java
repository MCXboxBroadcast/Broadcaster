package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;

public class StandaloneConfig {
    @JsonProperty("update-interval")
    public int updateInterval;
    @JsonProperty("session-info")
    public SessionInfo sessionInfo;
}
