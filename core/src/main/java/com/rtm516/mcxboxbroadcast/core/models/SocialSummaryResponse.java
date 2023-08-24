package com.rtm516.mcxboxbroadcast.core.models;

public record SocialSummaryResponse(
    int targetFollowingCount,
    int targetFollowerCount,
    boolean isCallerFollowingTarget,
    boolean isTargetFollowingCaller,
    boolean hasCallerMarkedTargetAsFavorite,
    boolean hasCallerMarkedTargetAsKnown,
    String legacyFriendStatus,
    int availablePeopleSlots,
    int recentChangeCount,
    String watermark
) {
}
