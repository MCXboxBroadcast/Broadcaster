package com.rtm516.mcxboxbroadcast.core.models.friend;

import java.time.Instant;

public record FriendStatusResponse(
    String xuid,
    Instant addedDateTimeUtc,
    boolean isFavorite,
    Object[] socialNetworks,
    boolean isFollowedByCaller,
    boolean isFollowingCaller,
    boolean isIdentityShared,
    boolean isSquadMateWith,
    boolean isUnfollowingFeed
) {
}
