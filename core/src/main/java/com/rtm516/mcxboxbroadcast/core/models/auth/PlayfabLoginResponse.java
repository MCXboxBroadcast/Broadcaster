package com.rtm516.mcxboxbroadcast.core.models.auth;

public record PlayfabLoginResponse(int code, Data data) {
    public record Data(String SessionTicket) {}
}
