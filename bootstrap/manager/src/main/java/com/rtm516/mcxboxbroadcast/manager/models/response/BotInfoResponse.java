package com.rtm516.mcxboxbroadcast.manager.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.manager.models.BotContainer;

public class BotInfoResponse {
    @JsonProperty
    private String id;
    @JsonProperty
    private String gamertag;
    @JsonProperty
    private String xid;
    @JsonProperty
    private BotContainer.Status status;
    @JsonProperty
    private int serverId;

    public BotInfoResponse(String id, String gamertag, String xid, BotContainer.Status status, int serverId) {
        this.id = id;
        this.gamertag = gamertag;
        this.xid = xid;
        this.status = status;
        this.serverId = serverId;
    }
}
