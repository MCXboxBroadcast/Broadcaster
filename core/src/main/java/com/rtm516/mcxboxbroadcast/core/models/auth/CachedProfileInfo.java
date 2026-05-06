package com.rtm516.mcxboxbroadcast.core.models.auth;

import net.raphimc.minecraftauth.util.Expirable;

import java.util.concurrent.TimeUnit;

public record CachedProfileInfo(String gamertag, String xuid, long expiresAt) implements Expirable {
    private static final long TTL_MS = TimeUnit.MINUTES.toMillis(10);

    public CachedProfileInfo(String gamertag, String xuid) {
        this(gamertag, xuid, System.currentTimeMillis() + TTL_MS);
    }

    @Override
    public long getExpireTimeMs() {
        return expiresAt;
    }
}
