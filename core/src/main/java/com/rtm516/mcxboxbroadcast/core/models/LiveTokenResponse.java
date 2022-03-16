package com.rtm516.mcxboxbroadcast.core.models;

public class LiveTokenResponse {
    public String token_type;
    public String scope;
    public int expires_in;
    public String access_token;
    public String refresh_token;
    public String id_token;
    public String user_id;

    public String error;
    public String error_description;
    public String correlation_id;
}
