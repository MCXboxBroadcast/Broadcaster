package com.rtm516.mcxboxbroadcast.core.models.session;

import java.util.Map;

public class CreateHandleRequest {
    public final int version;
    public final String type;
    public final SessionRef sessionRef;
    public final String invitedXuid;
    public final Map<String, String> inviteAttributes;

    public CreateHandleRequest(int version, String type, SessionRef sessionRef) {
        this(version, type, sessionRef, null, null);
    }

    public CreateHandleRequest(int version, String type, SessionRef sessionRef, String invitedXuid, Map<String, String> inviteAttributes) {
        this.version = version;
        this.type = type;
        this.sessionRef = sessionRef;
        this.invitedXuid = invitedXuid;
        this.inviteAttributes = inviteAttributes;
    }
}
