package com.rtm516.mcxboxbroadcast.manager.models;

import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPong;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.manager.database.model.Server;
import com.rtm516.mcxboxbroadcast.manager.models.response.ServerInfoResponse;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerContainer {
    private final Server server;
    private final SessionInfo sessionInfo;
    private Date lastUpdated;

    public ServerContainer(Server server) {
        this.server = server;
        this.sessionInfo = new SessionInfo("", "", "", 0, 0, 0, server.hostname(), server.port());
    }

    public Server server() {
        return server;
    }

    public SessionInfo sessionInfo() {
        return sessionInfo;
    }

    public Date lastUpdated() {
        return lastUpdated;
    }

    public ServerInfoResponse toResponse() {
        return server.toResponse(sessionInfo, lastUpdated);
    }

    public void updateSessionInfo() {
        BedrockClient client = null;
        try {
            InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", 0);
            client = new BedrockClient(bindAddress);

            client.bind().join();

            InetSocketAddress addressToPing = new InetSocketAddress(server.hostname(), server.port());
            BedrockPong pong = client.ping(addressToPing, 1500, TimeUnit.MILLISECONDS).get();

            // Update the session information
            sessionInfo.setHostName(pong.getMotd());
            sessionInfo.setWorldName(pong.getSubMotd());
            sessionInfo.setVersion(pong.getVersion());
            sessionInfo.setProtocol(pong.getProtocolVersion());
            sessionInfo.setPlayers(pong.getPlayerCount());
            sessionInfo.setMaxPlayers(pong.getMaximumPlayerCount());
        } catch (InterruptedException | ExecutionException e) {
            // TODO Log this to some backend log?
            // TODO Make this not show unless its the first ping or happened a few times in a row
            sessionInfo.setHostName("Unable to ping server");
            sessionInfo.setWorldName("");
            sessionInfo.setVersion("");
            sessionInfo.setProtocol(0);
            sessionInfo.setPlayers(0);
            sessionInfo.setMaxPlayers(0);
        } finally {
            if (client != null) {
                client.close();
            }

            // Update the last updated time
            lastUpdated = new Date();
        }
    }
}
