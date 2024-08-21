package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.configs.StandaloneConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.ping.PingUtil;
import com.rtm516.mcxboxbroadcast.core.storage.FileStorageManager;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

public class StandaloneMain {
    private static StandaloneConfig config;
    private static StandaloneLoggerImpl logger;
    private static SessionInfo sessionInfo;

    public static SessionManager sessionManager;

    public static void main(String[] args) throws Exception {
        // Redirect all logging to SLF4J since ice4j uses java.util.logging
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();

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

        sessionManager = new SessionManager(new FileStorageManager("./cache"), logger);

        sessionInfo = config.session().sessionInfo();

        // Sync the session info from the server if needed
        updateSessionInfo(sessionInfo);

        createSession();

        logger.start();
    }

    public static void restart() {
        try {
            sessionManager.shutdown();

            sessionManager = new SessionManager(new FileStorageManager("./cache"), logger);

            createSession();
        } catch (SessionCreationException | SessionUpdateException e) {
            logger.error("Failed to restart session", e);
        }
    }

    private static void createSession() throws SessionCreationException, SessionUpdateException {
        sessionManager.restartCallback(StandaloneMain::restart);
        sessionManager.init(sessionInfo, config.friendSync());

//        sessionManager.scheduledThread().scheduleWithFixedDelay(() -> {
//            updateSessionInfo(sessionInfo);
//
//            try {
//                // Update the session
//                sessionManager.updateSession(sessionInfo);
//                sessionManager.logger().info("Updated session!");
//            } catch (SessionUpdateException e) {
//                sessionManager.logger().error("Failed to update session", e);
//            }
//        }, config.session().updateInterval(), config.session().updateInterval(), TimeUnit.SECONDS);
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
                sessionManager.logger().error("Failed to ping server", e);
            }
        }
    }
}
