package com.rtm516.mcxboxbroadcast.core.models.session;

public record SessionSystemProperties(
    String joinRestriction,
    String readRestriction,
    boolean closed
) {
    public SessionSystemProperties() {
        this("followed", "followed", false);
    }
}
