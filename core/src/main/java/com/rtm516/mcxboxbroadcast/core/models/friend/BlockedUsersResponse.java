package com.rtm516.mcxboxbroadcast.core.models.friend;

import java.util.List;

public record BlockedUsersResponse(List<User> users) {
    public record User(String xuid) {
    }
}
