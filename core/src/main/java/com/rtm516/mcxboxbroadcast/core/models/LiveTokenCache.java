package com.rtm516.mcxboxbroadcast.core.models;

public final class LiveTokenCache {
    private final long obtainedOn;
    private final LiveTokenResponse token;

    public LiveTokenCache(long obtainedOn, LiveTokenResponse token) {
        this.obtainedOn = obtainedOn;
        this.token = token;
    }

    public long obtainedOn() {
        return obtainedOn;
    }

    public LiveTokenResponse token() {
        return token;
    }
}
