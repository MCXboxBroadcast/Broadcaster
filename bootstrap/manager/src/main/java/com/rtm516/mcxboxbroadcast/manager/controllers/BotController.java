package com.rtm516.mcxboxbroadcast.manager.controllers;

import com.rtm516.mcxboxbroadcast.manager.BackendManager;
import com.rtm516.mcxboxbroadcast.manager.BotManager;
import com.rtm516.mcxboxbroadcast.manager.ServerManager;
import com.rtm516.mcxboxbroadcast.manager.models.Bot;
import com.rtm516.mcxboxbroadcast.manager.models.BotUpdate;
import com.rtm516.mcxboxbroadcast.manager.models.ServerUpdate;
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
@RequestMapping("/api/bots")
public class BotController {

    private final BotManager botManager;
    private final ServerManager serverManager;
    private final BackendManager backendManager;

    @Autowired
    public BotController(final BotManager botManager, ServerManager serverManager, BackendManager backendManager) {
        this.botManager = botManager;
        this.serverManager = serverManager;
        this.backendManager = backendManager;
    }

    @GetMapping("")
    public List<Bot.Info> bots(HttpServletResponse response) {
        response.setStatus(200);
        return botManager.bots().values().stream().map(Bot::info).toList();
    }

    @PostMapping("/create")
    public int create(HttpServletResponse response) {
        response.setStatus(200);
        Bot bot = botManager.addBot();
        backendManager.scheduledThreadPool().execute(() -> bot.start()); // Start the bot in a new thread
        return bot.info().id();
    }

    @GetMapping("/{botId:[0-9]+}")
    public Bot.Info bot(HttpServletResponse response, @PathVariable int botId) {
        if (!botManager.bots().containsKey(botId)) {
            response.setStatus(404);
            return null;
        }
        response.setStatus(200);
        return botManager.bots().get(botId).info();
    }

    @PostMapping("/{botId:[0-9]+}")
    public void update(HttpServletResponse response, @PathVariable int botId, @RequestBody BotUpdate botUpdate) {
        if (!botManager.bots().containsKey(botId)) {
            response.setStatus(404);
            return;
        }

        if (!serverManager.servers().containsKey(botUpdate.serverId())) {
            response.setStatus(400);
            return;
        }

        botManager.bots().get(botId).info().serverId(botUpdate.serverId());

        backendManager.scheduledThreadPool().execute(() -> botManager.bots().get(botId).updateSessionInfo()); // Update the session info in a new thread

        response.setStatus(200);
    }

    @PostMapping("/{botId:[0-9]+}/start")
    public void start(HttpServletResponse response, @PathVariable int botId) {
        if (!botManager.bots().containsKey(botId)) {
            response.setStatus(404);
            return;
        }
        botManager.bots().get(botId).start();
        response.setStatus(200);
    }

    @PostMapping("/{botId:[0-9]+}/stop")
    public void stop(HttpServletResponse response, @PathVariable int botId) {
        if (!botManager.bots().containsKey(botId)) {
            response.setStatus(404);
            return;
        }
        botManager.bots().get(botId).stop();
        response.setStatus(200);
    }

    @PostMapping("/{botId:[0-9]+}/restart")
    public void restart(HttpServletResponse response, @PathVariable int botId) {
        if (!botManager.bots().containsKey(botId)) {
            response.setStatus(404);
            return;
        }
        botManager.bots().get(botId).restart();
        response.setStatus(200);
    }

    @DeleteMapping("/{botId:[0-9]+}")
    public void delete(HttpServletResponse response, @PathVariable int botId) {
        if (!botManager.bots().containsKey(botId)) {
            response.setStatus(404);
            return;
        }
        botManager.deleteBot(botId);
        response.setStatus(200);
    }

    @GetMapping("/{botId:[0-9]+}/logs")
    public String logs(HttpServletResponse response, @PathVariable int botId) {
        if (!botManager.bots().containsKey(botId)) {
            response.setStatus(404);
            return "";
        }
        response.setStatus(200);
        return botManager.logs(botId);
    }
}
