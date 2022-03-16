package com.rtm516.mcxboxbroadcast.core.models;

import java.util.Objects;

public final class LiveDeviceCodeResponse {
    private final String user_code;
    private final String device_code;
    private final String verification_uri;
    private final int interval;
    private final int expires_in;

    public LiveDeviceCodeResponse(String user_code, String device_code, String verification_uri, int interval, int expires_in) {
        this.user_code = user_code;
        this.device_code = device_code;
        this.verification_uri = verification_uri;
        this.interval = interval;
        this.expires_in = expires_in;
    }

    public String user_code() {
        return user_code;
    }

    public String device_code() {
        return device_code;
    }

    public String verification_uri() {
        return verification_uri;
    }

    public int interval() {
        return interval;
    }

    public int expires_in() {
        return expires_in;
    }
}
