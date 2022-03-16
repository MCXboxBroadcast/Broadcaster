package com.rtm516.mcxboxbroadcast.core.models;

public final class CreateHandleRequestSessionRef {
    public final String scid;
    public final String templateName;
    public final String name;

    public CreateHandleRequestSessionRef(String scid, String templateName, String name) {
        this.scid = scid;
        this.templateName = templateName;
        this.name = name;
    }
}
