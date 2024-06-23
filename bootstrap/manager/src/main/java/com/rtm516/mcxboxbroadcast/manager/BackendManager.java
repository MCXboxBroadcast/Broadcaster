package com.rtm516.mcxboxbroadcast.manager;

import org.java_websocket.util.NamedThreadFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Scope
public class BackendManager {
    private final ScheduledExecutorService scheduledThreadPool;

    public BackendManager() {
        // TODO Allow configuration of thread pool size
        scheduledThreadPool = Executors.newScheduledThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() * 3 / 8), new NamedThreadFactory("MCXboxBroadcast Manager Thread"));
    }

    public ScheduledExecutorService scheduledThreadPool() {
        return scheduledThreadPool;
    }
}
