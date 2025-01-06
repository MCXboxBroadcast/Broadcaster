package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.core.Logger;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class TeserLoggerImpl extends SimpleTerminalConsole implements Logger {
    private final org.slf4j.Logger logger;
    private final String prefixString;

    public TeserLoggerImpl(org.slf4j.Logger logger) {
        this(logger, "");
    }

    public TeserLoggerImpl(org.slf4j.Logger logger, String prefixString) {
        this.logger = logger;
        this.prefixString = prefixString;
    }

    @Override
    public void info(String message) {
        logger.info(prefix(message));
    }

    @Override
    public void warn(String message) {
        logger.warn(prefix(message));
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
        return new TeserLoggerImpl(logger, prefixString);
    }

    private String prefix(String message) {
        if (prefixString.isEmpty()) {
            return message;
        } else {
            return "[" + prefixString + "] " + message;
        }
    }

    public void setDebug(boolean debug) {
        Configurator.setLevel(logger.getName(), debug ? Level.DEBUG : Level.INFO);
    }

    @Override
    protected boolean isRunning() {
        return true;
    }

    @Override
    protected void runCommand(String command) {

    }

    @Override
    protected void shutdown() {

    }
}
