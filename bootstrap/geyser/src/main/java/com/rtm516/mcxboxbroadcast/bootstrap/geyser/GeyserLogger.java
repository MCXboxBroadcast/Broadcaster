package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.rtm516.mcxboxbroadcast.core.Logger;
import org.geysermc.geyser.api.extension.ExtensionLogger;

public class GeyserLogger implements Logger {
    private ExtensionLogger logger;

    public GeyserLogger(ExtensionLogger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warning(String message) {
        logger.warning(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable ex) {
        logger.error(message, ex);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }
}
