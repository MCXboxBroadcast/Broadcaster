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
    private int serverId;

    public Bot() {
        this(0);
    }

    public Bot(int serverId) {
        this.gamertag = "";
        this.xid = "";
        this.serverId = serverId;
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

    public int serverId() {
        return serverId;
    }

    public void serverId(int serverId) {
        this.serverId = serverId;
    }

    public BotInfoResponse toResponse(BotContainer.Status status) {
        return new BotInfoResponse(_id.toString(), gamertag, xid, status, serverId);
    }
}
