package com.rtm516.mcxboxbroadcast.manager.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.types.ObjectId;

public class UserInfoResponse {
    @JsonProperty
    private ObjectId id;
    @JsonProperty
    private String username;

    public UserInfoResponse(ObjectId id, String username) {
        this.id = id;
        this.username = username;
    }
}
