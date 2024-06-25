package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.manager.database.model.Server;
import com.rtm516.mcxboxbroadcast.manager.database.repository.ServerCollection;
import com.rtm516.mcxboxbroadcast.manager.models.ServerContainer;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Scope
public class ServerManager {
    private final Map<ObjectId, ServerContainer> servers = new HashMap<>();
    private final BackendManager backendManager;
    private final ServerCollection serverCollection;

    @Autowired
    public ServerManager(BackendManager backendManager, ServerCollection serverCollection) {
        this.backendManager = backendManager;
        this.serverCollection = serverCollection;

        // Load all servers from the database
        serverCollection.findAll().forEach(server -> {
            servers.put(server._id(), new ServerContainer(server));
        });

        // Start the bots in a new thread so the web server can start
        backendManager.scheduledThreadPool().scheduleWithFixedDelay(() -> {
            for (ServerContainer serverContainer : servers.values()) {
                serverContainer.updateSessionInfo();
            }
        }, 0, backendManager.updateTime() / 2, TimeUnit.SECONDS);
    }

    public Map<ObjectId, ServerContainer> servers() {
        return servers;
    }

    public ServerContainer addServer() {
        Server server = serverCollection.save(new Server("mc.example.com", 19132));

        ServerContainer serverContainer = new ServerContainer(server);
        servers.put(server._id(), serverContainer);
        return serverContainer;
    }

    public void deleteServer(ObjectId serverId) {
        servers.remove(serverId);
    }

    public ObjectId firstServer() {
        if (!servers.isEmpty()) {
            return servers.values().iterator().next().server()._id();
        }

        return addServer().server()._id();
    }
}
