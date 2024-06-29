package com.rtm516.mcxboxbroadcast.manager.database.model;

import com.rtm516.mcxboxbroadcast.manager.models.BotContainer;
import com.rtm516.mcxboxbroadcast.manager.models.response.BotInfoResponse;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("bots")
public class Bot {
    @Id
    private ObjectId _id;
    private String gamertag;
    private String xid;
    private ObjectId serverId;
    private String authCache;

    public Bot(ObjectId serverId) {
        this.gamertag = "";
        this.xid = "";
        this.serverId = serverId;
        this.authCache = "";
    }

    public ObjectId _id() {
        return _id;
    }

    public String gamertag() {
        return gamertag;
    }

    public void gamertag(String gamertag) {
        this.gamertag = gamertag;
    }

    public String xid() {
        return xid;
    }

    public void xid(String xid) {
        this.xid = xid;
    }

    public ObjectId serverId() {
        return serverId;
    }

    public void serverId(ObjectId serverId) {
        this.serverId = serverId;
    }

    public String authCache() {
        return authCache;
    }

    public void authCache(String authCache) {
        this.authCache = authCache;
    }

    public BotInfoResponse toResponse(BotContainer.Status status) {
        return new BotInfoResponse(_id, gamertag, xid, status, serverId);
    }
}
