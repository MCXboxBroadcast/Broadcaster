package com.rtm516.mcxboxbroadcast.core.models;

public final class CreateHandleRequest {
    public final int version;
    public final String type;
    public final CreateHandleRequestSessionRef sessionRef;

    public CreateHandleRequest(int version, String type, CreateHandleRequestSessionRef sessionRef) {
        this.version = version;
        this.type = type;
        this.sessionRef = sessionRef;
    }
}
