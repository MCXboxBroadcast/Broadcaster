package com.rtm516.mcxboxbroadcast.bootstrap.geyser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.rtm516.mcxboxbroadcast.bootstrap.geyser.player.PlayerImpl;
import com.rtm516.mcxboxbroadcast.bootstrap.geyser.player.converter.EssentialsPlayer;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.configs.ExtensionConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.FollowerResponse;
import com.rtm516.mcxboxbroadcast.core.player.Player;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.floodgate.util.Utils;
import org.geysermc.floodgate.util.WhitelistUtils;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.platform.standalone.GeyserStandaloneLogger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MCXboxBroadcastExtension implements Extension, Runnable {
    Logger logger;
    SessionManager sessionManager;
    SessionInfo sessionInfo;
    ExtensionConfig config;
    private File playersFolder;
    private File essentialsFolder;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final List<GeyserSession> sessions = new ArrayList<>();

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

                    sessionManager.shutdown();

                    sessionManager = new SessionManager(this.dataFolder().toString(), logger);

                    createSession();
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

                    if (args.length < 3) {
                        if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
                            sessionManager.listSessions();
                            return;
                        }

                        source.sendMessage("Usage:");
                        source.sendMessage("accounts list");
                        source.sendMessage("accounts add/remove <sub-session-id>");
                        return;
                    }

                    switch (args[1].toLowerCase()) {
                        case "add":
                            sessionManager.addSubSession(args[2]);
                            break;
                        case "remove":
                            sessionManager.removeSubSession(args[2]);
                            break;
                        default:
                            source.sendMessage("Unknown accounts command: " + args[1]);
                    }
                })
                .build());
    }

    @Subscribe
    public void sessionJoinEvent(SessionJoinEvent event) {
        String xuid = event.connection().xuid();
        Player playerImpl = sessionManager.getPlayer(xuid);
        sessions.add((GeyserSession) event.connection());
        playerImpl.setJoinTimes(playerImpl.getJoinTimes() + 1);
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        executor.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
        this.playersFolder = new File(this.dataFolder().toFile(), "players");
        this.essentialsFolder = new File(this.dataFolder().toFile(), "essentials");
        logger = new ExtensionLoggerImpl(this.logger());
        sessionManager = new SessionManager(this.dataFolder().toString(), logger);

        // Load the config file
        config = ConfigLoader.load(this, MCXboxBroadcastExtension.class, ExtensionConfig.class);

        // Pull onto another thread so we don't hang the main thread
        new Thread(() -> {
            // Get the ip to broadcast
            String ip = config.remoteAddress();
            if (ip.equals("auto")) {
                ip = this.geyserApi().bedrockListener().address();

                // This is the most reliable for getting the main local IP
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("geysermc.org", 80));
                    ip = socket.getLocalAddress().getHostAddress();
                } catch (IOException e1) {
                    try {
                        // Fallback to the normal way of getting the local IP
                        ip = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException ignored) {
                    }
                }
            }

            // Get the port to broadcast
            int port = this.geyserApi().bedrockListener().port();
            if (!config.remotePort().equals("auto")) {
                port = Integer.parseInt(config.remotePort());
            }

            // Create the session information based on the Geyser config
            sessionInfo = new SessionInfo();
            sessionInfo.setHostName(this.geyserApi().bedrockListener().primaryMotd());
            sessionInfo.setWorldName(this.geyserApi().bedrockListener().secondaryMotd());
            sessionInfo.setVersion(this.geyserApi().defaultRemoteServer().minecraftVersion());
            sessionInfo.setProtocol(this.geyserApi().defaultRemoteServer().protocolVersion());
            sessionInfo.setPlayers(this.geyserApi().onlineConnections().size());
            sessionInfo.setMaxPlayers(GeyserImpl.getInstance().getConfig().getMaxPlayers()); // TODO Find API equivalent

            sessionInfo.setIp(ip);
            sessionInfo.setPort(port);

            createSession();
        }).start();
    }

    private void createSession() {
        // Create the Xbox session
        sessionManager.setGetPlayerFunction(uuid -> {
            String javaUuid = getJavaUuid(uuid).toString();
            File file = new File(this.playersFolder, uuid + ".json");
            if (!file.exists()) {
                System.out.println("File doesn't exist");
                EssentialsPlayer essentialsPlayer = this.findEssentialsPlayer(javaUuid);
                if (essentialsPlayer != null) {
                    PlayerImpl player = new PlayerImpl();
                    player.setLastLogOff(essentialsPlayer.getTimestamps().getLogout());
                    this.deleteEssentialsPlayer(javaUuid);
                    return player;
                }
            }
            try {
                return new ObjectMapper(new JsonFactory()).readValue(new File(this.playersFolder, uuid + ".json"), PlayerImpl.class);
            } catch (IOException e) {
                return new PlayerImpl();
            }
        });
        try {
            sessionManager.init(sessionInfo);
        } catch (SessionCreationException | SessionUpdateException e) {
            sessionManager.logger().error("Failed to create xbox session!", e);
            return;
        }

        // Set up the auto friend sync
        sessionManager.friendManager().initAutoFriend(config.friendSync());

        // Start the update timer
        sessionManager.scheduledThread().scheduleWithFixedDelay(this::tick, config.updateInterval(), config.updateInterval(), TimeUnit.SECONDS);
    }

    private void tick() {
        // Update the player count for the session
        try {
            sessionInfo.setPlayers(this.geyserApi().onlineConnections().size());
            sessionManager.updateSession(sessionInfo);
        } catch (SessionUpdateException e) {
            sessionManager.logger().error("Failed to update session information!", e);
        }

        // If we are in spigot, using floodgate authentication and have the config option enabled
        // get the users friends and whitelist them
        if (this.geyserApi().defaultRemoteServer().authType() == AuthType.FLOODGATE
                && this.geyserApi().platformType() == PlatformType.SPIGOT // TODO Find API equivalent
                && config.whitelistFriends()) {
            try {
                for (FollowerResponse.Person person : sessionManager.friendManager().get()) {
                    if (WhitelistUtils.addPlayer(Utils.getJavaUuid(person.xuid), "unknown")) {
                        sessionManager.logger().info("Added xbox friend " + person.displayName + " to whitelist");
                    }
                }
            } catch (XboxFriendsException e) {
                sessionManager.logger().error("Failed to fetch xbox friends for whitelist!", e);
            }
        }
    }

    public static UUID getJavaUuid(long xuid) {
        return new UUID(0, xuid);
    }

    public static UUID getJavaUuid(String xuid) {
        return getJavaUuid(Long.parseLong(xuid));
    }

    public EssentialsPlayer findEssentialsPlayer(String minecraftUUID) {
        File file = new File(this.essentialsFolder, minecraftUUID + ".yml");
        if (!file.exists()) return null;

        try {
            return new ObjectMapper(new YAMLFactory()).readValue(file, EssentialsPlayer.class);
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }

        return null;
    }

    public void deleteEssentialsPlayer(String minecraftUUID) {
        File file = new File(this.essentialsFolder, minecraftUUID + ".yml");
        if (!file.exists()) return;

        file.delete();
    }

    @Override
    public void run() {
        for (GeyserSession session : new ArrayList<>(sessions)) {
            if (GeyserImpl.getInstance().onlineConnections().stream().anyMatch(x -> x.xuid().equals(session.xuid())))
                continue;

            sessions.remove(session);
            String xuid = session.xuid();
            Player playerImpl = sessionManager.getPlayer(xuid);
            playerImpl.setLastLogOff(System.currentTimeMillis());
            sessionManager.getPlayers().remove(xuid);
            try {
                File file = new File(playersFolder, xuid + ".json");
                if (!file.exists()) {
                    file.createNewFile();
                }
                new ObjectMapper(new JsonFactory()).writeValue(file, playerImpl);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
