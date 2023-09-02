package com.rtm516.mcxboxbroadcast.bootstrap.geyser.player.converter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EssentialsPlayer {

    @JsonProperty("timestamps")
    private Timestamps timestamps;

    public Timestamps getTimestamps() {
        return this.timestamps;
    }
}
