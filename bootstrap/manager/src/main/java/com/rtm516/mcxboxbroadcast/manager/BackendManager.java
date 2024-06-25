package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.manager.database.model.User;
import com.rtm516.mcxboxbroadcast.manager.database.repository.UserCollection;
import org.java_websocket.util.NamedThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Scope
public class BackendManager {
    private final ScheduledExecutorService scheduledThreadPool;
    private final UserCollection userCollection;

    @Autowired
    public BackendManager(UserCollection userCollection, PasswordEncoder passwordEncoder) {
        // TODO Allow configuration of thread pool size
        this.scheduledThreadPool = Executors.newScheduledThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() * 3 / 8), new NamedThreadFactory("MCXboxBroadcast Manager Thread"));

        this.userCollection = userCollection;

        // Create the admin user if it doesn't exist
        if (authEnabled() && userCollection.findUserByUsername("admin").isEmpty()) {
            userCollection.save(new User("admin", passwordEncoder.encode("password")));
        }
    }

    public ScheduledExecutorService scheduledThreadPool() {
        return scheduledThreadPool;
    }

    public boolean authEnabled() {
        return System.getenv("SECURITY") == null || System.getenv("SECURITY").equals("true");
    }
}
