package com.rtm516.mcxboxbroadcast.core.models.auth;

public record SessionStartResponse(Result result) {
    public record Result(String authorizationHeader) {}
}
