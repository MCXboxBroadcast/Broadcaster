package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtensionConfig {
    @JsonProperty("remote-address")
    public String remoteAddress;

    @JsonProperty("remote-port")
    public String remotePort;

    @JsonProperty("update-interval")
    public int updateInterval;

    @JsonProperty("whitelist-friends")
    public boolean whitelistFriends;
}
