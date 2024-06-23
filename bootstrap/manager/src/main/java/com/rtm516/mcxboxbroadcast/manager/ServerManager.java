package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.manager.models.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Scope
public class ServerManager {
    private final BackendManager backendManager;

    private Map<Integer, Server> servers = new HashMap<>();

    @Autowired
    public ServerManager(BackendManager backendManager) {
        this.backendManager = backendManager;

        // String hostName, String worldName, String version, int protocol, int players, int maxPlayers, String ip, int port
        servers.put(0, new Server(0, "test.geysermc.org", 19132));

        // Start the bots in a new thread so the web server can start
        backendManager.scheduledThreadPool().execute(() -> {
            for (Server server : servers.values()) {
                server.updateSessionInfo();
            }
        });
    }

    public Map<Integer, Server> servers() {
        return servers;
    }

    public Server addServer() {
        Server server = new Server(servers.size(), "", 19132);
        servers.put(server.id(), server);
        return server;
    }

    public void deleteServer(int serverId) {
        servers.remove(serverId);
    }
}
