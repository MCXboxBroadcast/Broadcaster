package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rtm516.mcxboxbroadcast.core.GenericLoggerImpl;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;

public class StandaloneMain {
    public static void main(String[] args) throws Exception {
        Logger logger = new GenericLoggerImpl();

        SessionManager sessionManager = new SessionManager("./cache", logger);

        String configFileName = "config.yml";
        File configFile = new File(configFileName);
        StandaloneConfig config;

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

        SessionInfo sessionInfo = config.sessionInfo;

        logger.info("Creating session...");

        sessionManager.createSession(sessionInfo);

        logger.info("Created session!");

        Thread.sleep(config.updateInterval * 1000L);

//        sessionInfo.setPlayers(10);
        sessionManager.updateSession(sessionInfo);
        logger.info("Updated session!");
    }
}
