package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.notifications.SlackNotificationManager;
import com.rtm516.mcxboxbroadcast.core.configs.StandaloneConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.ping.PingUtil;
import com.rtm516.mcxboxbroadcast.core.storage.FileStorageManager;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StandaloneMain {
    private static StandaloneConfig config;
    private static StandaloneLoggerImpl logger;
    private static SessionInfo sessionInfo;
    private static NotificationManager notificationManager;

    public static SessionManager sessionManager;

    public static void main(String[] args) throws Exception {
        logger = new StandaloneLoggerImpl(LoggerFactory.getLogger(StandaloneMain.class));

        logger.info("Starting MCXboxBroadcast Standalone " + BuildData.VERSION);

        String configFileName = "config.yml";
        File configFile = new File(configFileName);

        // Create the config file if it doesn't exist
        if (!configFile.exists()) {
            try (FileOutputStream fos = new FileOutputStream(configFileName)) {
                try (InputStream input = StandaloneMain.class.getClassLoader().getResourceAsStream(configFileName)) {
                    byte[] bytes = new byte[input.available()];

                    //noinspection ResultOfMethodCallIgnored
                    input.read(bytes);

                    fos.write(bytes);

                    fos.flush();
                }
            } catch (IOException e) {
                logger.error("Failed to create config", e);
                return;
            }
        }

        try {
            config = new ObjectMapper(new YAMLFactory()).readValue(configFile, StandaloneConfig.class);
        } catch (IOException e) {
            logger.error("Failed to load config", e);
            return;
        }

        logger.setDebug(config.debugLog());

        // TODO Support multiple notification types
        notificationManager = new SlackNotificationManager(logger, config.slackWebhook());

        sessionManager = new SessionManager(new FileStorageManager("./cache", "./screenshot.jpg"), notificationManager, logger);

        sessionInfo = (SessionInfo) config.session().sessionInfo().copy();

        PingUtil.setWebPingEnabled(config.session().webQueryFallback());

        // Sync the session info from the server if needed
        updateSessionInfo(sessionInfo);

        createSession();

        logger.start();
    }

    public static void restart() {
        try {
            sessionManager.shutdown();

            // Create a new session manager, but reuse the notification manager as config hasn't been reloaded
            sessionManager = new SessionManager(new FileStorageManager("./cache", "./screenshot.jpg"), notificationManager, logger);

            createSession();
        } catch (SessionCreationException | SessionUpdateException e) {
            logger.error("Failed to restart session", e);
        }
    }

    private static void createSession() throws SessionCreationException, SessionUpdateException {
        sessionManager.restartCallback(StandaloneMain::restart);
        sessionManager.init(sessionInfo, config.friendSync());

        sessionManager.scheduledThread().scheduleWithFixedDelay(() -> {
            updateSessionInfo(sessionInfo);

            try {
                // Update the session
                sessionManager.updateSession(sessionInfo);
                if (config.suppressSessionUpdateInfo()) {
                    sessionManager.logger().debug("Updated session!");
                } else {
                    sessionManager.logger().info("Updated session!");
                }
            } catch (SessionUpdateException e) {
                sessionManager.logger().error("Failed to update session", e);
            }
        }, config.session().updateInterval(), config.session().updateInterval(), TimeUnit.SECONDS);
    }

    private static void updateSessionInfo(SessionInfo sessionInfo) {
        if (config.session().queryServer()) {
            try {
                InetSocketAddress addressToPing = new InetSocketAddress(sessionInfo.getIp(), sessionInfo.getPort());
                BedrockPong pong = PingUtil.ping(addressToPing, 1500, TimeUnit.MILLISECONDS).get();

                // Update the session information
                sessionInfo.setHostName(pong.motd());
                sessionInfo.setWorldName(pong.subMotd());
                sessionInfo.setVersion(pong.version());
                sessionInfo.setProtocol(pong.protocolVersion());
                sessionInfo.setPlayers(pong.playerCount());
                sessionInfo.setMaxPlayers(pong.maximumPlayerCount());
            } catch (InterruptedException | ExecutionException e) {
                if (config.session().configFallback()) {
                    sessionManager.logger().error("Failed to ping server, falling back to config values", e);

                    sessionInfo.setHostName(config.session().sessionInfo().getHostName());
                    sessionInfo.setWorldName(config.session().sessionInfo().getWorldName());
                    sessionInfo.setVersion(config.session().sessionInfo().getVersion());
                    sessionInfo.setProtocol(config.session().sessionInfo().getProtocol());
                    sessionInfo.setPlayers(config.session().sessionInfo().getPlayers());
                    sessionInfo.setMaxPlayers(config.session().sessionInfo().getMaxPlayers());
                } else {
                    sessionManager.logger().error("Failed to ping server", e);
                }
            }
        }
    }
}
