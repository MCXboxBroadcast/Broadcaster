package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.FollowerResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FriendConfig {

    @JsonProperty("friend-sync")
    public FriendSyncConfig friendSyncConfig;

    public static class FriendSyncConfig {
        @JsonProperty("update-interval")
        public int updateInterval;
        @JsonProperty("auto-follow")
        public boolean autoFollow;
        @JsonProperty("auto-unfollow")
        public boolean autoUnfollow;

    }

    public static void autoFriend(SessionManager sessionManager, FriendConfig friendConfig, Logger logger) {
        // Make sure the connection is still active
        sessionManager.checkConnection();

        // Auto Friend Checker
        try {
            for (FollowerResponse.Person person : sessionManager.getXboxFriends(friendConfig.friendSyncConfig.autoFollow, friendConfig.friendSyncConfig.autoUnfollow)) {
                // Follow the person back
                if (friendConfig.friendSyncConfig.autoFollow && person.isFollowingCaller && !person.isFollowedByCaller) {
                    logger.info("Added " + person.displayName + " (" + person.xuid + ") as a friend");
                    sessionManager.addXboxFriend(person.xuid);
                }

                // Unfollow the person
                if (friendConfig.friendSyncConfig.autoUnfollow && !person.isFollowingCaller && person.isFollowedByCaller) {
                    logger.info("Removed " + person.displayName + " (" + person.xuid + ") as a friend");
                    sessionManager.removeXboxFriend(person.xuid);
                }
            }
        } catch (XboxFriendsException e) {
            logger.error("Failed to sync friends", e);
        }

    }

}
