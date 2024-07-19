package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.manager.config.MainConfig;
import com.rtm516.mcxboxbroadcast.manager.database.model.Server;
import com.rtm516.mcxboxbroadcast.manager.database.model.User;
import com.rtm516.mcxboxbroadcast.manager.database.repository.ServerCollection;
import com.rtm516.mcxboxbroadcast.manager.database.repository.UserCollection;
import org.java_websocket.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Scope
public class BackendManager {
    private final UserCollection userCollection;
    private final ServerCollection serverCollection;
    private final MainConfig mainConfig;

    private final ScheduledExecutorService scheduledThreadPool;

    public static final Logger LOGGER = LoggerFactory.getLogger("Backend");

    @Autowired
    public BackendManager(UserCollection userCollection, PasswordEncoder passwordEncoder, ServerCollection serverCollection, MainConfig mainConfig) {
        this.userCollection = userCollection;
        this.serverCollection = serverCollection;
        this.mainConfig = mainConfig;

        this.scheduledThreadPool = Executors.newScheduledThreadPool(mainConfig.workerThreads(), new NamedThreadFactory("MCXboxBroadcast Manager Thread"));

        // Create the admin user if it doesn't exist
        if (mainConfig.auth() && userCollection.findUserByUsername("admin").isEmpty()) {
            userCollection.save(new User("admin", passwordEncoder.encode("password")));
        }

        // Create the default server if it doesn't exist
        if (serverCollection.count() == 0) {
            serverCollection.save(new Server("test.geysermc.org", 19132));
        }
    }

    /**
     * Get the scheduled thread pool
     *
     * @return the scheduled thread pool
     */
    public ScheduledExecutorService scheduledThreadPool() {
        return scheduledThreadPool;
    }

    /**
     * Get the main config
     *
     * @return the main config
     */
    public MainConfig config() {
        return mainConfig;
    }
}
