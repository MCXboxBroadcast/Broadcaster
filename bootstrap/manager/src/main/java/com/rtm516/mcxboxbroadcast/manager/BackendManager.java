package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.manager.database.model.Server;
import com.rtm516.mcxboxbroadcast.manager.database.model.User;
import com.rtm516.mcxboxbroadcast.manager.database.repository.ServerCollection;
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
    private final ServerCollection serverCollection;

    @Autowired
    public BackendManager(UserCollection userCollection, PasswordEncoder passwordEncoder, ServerCollection serverCollection) {
        this.userCollection = userCollection;
        this.serverCollection = serverCollection;

        // TODO Allow configuration of thread pool size
        this.scheduledThreadPool = Executors.newScheduledThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() * 3 / 8), new NamedThreadFactory("MCXboxBroadcast Manager Thread"));


        // Create the admin user if it doesn't exist
        if (authEnabled() && userCollection.findUserByUsername("admin").isEmpty()) {
            userCollection.save(new User("admin", passwordEncoder.encode("password")));
        }

        // Create the default server if it doesn't exist
        if (serverCollection.count() == 0) {
            serverCollection.save(new Server("test.geysermc.org", 19132));
        }
    }

    public ScheduledExecutorService scheduledThreadPool() {
        return scheduledThreadPool;
    }

    public boolean authEnabled() {
        return System.getenv("SECURITY") == null || System.getenv("SECURITY").equals("true");
    }

    /**
     * Get the time in seconds between each update
     * Full time is the bot update time
     * Half time is the server update time
     * <p>
     * TODO Make configurable
     *
     * @return the time in seconds between each update
     */
    public int updateTime() {
        return 30;
    }
}
