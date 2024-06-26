package com.rtm516.mcxboxbroadcast.manager.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import org.bson.types.ObjectId;

import java.util.Date;

public class ServerInfoResponse {
    @JsonProperty
    private final ObjectId id;
    @JsonProperty
    private String hostname;
    @JsonProperty
    private int port;
    @JsonProperty
    private final SessionInfo sessionInfo;
    @JsonProperty
    private Date lastUpdated;

    public ServerInfoResponse(ObjectId id, String hostname, int port, SessionInfo sessionInfo, Date lastUpdated) {
        this.id = id;
        this.hostname = hostname;
        this.port = port;
        this.sessionInfo = sessionInfo;
        this.lastUpdated = lastUpdated;
    }
}
