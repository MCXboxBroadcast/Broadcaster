package com.rtm516.mcxboxbroadcast.manager.models.response;

import com.rtm516.mcxboxbroadcast.core.models.session.FollowerResponse;

public record FriendResponse(
    String xuid,
    boolean isFollowingCaller,
    boolean isFollowedByCaller,
    String gamertag,
    String presenceState

) {
    public FriendResponse(FollowerResponse.Person person) {
        this(person.xuid, person.isFollowingCaller, person.isFollowedByCaller, person.gamertag, person.presenceState);
    }
}
