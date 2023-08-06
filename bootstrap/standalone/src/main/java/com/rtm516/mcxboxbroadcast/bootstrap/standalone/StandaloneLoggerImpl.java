package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.rtm516.mcxboxbroadcast.core.Logger;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class StandaloneLoggerImpl extends SimpleTerminalConsole implements Logger {
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

    public void setDebug(boolean debug) {
        Configurator.setLevel(logger.getName(), debug ? Level.DEBUG : Level.INFO);
    }

    @Override
    protected boolean isRunning() {
        return true;
    }

    @Override
    protected void runCommand(String command) {
        String commandNode = command.split(" ")[0].toLowerCase();
        try {
            switch (commandNode) {
                case "exit" -> System.exit(0);
                case "restart" -> StandaloneMain.restart();
                default -> logger.warn("Unknown command: {}", commandNode);
            }
        } catch (Exception e) {
            logger.error("Failed to execute command", e);
        }
    }

    @Override
    protected void shutdown() {

    }
}
