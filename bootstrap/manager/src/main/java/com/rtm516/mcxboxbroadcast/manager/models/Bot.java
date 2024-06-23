package com.rtm516.mcxboxbroadcast.manager.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.configs.FriendSyncConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.manager.BotManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class Bot {
    private final Info info;
    private final StringBuilder logs = new StringBuilder();
    private final DateTimeFormatter logTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final BotManager botManager;

    private Logger logger;
    private SessionManager sessionManager;

    public Bot(BotManager botManager, Info info) {
        this.botManager = botManager;
        this.info = info;
    }

    public Info info() {
        return info;
    }

    public String logs() {
        return logs.toString();
    }

    protected void log(String level, String message) {
        // [%d{HH:mm:ss.SSS} %t/%level]
        logs.append("[" + LocalDateTime.now().format(logTimeFormatter) + " " + level + "]").append(message).append("\n");
    }

    public void start() {
        info().status(Status.STARTING);
        logger = new Logger(this); // TODO Move to file based?
        sessionManager = new SessionManager("./cache/" + info().id(), logger);

        sessionManager.restartCallback(this::restart);
        try {
            sessionManager.init(botManager.serverSessionInfo(info().serverId()), new FriendSyncConfig(20, true, true));
            info().status(Status.ONLINE);

            info().gamertag(sessionManager.getGamertag());
            info().xid(sessionManager.getXuid());

            sessionManager.scheduledThread().scheduleWithFixedDelay(this::updateSessionInfo, 30, 30, TimeUnit.SECONDS);
        } catch (SessionCreationException | SessionUpdateException e) {
            logger.error("Failed to create session", e);
            info().status(Status.OFFLINE);
        } catch (Exception e) {
            logger.error("An unexpected error occurred", e);
            info().status(Status.OFFLINE);
        }
    }

    public void updateSessionInfo() {
        // If the bot is not online, don't update the session
        if (info().status() != Status.ONLINE) {
            return;
        }

        try {
            // Update the session
            sessionManager.updateSession(botManager.serverSessionInfo(info().serverId()));
            sessionManager.logger().info("Updated session!");
        } catch (SessionUpdateException e) {
            sessionManager.logger().error("Failed to update session", e);
        }
    }

    public void stop() {
        sessionManager.shutdown();
        info().status(Status.OFFLINE);
    }

    public void restart() {
        stop();
        start();
    }

    public static class Info {
        @JsonProperty
        private int id;
        @JsonProperty
        private String gamertag;
        @JsonProperty
        private String xid;
        @JsonProperty
        private Status status;
        @JsonProperty
        private int serverId;

        public Info(int id) {
            this(id, "", "", 0);
        }

        public Info(int id, String gamertag, String xid, int serverId) {
            this.id = id;
            this.gamertag = gamertag;
            this.xid = xid;
            this.serverId = serverId;

            this.status = Status.OFFLINE;
        }

        public int id() {
            return id;
        }

        public String gamertag() {
            return gamertag;
        }

        public void gamertag(String gamertag) {
            this.gamertag = gamertag;
        }

        public String xid() {
            return xid;
        }

        public void xid(String xid) {
            this.xid = xid;
        }

        public Status status() {
            return status;
        }

        public void status(Status status) {
            this.status = status;
        }

        public int serverId() {
            return serverId;
        }

        public void serverId(int serverId) {
            this.serverId = serverId;
        }
    }

    public class Logger implements com.rtm516.mcxboxbroadcast.core.Logger {
        private final Bot bot;
        private final String prefixString;

        public Logger(Bot bot) {
            this(bot, "");
        }

        public Logger(Bot bot, String prefixString) {
            this.bot = bot;
            this.prefixString = prefixString;
        }

        @Override
        public void info(String message) {
            bot.log("INFO", prefix(message));
        }

        @Override
        public void warn(String message) {
            bot.log("WARN", prefix(message));
        }

        @Override
        public void error(String message) {
            bot.log("ERROR", prefix(message));
        }

        @Override
        public void error(String message, Throwable ex) {
            bot.log("ERROR", prefix(message) + "\n" + getStackTrace(ex));
        }

        @Override
        public void debug(String message) {
            bot.log("DEBUG", prefix(message));
        }

        @Override
        public com.rtm516.mcxboxbroadcast.core.Logger prefixed(String prefixString) {
            return new Logger(bot, prefixString);
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
