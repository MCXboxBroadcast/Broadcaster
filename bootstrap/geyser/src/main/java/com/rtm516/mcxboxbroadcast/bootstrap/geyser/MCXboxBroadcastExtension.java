package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import org.geysermc.geyser.api.network.AuthType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class MCXboxBroadcastExtension implements Extension {
    Logger logger;
    SessionManager sessionManager;
    SessionInfo sessionInfo;
    ExtensionConfig config;

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logger = new ExtensionLoggerImpl(this.logger());
        sessionManager = new SessionManager(this.dataFolder().toString(), logger);

        File configFile = this.dataFolder().resolve("config.yml").toFile();

        // Create the config file if it doesn't exist
        if (!configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                try (FileSystem fileSystem = this.fileSystem()) {
                    try (InputStream input = Files.newInputStream(fileSystem.getPath("config.yml"))) {
                        byte[] bytes = new byte[input.available()];

                        input.read(bytes);

                        writer.write(new String(bytes).toCharArray());

                        writer.flush();
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to create config", e);
                return;
            }
        }

        try {
            config = new ObjectMapper(new YAMLFactory()).readValue(configFile, ExtensionConfig.class);
        } catch (IOException e) {
            logger.error("Failed to load config", e);
            return;
        }

        // Pull onto another thread so we don't hang the main thread
        new Thread(() -> {
            logger.info("Setting up Xbox session...");

            // Get the ip to broadcast
            String ip = config.remoteAddress;
            if (ip.equals("auto")) {
                // Taken from core Geyser code
                ip = this.geyserApi().bedrockListener().address();
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
            }

            // Get the port to broadcast
            int port = this.geyserApi().bedrockListener().port();
            if (!config.remotePort.equals("auto")) {
                port = Integer.parseInt(config.remotePort);
            }

            // Create the session information based on the Geyser config
            sessionInfo = new SessionInfo();
            sessionInfo.setHostName(this.geyserApi().bedrockListener().primaryMotd());
            sessionInfo.setWorldName(this.geyserApi().bedrockListener().secondaryMotd());
            sessionInfo.setVersion(this.geyserApi().defaultRemoteServer().minecraftVersion());
            sessionInfo.setProtocol(this.geyserApi().defaultRemoteServer().protocolVersion());
            sessionInfo.setPlayers(this.geyserApi().onlineConnections().size());
            sessionInfo.setMaxPlayers(this.geyserApi().maxPlayers());

            sessionInfo.setIp(ip);
            sessionInfo.setPort(port);

            // Create the Xbox session
            try {
                sessionManager.createSession(sessionInfo);
                logger.info("Created Xbox session!");
            } catch (SessionCreationException | SessionUpdateException e) {
                logger.error("Failed to create xbox session!", e);
                return;
            }

            // Start the update timer
            GeyserImpl.getInstance().getScheduledThread().scheduleWithFixedDelay(this::tick, config.updateInterval, config.updateInterval, TimeUnit.SECONDS);
        }).start();
    }

    private void tick() {
        // Make sure the connection is still active
        sessionManager.checkConnection();

        // Update the player count for the session
        try {
            sessionInfo.setPlayers(this.geyserApi().onlineConnections().size());
            sessionManager.updateSession(sessionInfo);
        } catch (SessionUpdateException e) {
            logger.error("Failed to update session information!", e);
        }

        // If we are in spigot, using floodgate authentication and have the config option enabled
        // get the users friends and whitelist them
        if (this.geyserApi().defaultRemoteServer().authType() == AuthType.HYBRID
            && GeyserImpl.getInstance().getPlatformType() == PlatformType.SPIGOT
            && config.whitelistFriends) {
            try {
                for (String xuid : sessionManager.getXboxFriends()) {
                    if (WhitelistUtils.addPlayer(Utils.getJavaUuid(xuid), "unknown")) {
                        logger.info("Added xbox friend " + xuid + " to whitelist");
                    }
                }
            } catch (XboxFriendsException e) {
                logger.error("Failed to fetch xbox friends for whitelist!", e);
            }
        }
    }
}
