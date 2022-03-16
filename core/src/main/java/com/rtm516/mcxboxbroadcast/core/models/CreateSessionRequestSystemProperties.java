package com.rtm516.mcxboxbroadcast.core.models;

public final class CreateSessionRequestSystemProperties {
    public final String joinRestriction;
    public final String readRestriction;
    public final boolean closed;

    public CreateSessionRequestSystemProperties(String joinRestriction, String readRestriction, boolean closed) {
        this.joinRestriction = joinRestriction;
        this.readRestriction = readRestriction;
        this.closed = closed;
    }
}
