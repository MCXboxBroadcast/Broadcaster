package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.rtm516.mcxboxbroadcast.core.Logger;
import org.geysermc.geyser.api.extension.ExtensionLogger;

public class ExtensionLoggerImpl implements Logger {
    private final ExtensionLogger logger;
    private final String prefixString;

    public ExtensionLoggerImpl(ExtensionLogger logger) {
        this(logger, "");
    }

    public ExtensionLoggerImpl(ExtensionLogger logger, String prefixString) {
        this.logger = logger;
        this.prefixString = prefixString;
    }

    @Override
    public void info(String message) {
        logger.info(prefix(message));
    }

    @Override
    public void warn(String message) {
        logger.warning(prefix(message));
    }

    @Override
    public void error(String message) {
        logger.error(prefix(message));
    }

    @Override
    public void error(String message, Throwable ex) {
        logger.error(prefix(message), ex);
    }

    @Override
    public void debug(String message) {
        logger.debug(prefix(message));
    }

    @Override
    public Logger prefixed(String prefixString) {
        return new ExtensionLoggerImpl(logger, prefixString);
    }

    private String prefix(String message) {
        if (prefixString.isEmpty()) {
            return message;
        } else {
            return "[" + prefixString + "] " + message;
        }
    }
}
