package com.rtm516.mcxboxbroadcast.boostrap.geyser;

import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.network.MinecraftProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main implements Extension {
    @Subscribe
    public void onPostInitialize(GeyserPreInitializeEvent event) {
        this.logger().info("Setting up xbox session...");

        SessionManager sessionManager = new SessionManager(this.dataFolder().toString());

        String ip = GeyserImpl.getInstance().getConfig().getBedrock().getAddress();
        try {
            // This is the most reliable for getting the main local IP
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("geysermc.org", 80));
            ip = socket.getLocalAddress().getHostAddress();
        } catch (IOException e1) {
            try {
                // Fallback to the normal way of getting the local IP
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ignored) {
            }
        }

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setHostName(GeyserImpl.getInstance().getConfig().getBedrock().getMotd1());
        sessionInfo.setWorldName(GeyserImpl.getInstance().getConfig().getBedrock().getMotd2());
        sessionInfo.setVersion(MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getMinecraftVersion());
        sessionInfo.setProtocol(MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion());
        sessionInfo.setPlayers(this.geyserApi().onlineConnections().size());
        sessionInfo.setMaxPlayers(GeyserImpl.getInstance().getConfig().getMaxPlayers());
        sessionInfo.setIp(ip); // TODO Allow users to change this
        sessionInfo.setPort(GeyserImpl.getInstance().getConfig().getBedrock().getPort());

        try {
            sessionManager.createSession(sessionInfo);
            this.logger().info("Created xbox session!");
        } catch (Exception e) {
            this.logger().info("Failed to create xbox session!");
            e.printStackTrace();
        }
    }
}
