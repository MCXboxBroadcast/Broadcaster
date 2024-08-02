package com.rtm516.mcxboxbroadcast.manager.models;

import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;

import java.io.IOException;

public class MongoStorageManager implements StorageManager {
    private final BotContainer botContainer;

    private String currentSessionResponse;

    public MongoStorageManager(BotContainer botContainer) {
        this.botContainer = botContainer;
        this.currentSessionResponse = "";
    }

    @Override
    public String cache() throws IOException {
        return botContainer.bot().authCache();
    }

    @Override
    public void cache(String data) throws IOException {
        botContainer.bot().authCache(data);
        botContainer.save();
    }

    @Override
    public String subSessions() throws IOException {
        // Not needed for this implementation
        return "";
    }

    @Override
    public void subSessions(String data) throws IOException {
        // Not needed for this implementation
    }

    @Override
    public String lastSessionResponse() throws IOException {
        // Not needed for this implementation
        return "";
    }

    @Override
    public void lastSessionResponse(String data) throws IOException {
        // Not needed for this implementation
    }

    @Override
    public String currentSessionResponse() throws IOException {
        return currentSessionResponse;
    }

    @Override
    public void currentSessionResponse(String data) throws IOException {
        currentSessionResponse = data;
    }

    @Override
    public StorageManager subSession(String id) {
        // Not needed for this implementation
        return null;
    }

    @Override
    public void cleanup() throws IOException {
        // Not needed for this implementation
    }

    @Override
    public String liveToken() throws IOException {
        // Not needed for this implementation
        return "";
    }

    @Override
    public void liveToken(String data) throws IOException {
        // No longer for this implementation
    }

    @Override
    public void xboxToken(String data) throws IOException {
        // No longer for this implementation
    }
}
