package com.rtm516.mcxboxbroadcast.manager.database.model;

import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.manager.models.response.ServerInfoResponse;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document("servers")
public class Server {
    @Id
    private ObjectId _id;
    private String hostname;
    private int port;

    public Server(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public ObjectId _id() {
        return _id;
    }

    public String hostname() {
        return hostname;
    }

    public void hostname(String hostname) {
        this.hostname = hostname;
    }

    public int port() {
        return port;
    }

    public void port(int port) {
        this.port = port;
    }

    public ServerInfoResponse toResponse(SessionInfo sessionInfo, Date lastUpdated) {
        return new ServerInfoResponse(_id, hostname, port, sessionInfo, lastUpdated);
    }
}
