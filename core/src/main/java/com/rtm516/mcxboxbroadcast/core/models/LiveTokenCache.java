package com.rtm516.mcxboxbroadcast.core.models;

public class LiveTokenCache {
    public long obtainedOn;
    public LiveTokenResponse token;

    public LiveTokenCache() {
    }

    public LiveTokenCache(long obtainedOn, LiveTokenResponse token) {
        this.obtainedOn = obtainedOn;
        this.token = token;
    }
}
