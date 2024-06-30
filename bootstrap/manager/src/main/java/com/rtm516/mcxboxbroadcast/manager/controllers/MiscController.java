package com.rtm516.mcxboxbroadcast.manager.controllers;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.manager.models.response.ManagerInfoResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;

@RestController
public class MiscController {
    @GetMapping("/health")
    public void health(HttpServletResponse response) {
        response.setStatus(200);
    }

    @GetMapping("/api/info")
    public ManagerInfoResponse info(HttpServletResponse response) {
        response.setStatus(200);
        return new ManagerInfoResponse(BuildData.VERSION, (int) (ManagementFactory.getRuntimeMXBean().getUptime() / 1000));
    }
}
