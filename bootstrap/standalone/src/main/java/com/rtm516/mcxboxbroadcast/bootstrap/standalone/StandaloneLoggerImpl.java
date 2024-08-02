package com.rtm516.mcxboxbroadcast.bootstrap.standalone;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.core.Logger;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class StandaloneLoggerImpl extends SimpleTerminalConsole implements Logger {
    private final org.slf4j.Logger logger;
    private final String prefixString;

    public StandaloneLoggerImpl(org.slf4j.Logger logger) {
        this(logger, "");
    }

    public StandaloneLoggerImpl(org.slf4j.Logger logger, String prefixString) {
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
        return new StandaloneLoggerImpl(logger, prefixString);
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
        String commandNode = command.split(" ")[0].toLowerCase();
        try {
            switch (commandNode) {
                case "exit" -> System.exit(0);
                case "restart" -> StandaloneMain.restart();
                case "dumpsession" -> {
                    info("Dumping session responses to 'lastSessionResponse.json' and 'currentSessionResponse.json'");
                    StandaloneMain.sessionManager.dumpSession();
                }
                case "accounts" -> {
                    String[] args = command.split(" ");
                    if (args.length < 3) {
                        if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
                            StandaloneMain.sessionManager.listSessions();
                            return;
                        }

                        warn("Usage:");
                        warn("accounts list");
                        warn("accounts add/remove <sub-session-id>");
                        return;
                    }

                    switch (args[1].toLowerCase()) {
                        case "add" -> StandaloneMain.sessionManager.addSubSession(args[2]);
                        case "remove" -> StandaloneMain.sessionManager.removeSubSession(args[2]);
                        default -> warn("Unknown accounts command: " + args[1]);
                    }
                }
                case "version" -> info("MCXboxBroadcast Standalone " + BuildData.VERSION);
                case "help" -> {
                    info("Available commands:");
                    info("exit - Exit the application");
                    info("restart - Restart the application");
                    info("dumpsession - Dump the current session to json files");
                    info("accounts list - List sub-accounts");
                    info("accounts add <sub-session-id> - Add a sub-account");
                    info("accounts remove <sub-session-id> - Remove a sub-account");
                    info("version - Display the version");
                }
                default -> warn("Unknown command: " + commandNode);
            }
        } catch (Exception e) {
            error("Failed to execute command", e);
        }
    }

    @Override
    protected void shutdown() {

    }
}
