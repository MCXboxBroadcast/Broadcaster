package com.rtm516.mcxboxbroadcast.core.storage;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public interface StorageManager {
    String cache() throws IOException;
    void cache(String data) throws IOException;

    String subSessions() throws IOException;
    void subSessions(String data) throws IOException;

    String lastSessionResponse() throws IOException;
    void lastSessionResponse(String data) throws IOException;

    String currentSessionResponse() throws IOException;
    void currentSessionResponse(String data) throws IOException;

    StorageManager subSession(String id);

    File screenshot();

    void cleanup() throws IOException;

    PlayerHistoryStorage playerHistory();

    interface PlayerHistoryStorage {
        boolean isFirstRun();
        Instant lastSeen(String xuid) throws IOException;
        void lastSeen(String xuid, Instant lastSeen) throws IOException;
        void clear(String xuid) throws IOException;
        Map<String, Instant> all() throws IOException;
    }
}
