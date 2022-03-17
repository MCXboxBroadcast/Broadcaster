package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;

import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import org.geysermc.common.PlatformType;
import org.geysermc.floodgate.util.Utils;
import org.geysermc.floodgate.util.WhitelistUtils;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.network.MinecraftProtocol;
import org.geysermc.geyser.session.auth.AuthType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GeyserMain implements Extension {
    Logger logger;
    SessionManager sessionManager;
    SessionInfo sessionInfo;

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logger = new GeyserLogger(this.logger());
        sessionManager = new SessionManager(this.dataFolder().toString(), logger);

        // Pull onto another thread so we don't hang the main thread
        new Thread(() -> {
            logger.info("Setting up xbox session...");

            // Taken from core Geyser code
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

            sessionInfo = new SessionInfo();
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
                logger.info("Created xbox session!");
            } catch (SessionCreationException | SessionUpdateException e) {
                logger.error("Failed to create xbox session!", e);
            }

            GeyserImpl.getInstance().getScheduledThread().scheduleWithFixedDelay(() -> {
                tick();
            }, 30, 30, TimeUnit.SECONDS);
        }).start();
    }

    private void tick() {
        try {
            sessionInfo.setPlayers(this.geyserApi().onlineConnections().size());
            sessionManager.updateSession(sessionInfo);
        } catch (SessionUpdateException e) {
            logger.error("Failed to update session information!", e);
        }

        if (GeyserImpl.getInstance().getConfig().getRemote().getAuthType() == AuthType.FLOODGATE
            && GeyserImpl.getInstance().getPlatformType() == PlatformType.SPIGOT) {
            try {
                for (String xuid : sessionManager.getXboxFriends()) {
                    if (WhitelistUtils.addPlayer(Utils.getJavaUuid(xuid), "unknown")) {
                        logger.info("Added xbox friend " + xuid + " to whitelist");
                    }
                }
            } catch (XboxFriendsException e) {
                e.printStackTrace();
            }
        }
    }
}
