package com.rtm516.mcxboxbroadcast.manager.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.manager.models.BotContainer;
import org.bson.types.ObjectId;

public class BotInfoResponse {
    @JsonProperty
    private ObjectId id;
    @JsonProperty
    private String gamertag;
    @JsonProperty
    private String xid;
    @JsonProperty
    private BotContainer.Status status;
    @JsonProperty
    private ObjectId serverId;
    @JsonProperty
    private int friendCount;

    public BotInfoResponse(ObjectId id, String gamertag, String xid, BotContainer.Status status, ObjectId serverId, int friendCount) {
        this.id = id;
        this.gamertag = gamertag;
        this.xid = xid;
        this.status = status;
        this.serverId = serverId;
        this.friendCount = friendCount;
    }
}
