package com.rtm516.mcxboxbroadcast.manager.models;

import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.configs.FriendSyncConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.manager.BotManager;
import com.rtm516.mcxboxbroadcast.manager.database.model.Bot;
import com.rtm516.mcxboxbroadcast.manager.models.response.BotInfoResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class BotContainer {
    private final Bot bot;
    private final StringBuilder logs = new StringBuilder();
    private final DateTimeFormatter logTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final BotManager botManager;

    private Logger logger;
    private SessionManager sessionManager;
    private Status status;

    public BotContainer(BotManager botManager, Bot bot) {
        this.botManager = botManager;
        this.bot = bot;

        status = Status.OFFLINE;
    }

    public Bot bot() {
        return bot;
    }

    public String logs() {
        return logs.toString();
    }

    protected void log(String level, String message) {
        logs.append("[" + LocalDateTime.now().format(logTimeFormatter) + " " + level + "]").append(message).append("\n");
    }

    public BotInfoResponse toResponse() {
        return bot.toResponse(status);
    }

    public void start() {
        status = Status.STARTING;
        logger = new Logger(this); // TODO Move to file based?
        sessionManager = new SessionManager("./cache/" + bot()._id(), logger);

        sessionManager.restartCallback(this::restart);
        try {
            sessionManager.init(botManager.serverSessionInfo(bot().serverId()), new FriendSyncConfig(20, true, true));
            status = Status.ONLINE;

            bot().gamertag(sessionManager.getGamertag());
            bot().xid(sessionManager.getXuid());
            botManager.botCollection().save(bot());

            sessionManager.scheduledThread().scheduleWithFixedDelay(this::updateSessionInfo, 30, 30, TimeUnit.SECONDS);
        } catch (SessionCreationException | SessionUpdateException e) {
            logger.error("Failed to create session", e);
            status = Status.OFFLINE;
        } catch (Exception e) {
            logger.error("An unexpected error occurred", e);
            status = Status.OFFLINE;
        }
    }

    public void updateSessionInfo() {
        // If the bot is not online, don't update the session
        if (status != Status.ONLINE) {
            return;
        }

        try {
            // Update the session
            sessionManager.updateSession(botManager.serverSessionInfo(bot().serverId()));
            sessionManager.logger().info("Updated session!");
        } catch (SessionUpdateException e) {
            sessionManager.logger().error("Failed to update session", e);
        }
    }

    public void stop() {
        sessionManager.shutdown();
        status = Status.OFFLINE;
    }

    public void restart() {
        stop();
        start();
    }

    public class Logger implements com.rtm516.mcxboxbroadcast.core.Logger {
        private final BotContainer botContainer;
        private final String prefixString;

        public Logger(BotContainer botContainer) {
            this(botContainer, "");
        }

        public Logger(BotContainer botContainer, String prefixString) {
            this.botContainer = botContainer;
            this.prefixString = prefixString;
        }

        @Override
        public void info(String message) {
            botContainer.log("INFO", prefix(message));
        }

        @Override
        public void warn(String message) {
            botContainer.log("WARN", prefix(message));
        }

        @Override
        public void error(String message) {
            botContainer.log("ERROR", prefix(message));
        }

        @Override
        public void error(String message, Throwable ex) {
            botContainer.log("ERROR", prefix(message) + "\n" + getStackTrace(ex));
        }

        @Override
        public void debug(String message) {
            botContainer.log("DEBUG", prefix(message));
        }

        @Override
        public com.rtm516.mcxboxbroadcast.core.Logger prefixed(String prefixString) {
            return new Logger(botContainer, prefixString);
        }

        private String prefix(String message) {
            if (prefixString.isEmpty()) {
                return message;
            } else {
                return "[" + prefixString + "] " + message;
            }
        }

        private static String getStackTrace(Throwable ex) {
            // Create a PrintWriter to write the stack trace to
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            // Write the stack trace to the PrintWriter
            ex.printStackTrace(pw);

            // Close the PrintWriter
            pw.close();

            // Return the stack trace as a string
            return sw.toString();
        }
    }

    public enum Status {
        OFFLINE,
        STARTING,
        ONLINE
    }
}
