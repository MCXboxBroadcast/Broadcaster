package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.notifications.SlackNotificationManager;
import com.rtm516.mcxboxbroadcast.core.configs.ExtensionConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.storage.FileStorageManager;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.connection.GeyserBedrockPingEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class MCXboxBroadcastExtension implements Extension {
    Logger logger;
    NotificationManager notificationManager;
    SessionManager sessionManager;
    SessionInfo sessionInfo;
    ExtensionConfig config;

    @Subscribe
    public void onCommandDefine(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this)
            .source(CommandSource.class)
            .name("restart")
            .description("Restart the connection to Xbox Live.")
            .executor((source, command, args) -> {
                if (!source.isConsole()) {
                    source.sendMessage("This command can only be ran from the console.");
                    return;
                }

                restart();
            })
            .build());

        event.register(Command.builder(this)
            .source(CommandSource.class)
            .name("dumpsession")
            .description("Dump the current session to json files.")
            .executor((source, command, args) -> {
                if (!source.isConsole()) {
                    source.sendMessage("This command can only be ran from the console.");
                    return;
                }

                logger.info("Dumping session responses to 'lastSessionResponse.json' and 'currentSessionResponse.json'");

                sessionManager.dumpSession();
            })
            .build());

        event.register(Command.builder(this)
            .source(CommandSource.class)
            .name("accounts")
            .description("Manage sub-accounts.")
            .executor((source, command, args) -> {
                if (!source.isConsole()) {
                    source.sendMessage("This command can only be ran from the console.");
                    return;
                }

                if (args.length < 2) {
                    if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                        sessionManager.listSessions();
                        return;
                    }

                    source.sendMessage("Usage:");
                    source.sendMessage("accounts list");
                    source.sendMessage("accounts add/remove <sub-session-id>");
                    return;
                }

                switch (args[0].toLowerCase()) {
                    case "add":
                        sessionManager.addSubSession(args[1]);
                        break;
                    case "remove":
                        sessionManager.removeSubSession(args[1]);
                        break;
                    default:
                        source.sendMessage("Unknown accounts command: " + args[0]);
                }
            })
            .build());

        event.register(Command.builder(this)
            .source(CommandSource.class)
            .name("version")
            .description("Get the version of the extension.")
            .executor((source, command, args) -> {
                source.sendMessage("MCXboxBroadcast Extension " + BuildData.VERSION);
            })
            .build());
    }

    private void restart() {
        sessionManager.shutdown();

        // Create a new session manager, but reuse the notification manager as config hasn't been reloaded
        sessionManager = new SessionManager(new FileStorageManager(this.dataFolder().toString(), this.dataFolder().resolve("screenshot.jpg").toString()), notificationManager, logger);

        // Pull onto another thread so we don't hang the main thread
        sessionManager.scheduledThread().execute(this::createSession);
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        logger = new ExtensionLoggerImpl(this.logger());

        logger.info("Starting MCXboxBroadcast Extension " + BuildData.VERSION + " for Bedrock " + Constants.BEDROCK_CODEC.getMinecraftVersion() + " (" + Constants.BEDROCK_CODEC.getProtocolVersion() + ")");

        // Load the config file
        config = ConfigLoader.load(this, MCXboxBroadcastExtension.class, ExtensionConfig.class);

        // Make sure we loaded a config and disable the extension if we didn't
        if (config == null) {
            logger.error("Failed to load config, extension will not start!");
            this.disable();
            return;
        }

        // TODO Support multiple notification types
        notificationManager = new SlackNotificationManager(logger, config.slackWebhook());

        // Create the session manager
        sessionManager = new SessionManager(new FileStorageManager(this.dataFolder().toString(), this.dataFolder().resolve("screenshot.jpg").toString()), notificationManager, logger);

        // Pull onto another thread so we don't hang the main thread
        sessionManager.scheduledThread().execute(() -> {
            // Get the ip to broadcast
            String ip = config.remoteAddress();
            if (ip.equals("auto")) {
                ip = this.geyserApi().bedrockListener().address();

                try {
                    InetAddress address = InetAddress.getByName(ip);

                    // Get the public IP if the config ip is a non-public address
                    if (address.isSiteLocalAddress() || address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                        HttpRequest ipRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://ipv4.icanhazip.com"))
                            .GET()
                            .build();

                        ip = HttpClient.newHttpClient().send(ipRequest, HttpResponse.BodyHandlers.ofString()).body().trim();
                    }
                } catch (IOException | InterruptedException e) {
                    // Silently ignore
                }
            }

            // Get the port to broadcast
            int port = this.geyserApi().bedrockListener().port();
            if (!config.remotePort().equals("auto")) {
                port = Integer.parseInt(config.remotePort());
            }

            // Create the session information based on the Geyser config
            sessionInfo = new SessionInfo();
            sessionInfo.setHostName(this.geyserApi().bedrockListener().secondaryMotd());
            sessionInfo.setWorldName(this.geyserApi().bedrockListener().primaryMotd());
            sessionInfo.setPlayers(this.geyserApi().onlineConnections().size());
            sessionInfo.setMaxPlayers(GeyserImpl.getInstance().config().motd().maxPlayers()); // TODO Find API equivalent

            // Fallback to the gamertag if the host name is empty
            if (sessionInfo.getHostName().isEmpty()) {
                sessionInfo.setHostName(sessionManager.getGamertag());
            }

            sessionInfo.setIp(ip);
            sessionInfo.setPort(port);

            createSession();
        });
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        sessionManager.shutdown();
    }

    @Subscribe
    public void onBedrockPing(GeyserBedrockPingEvent event) {
        if (sessionInfo == null) {
            return;
        }

        // Allows support for motd and player count passthrough
        sessionInfo.setHostName(event.secondaryMotd());
        sessionInfo.setWorldName(event.primaryMotd());
        
        sessionInfo.setPlayers(event.playerCount());
        sessionInfo.setMaxPlayers(event.maxPlayerCount());

        // Fallback to the gamertag if the host name is empty
        if (sessionInfo.getHostName().isEmpty()) {
            sessionInfo.setHostName(sessionManager.getGamertag());
        }
    }


    private void createSession() {
        // Create the Xbox session
        sessionManager.restartCallback(this::restart);
        try {
            sessionManager.init(sessionInfo, config.friendSync());
        } catch (SessionCreationException | SessionUpdateException e) {
            sessionManager.logger().error("Failed to create xbox session!", e);
            return;
        }

        // Start the update timer
        sessionManager.scheduledThread().scheduleWithFixedDelay(this::tick, config.updateInterval(), config.updateInterval(), TimeUnit.SECONDS);
    }

    private void tick() {
        try {
            sessionManager.updateSession(sessionInfo);
        } catch (SessionUpdateException e) {
            sessionManager.logger().error("Failed to update session information!", e);
        }
    }
}
