package com.rtm516.mcxboxbroadcast.core.models;

public class CreateHandleRequest {
    public int version;
    public String type;
    public SessionRef sessionRef;

    public CreateHandleRequest(int version, String type, SessionRef sessionRef) {
        this.version = version;
        this.type = type;
        this.sessionRef = sessionRef;
    }

    public static class SessionRef {
        public String scid;
        public String templateName;
        public String name;

        public SessionRef(String scid, String templateName, String name) {
            this.scid = scid;
            this.templateName = templateName;
            this.name = name;
        }
    }
}
