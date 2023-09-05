package com.rtm516.mcxboxbroadcast.bootstrap.geyser.player.converter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Timestamps {

    @JsonProperty("logout")
    private long logout;

    public long getLogout() {
        return this.logout;
    }
}