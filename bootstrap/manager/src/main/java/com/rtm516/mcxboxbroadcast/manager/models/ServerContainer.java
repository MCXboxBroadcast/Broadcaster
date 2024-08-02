package com.rtm516.mcxboxbroadcast.manager.models;

import com.nukkitx.protocol.bedrock.BedrockPong;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.manager.ServerManager;
import com.rtm516.mcxboxbroadcast.manager.database.model.Server;
import com.rtm516.mcxboxbroadcast.manager.models.response.ServerInfoResponse;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerContainer {
    private final Server server;
    private final SessionInfo sessionInfo;
    private final ServerManager serverManager;

    private Date lastUpdated;

    public ServerContainer(ServerManager serverManager, Server server) {
        this.server = server;
        this.sessionInfo = new SessionInfo("", "", "", 0, 0, 0, server.hostname(), server.port());
        this.serverManager = serverManager;
    }

    /**
     * Get the server info
     *
     * @return the server info
     */
    public Server server() {
        return server;
    }

    /**
     * Get the session info
     *
     * @return the session info
     */
    public SessionInfo sessionInfo() {
        return sessionInfo;
    }

    /**
     * Convert the server info into a api response
     *
     * @return the server info response
     */
    public ServerInfoResponse toResponse() {
        return server.toResponse(sessionInfo, lastUpdated);
    }

    /**
     * Ping the server and update the session information
     */
    public void updateSessionInfo() {
        try {
            InetSocketAddress addressToPing = new InetSocketAddress(server.hostname(), server.port());
            BedrockPong pong = serverManager.bedrockClient().ping(addressToPing, 1500, TimeUnit.MILLISECONDS).get();

            // Update the session information
            sessionInfo.setHostName(pong.getMotd());
            sessionInfo.setWorldName(pong.getSubMotd());
            sessionInfo.setVersion(pong.getVersion());
            sessionInfo.setProtocol(pong.getProtocolVersion());
            sessionInfo.setPlayers(pong.getPlayerCount());
            sessionInfo.setMaxPlayers(pong.getMaximumPlayerCount());

            // We only want to change this if the ping is successful so users can still join
            sessionInfo.setIp(server.hostname());
            sessionInfo.setPort(server.port());
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
            // Update the last updated time
            lastUpdated = new Date();
        }
    }
}
