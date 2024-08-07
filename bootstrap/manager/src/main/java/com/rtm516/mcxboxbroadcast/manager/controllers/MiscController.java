package com.rtm516.mcxboxbroadcast.manager.controllers;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.manager.BackendManager;
import com.rtm516.mcxboxbroadcast.manager.config.MainConfig;
import com.rtm516.mcxboxbroadcast.manager.database.model.User;
import com.rtm516.mcxboxbroadcast.manager.models.response.ManagerInfoResponse;
import com.rtm516.mcxboxbroadcast.manager.models.response.UserInfoResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.Map;

@RestController
public class MiscController {
    private final BackendManager backendManager;

    @Autowired
    public MiscController(BackendManager backendManager) {
        this.backendManager = backendManager;
    }

    @GetMapping("/health")
    public void health(HttpServletResponse response) {
        response.setStatus(200);
    }

    @GetMapping("/api/info")
    public ManagerInfoResponse info(HttpServletResponse response) {
        response.setStatus(200);
        return new ManagerInfoResponse(BuildData.VERSION, (int) (ManagementFactory.getRuntimeMXBean().getUptime() / 1000));
    }

    @GetMapping("/api/settings")
    public Map<String, String> settings(HttpServletResponse response) {
        response.setStatus(200);

        MainConfig config = backendManager.config();

        return Map.of(
            "Authentication", config.auth() ? "Enabled" : "Disabled",
            "Update time (s)", "Server: " + config.updateTime().server() + ", Session: " + config.updateTime().session() + ", Stats: " + config.updateTime().stats() + ", Friend: " + config.updateTime().friend(),
            "Worker Threads", String.valueOf(config.workerThreads()),
            "Log lines", String.valueOf(config.logSize())
        );
    }

    @GetMapping("/api/user")
    public UserInfoResponse user(HttpServletResponse response) {
        if (!backendManager.config().auth()) {
            response.setStatus(404);
            return null;
        }

        response.setStatus(200);
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new UserInfoResponse(user.getId(), user.getUsername());
    }
}
