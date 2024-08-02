package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.manager.database.model.Bot;
import com.rtm516.mcxboxbroadcast.manager.database.repository.BotCollection;
import com.rtm516.mcxboxbroadcast.manager.models.BotContainer;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Scope
public class BotManager {
    private final Map<ObjectId, BotContainer> bots = new HashMap<>();
    private final ServerManager serverManager;
    private final BackendManager backendManager;
    private final BotCollection botCollection;

    @Autowired
    public BotManager(ServerManager serverManager, BackendManager backendManager, BotCollection botCollection) {
        this.serverManager = serverManager;
        this.backendManager = backendManager;
        this.botCollection = botCollection;

        // Load all bots from the database
        botCollection.findAll().forEach(bot -> {
            bots.put(bot._id(), new BotContainer(this, bot));
        });

        // Start the bots in a new thread so the web server can start
        backendManager.scheduledThreadPool().execute(() -> {
            for (BotContainer botContainer : bots.values()) {
                botContainer.start();
            }
        });
    }

    /**
     * Get the bot collection
     *
     * @return the bot collection
     */
    public BotCollection botCollection() {
        return botCollection;
    }

    /**
     * Get the server manager
     *
     * @return the server manager
     */
    public BackendManager backendManager() {
        return backendManager;
    }

    /**
     * Get the current bots
     *
     * @return the current bots
     */
    public Map<ObjectId, BotContainer> bots() {
        return bots;
    }

    /**
     * Get the logs for a bot
     *
     * @param botId the bot ID
     * @return the logs
     */
    public String logs(ObjectId botId) {
        return bots.get(botId).logs();
    }

    /**
     * Get the server session info
     *
     * @param id the server ID
     * @return the server session info
     */
    public SessionInfo serverSessionInfo(ObjectId id) {
        return serverManager.servers().get(id).sessionInfo();
    }

    /**
     * Add a new bot
     *
     * @return the bot container
     */
    public BotContainer addBot() {
        Bot bot = botCollection.save(new Bot(serverManager.firstServer()));

        BotContainer botContainer = new BotContainer(this, bot);
        bots.put(bot._id(), botContainer);
        return botContainer;
    }

    /**
     * Delete a bot
     *
     * @param botId the bot ID
     */
    public void deleteBot(ObjectId botId) {
        bots.get(botId).stop();
        bots.remove(botId);
        botCollection.deleteById(botId);
    }
}
