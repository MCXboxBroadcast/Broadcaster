package com.rtm516.mcxboxbroadcast.manager.controllers;

import com.rtm516.mcxboxbroadcast.manager.BackendManager;
import com.rtm516.mcxboxbroadcast.manager.ServerManager;
import com.rtm516.mcxboxbroadcast.manager.models.Server;
import com.rtm516.mcxboxbroadcast.manager.models.request.ServerUpdateRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping("/api/servers")
public class ServersController {

    private final ServerManager serverManager;
    private final BackendManager backendManager;

    @Autowired
    public ServersController(ServerManager serverManager, BackendManager backendManager) {
        this.serverManager = serverManager;
        this.backendManager = backendManager;
    }

    @GetMapping("")
    public List<Server> servers(HttpServletResponse response) {
        response.setStatus(200);
        return serverManager.servers().values().stream().toList();
    }

    @PostMapping("/create")
    public int create(HttpServletResponse response) {
        response.setStatus(200);
        Server server = serverManager.addServer();
        return server.id();
    }

    @GetMapping("/{serverId:[0-9]+}")
    public Server server(HttpServletResponse response, @PathVariable int serverId) {
        if (!serverManager.servers().containsKey(serverId)) {
            response.setStatus(404);
            return null;
        }
        response.setStatus(200);
        return serverManager.servers().get(serverId);
    }

    @PostMapping("/{serverId:[0-9]+}")
    public void update(HttpServletResponse response, @PathVariable int serverId, @RequestBody ServerUpdateRequest serverUpdateRequest) {
        if (!serverManager.servers().containsKey(serverId)) {
            response.setStatus(404);
            return;
        }

        if (serverUpdateRequest.hostname() == null || serverUpdateRequest.hostname().isEmpty()) {
            response.setStatus(400);
            return;
        }

        if (serverUpdateRequest.port() < 1 || serverUpdateRequest.port() > 65535) {
            response.setStatus(400);
            return;
        }

        serverManager.servers().get(serverId).hostname(serverUpdateRequest.hostname());
        serverManager.servers().get(serverId).port(serverUpdateRequest.port());

        backendManager.scheduledThreadPool().execute(() -> serverManager.servers().get(serverId).updateSessionInfo()); // Update the session info in a new thread

        response.setStatus(200);
    }

    @DeleteMapping("/{serverId:[0-9]+}")
    public void delete(HttpServletResponse response, @PathVariable int serverId) {
        if (!serverManager.servers().containsKey(serverId)) {
            response.setStatus(404);
            return;
        }
        serverManager.deleteServer(serverId);
        response.setStatus(200);
    }
}
