package com.rtm516.mcxboxbroadcast.core.models.session;

public record SessionProperties(
    SessionSystemProperties system,
    SessionCustomProperties custom
) {
}
