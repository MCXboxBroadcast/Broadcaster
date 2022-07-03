package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.rtm516.mcxboxbroadcast.core.GenericLoggerImpl;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;

public class StandaloneMain {
    public static void main(String[] args) throws Exception {
        Logger logger = new GenericLoggerImpl();

        SessionManager sessionManager = new SessionManager("./cache", logger);

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setHostName("Geyser Test Server");
        sessionInfo.setWorldName("Test");
        sessionInfo.setVersion("1.18.12");
        sessionInfo.setProtocol(486);
        sessionInfo.setPlayers(0);
        sessionInfo.setMaxPlayers(20);
        sessionInfo.setIp("51.210.124.95");
        sessionInfo.setPort(19132);

        logger.info("Creating session...");

        sessionManager.createSession(sessionInfo);

        logger.info("Created session!");

        Thread.sleep(10 * 1000);

        sessionInfo.setPlayers(10);
        sessionManager.updateSession(sessionInfo);
        logger.info("Updated session!");
    }
}
