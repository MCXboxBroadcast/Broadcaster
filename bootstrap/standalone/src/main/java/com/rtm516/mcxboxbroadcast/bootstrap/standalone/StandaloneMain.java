package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nukkitx.protocol.bedrock.BedrockClient;
import com.nukkitx.protocol.bedrock.BedrockPong;
import com.rtm516.mcxboxbroadcast.core.FriendUtils;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.configs.StandaloneConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import org.java_websocket.util.NamedThreadFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StandaloneMain {
    private static StandaloneConfig config;
    private static StandaloneLoggerImpl logger;
    private static SessionInfo sessionInfo;
    private static ScheduledExecutorService scheduledThreadPool;

    public static SessionManager sessionManager;

    public static void main(String[] args) throws Exception {
        logger = new StandaloneLoggerImpl(LoggerFactory.getLogger(StandaloneMain.class));

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

        sessionManager = new SessionManager("./cache", logger);

        sessionInfo = config.session().sessionInfo();

        // Sync the session info from the server if needed
        updateSessionInfo(sessionInfo);

        createSession();

        logger.start();
    }

    public static void restart() throws SessionUpdateException, SessionCreationException {
        sessionManager.stopSession();
        scheduledThreadPool.shutdown();

        sessionManager = new SessionManager("./cache", logger);

        createSession();
    }

    private static void createSession() throws SessionCreationException, SessionUpdateException {
        scheduledThreadPool = Executors.newScheduledThreadPool(2, new NamedThreadFactory("Scheduled Thread"));

        logger.info("Creating session...");

        sessionManager.createSession(sessionInfo);
        sessionManager.updatePresence();

        logger.info("Created session!");

        scheduledThreadPool.scheduleWithFixedDelay(() -> {
            updateSessionInfo(sessionInfo);

            try {
                // Make sure the connection is still active
                sessionManager.checkConnection();

                // Update the session
                sessionManager.updateSession(sessionInfo);
                sessionManager.updatePresence();
                logger.info("Updated session!");
            } catch (SessionUpdateException e) {
                logger.error("Failed to update session", e);
            }
        }, config.session().updateInterval(), config.session().updateInterval(), TimeUnit.SECONDS);

        if (config.friendSync().autoFollow() || config.friendSync().autoUnfollow()) {
            scheduledThreadPool.scheduleWithFixedDelay(() -> FriendUtils.autoFriend(sessionManager, logger, config.friendSync()), config.friendSync().updateInterval(), config.friendSync().updateInterval(), TimeUnit.SECONDS);
        }
    }

    private static void updateSessionInfo(SessionInfo sessionInfo) {
        if (config.session().queryServer()) {
            BedrockClient client = null;
            try {
                InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", 0);
                client = new BedrockClient(bindAddress);

                client.bind().join();

                InetSocketAddress addressToPing = new InetSocketAddress(sessionInfo.getIp(), sessionInfo.getPort());
                BedrockPong pong = client.ping(addressToPing, 1500, TimeUnit.MILLISECONDS).get();

                // Update the session information
                sessionInfo.setHostName(pong.getMotd());
                sessionInfo.setWorldName(pong.getSubMotd());
                sessionInfo.setVersion(pong.getVersion());
                sessionInfo.setProtocol(pong.getProtocolVersion());
                sessionInfo.setPlayers(pong.getPlayerCount());
                sessionInfo.setMaxPlayers(pong.getMaximumPlayerCount());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Failed to ping server", e);
            } finally {
                if (client != null) {
                    client.close();
                }
            }
        }
    }
}
