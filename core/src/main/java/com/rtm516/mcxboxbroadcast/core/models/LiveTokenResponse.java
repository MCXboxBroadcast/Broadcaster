package com.rtm516.mcxboxbroadcast.core.models;

public final class LiveTokenResponse {
    private final String token_type;
    private final String scope;
    private final int expires_in;
    private final String access_token;
    private final String refresh_token;
    private final String id_token;
    private final String error;

    public LiveTokenResponse(String token_type, String scope, int expires_in, String access_token, String refresh_token, String id_token, String error) {
        this.token_type = token_type;
        this.scope = scope;
        this.expires_in = expires_in;
        this.access_token = access_token;
        this.refresh_token = refresh_token;
        this.id_token = id_token;
        this.error = error;
    }

    public String token_type() {
        return token_type;
    }

    public String scope() {
        return scope;
    }

    public int expires_in() {
        return expires_in;
    }

    public String access_token() {
        return access_token;
    }

    public String refresh_token() {
        return refresh_token;
    }

    public String id_token() {
        return id_token;
    }

    public String error() {
        return error;
    }
}
