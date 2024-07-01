package com.rtm516.mcxboxbroadcast.manager;

import com.nukkitx.protocol.bedrock.BedrockClient;
import com.rtm516.mcxboxbroadcast.manager.database.model.Server;
import com.rtm516.mcxboxbroadcast.manager.database.repository.ServerCollection;
import com.rtm516.mcxboxbroadcast.manager.models.ServerContainer;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Scope
public class ServerManager {
    private final Map<ObjectId, ServerContainer> servers = new HashMap<>();
    private final BackendManager backendManager;
    private final ServerCollection serverCollection;

    private BedrockClient client;

    @Autowired
    public ServerManager(BackendManager backendManager, ServerCollection serverCollection) {
        this.backendManager = backendManager;
        this.serverCollection = serverCollection;

        // Load all servers from the database
        serverCollection.findAll().forEach(server -> {
            servers.put(server._id(), new ServerContainer(this, server));
        });

        // Start the bots in a new thread so the web server can start
        backendManager.scheduledThreadPool().scheduleWithFixedDelay(() -> {
            for (ServerContainer serverContainer : servers.values()) {
                serverContainer.updateSessionInfo();
            }
        }, 0, backendManager.updateTime().server(), TimeUnit.SECONDS);
    }

    /**
     * Get the stored servers
     *
     * @return the stored servers
     */
    public Map<ObjectId, ServerContainer> servers() {
        return servers;
    }

    /**
     * Add a new server
     *
     * @return the server container
     */
    public ServerContainer addServer() {
        Server server = serverCollection.save(new Server("mc.example.com", 19132));

        ServerContainer serverContainer = new ServerContainer(this, server);
        servers.put(server._id(), serverContainer);
        return serverContainer;
    }

    /**
     * Delete a server
     *
     * @param serverId the server id
     */
    public void deleteServer(ObjectId serverId) {
        servers.remove(serverId);
        serverCollection.deleteById(serverId);
    }

    /**
     * Get the first server or create a new one
     *
     * @return the first server
     */
    public ObjectId firstServer() {
        if (!servers.isEmpty()) {
            return servers.values().iterator().next().server()._id();
        }

        return addServer().server()._id();
    }

    /**
     * Get the bedrock client for pinging or create a new one
     *
     * @return the bedrock client
     */
    public BedrockClient bedrockClient() {
        if (client != null && !client.getRakNet().isClosed()) {
            return client;
        }

        try {
            InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", 0);
            client = new BedrockClient(bindAddress);

            client.bind().join();

            return client;
        } catch (Exception e) {
            BackendManager.LOGGER.error("Error creating bedrock client for ping", e);
        }

        return null;
    }
}
