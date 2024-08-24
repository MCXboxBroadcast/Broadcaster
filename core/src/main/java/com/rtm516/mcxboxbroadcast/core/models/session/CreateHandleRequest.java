package com.rtm516.mcxboxbroadcast.core.models.session;

public class CreateHandleRequest {
    public final int version;
    public final String type;
    public final SessionRef sessionRef;

    public CreateHandleRequest(int version, String type, SessionRef sessionRef) {
        this.version = version;
        this.type = type;
        this.sessionRef = sessionRef;
    }
}
