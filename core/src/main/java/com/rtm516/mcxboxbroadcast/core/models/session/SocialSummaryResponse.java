package com.rtm516.mcxboxbroadcast.core.models.session;

public record SocialSummaryResponse(
    int targetFollowingCount,
    int targetFollowerCount,
    int targetFriendCount,
    boolean isCallerFollowingTarget,
    boolean isTargetFollowingCaller,
    boolean hasCallerMarkedTargetAsFavorite,
    boolean hasCallerMarkedTargetAsKnown,
    String legacyFriendStatus,
    long availablePeopleSlots,
    boolean isFriend,
    long availableFollowingSlots,
    long availableFriendSlots,
    int recentChangeCount,
    String watermark
) {
}
