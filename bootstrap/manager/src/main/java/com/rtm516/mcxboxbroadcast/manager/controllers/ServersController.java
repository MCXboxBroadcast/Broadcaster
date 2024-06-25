package com.rtm516.mcxboxbroadcast.manager.controllers;

import com.rtm516.mcxboxbroadcast.manager.BackendManager;
import com.rtm516.mcxboxbroadcast.manager.ServerManager;
import com.rtm516.mcxboxbroadcast.manager.database.repository.ServerCollection;
import com.rtm516.mcxboxbroadcast.manager.models.ServerContainer;
import com.rtm516.mcxboxbroadcast.manager.models.request.ServerUpdateRequest;
import com.rtm516.mcxboxbroadcast.manager.models.response.ServerInfoResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
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
    private final ServerCollection serverCollection;

    @Autowired
    public ServersController(ServerManager serverManager, BackendManager backendManager, ServerCollection serverCollection) {
        this.serverManager = serverManager;
        this.backendManager = backendManager;
        this.serverCollection = serverCollection;
    }

    @GetMapping("")
    public List<ServerInfoResponse> servers(HttpServletResponse response) {
        response.setStatus(200);
        return serverManager.servers().values().stream().map(ServerContainer::toResponse).toList();
    }

    @PostMapping("/create")
    public ObjectId create(HttpServletResponse response) {
        response.setStatus(200);
        ServerContainer serverContainer = serverManager.addServer();
        return serverContainer.server()._id();
    }

    @GetMapping("/{serverId:[a-z0-9]+}")
    public ServerInfoResponse server(HttpServletResponse response, @PathVariable ObjectId serverId) {
        if (!serverManager.servers().containsKey(serverId)) {
            response.setStatus(404);
            return null;
        }
        response.setStatus(200);
        return serverManager.servers().get(serverId).toResponse();
    }

    @PostMapping("/{serverId:[a-z0-9]+}")
    public void update(HttpServletResponse response, @PathVariable ObjectId serverId, @RequestBody ServerUpdateRequest serverUpdateRequest) {
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

        // Update the hostname and port then save the server to the database
        ServerContainer serverContainer = serverManager.servers().get(serverId);
        serverContainer.server().hostname(serverUpdateRequest.hostname());
        serverContainer.server().port(serverUpdateRequest.port());
        serverCollection.save(serverContainer.server());

        // Update the session info in a new thread
        backendManager.scheduledThreadPool().execute(serverContainer::updateSessionInfo);

        response.setStatus(200);
    }

    @DeleteMapping("/{serverId:[a-z0-9]+}")
    public void delete(HttpServletResponse response, @PathVariable ObjectId serverId) {
        if (!serverManager.servers().containsKey(serverId)) {
            response.setStatus(404);
            return;
        }
        serverManager.deleteServer(serverId);
        response.setStatus(200);
    }
}
