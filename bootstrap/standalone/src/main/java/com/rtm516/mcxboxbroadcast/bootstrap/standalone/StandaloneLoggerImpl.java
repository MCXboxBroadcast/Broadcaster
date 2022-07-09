package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.rtm516.mcxboxbroadcast.core.Logger;

public class StandaloneLoggerImpl implements Logger {
    private final org.slf4j.Logger logger;

    public StandaloneLoggerImpl(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
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
