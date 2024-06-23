package com.rtm516.mcxboxbroadcast.manager;

import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.manager.models.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Scope
public class BotManager {
    private final Map<Integer, Bot> bots = new HashMap<>();
    private final ServerManager serverManager;
    private final BackendManager backendManager;

    @Autowired
    public BotManager(ServerManager serverManager, BackendManager backendManager) {
        this.serverManager = serverManager;
        this.backendManager = backendManager;

        bots.put(0, new Bot(this, new Bot.Info(0, "CrimpyLace85127", "2533274813789647", 0)));
        bots.put(1, new Bot(this, new Bot.Info(1)));

        // Start the bots in a new thread so the web server can start
        backendManager.scheduledThreadPool().execute(() -> {
            for (Bot bot : bots.values()) {
                bot.start();
            }
        });
    }

    public Map<Integer, Bot> bots() {
        return bots;
    }

    public String logs(int botId) {
        return bots.get(botId).logs();
    }

    public SessionInfo serverSessionInfo(int id) {
        return serverManager.servers().get(id).sessionInfo();
    }

    public Bot addBot() {
        Bot bot = new Bot(this, new Bot.Info(bots.size()));
        bots.put(bot.info().id(), bot);
        return bot;
    }

    public void deleteBot(int botId) {
        bots.get(botId).stop();
        // TODO Cleanup cache folder
        bots.remove(botId);
    }
}
