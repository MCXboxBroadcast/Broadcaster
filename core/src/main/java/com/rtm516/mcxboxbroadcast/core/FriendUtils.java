package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.configs.FriendSyncConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.FollowerResponse;


public class FriendUtils {
    public static void autoFriend(SessionManager sessionManager, Logger logger, FriendSyncConfig config) {
        // Make sure the connection is still active
        sessionManager.checkConnection();

        // Auto Friend Checker
        try {
            for (FollowerResponse.Person person : sessionManager.getXboxFriends(config.autoFollow, config.autoUnfollow)) {
                // Follow the person back
                if (config.autoFollow && person.isFollowingCaller && !person.isFollowedByCaller) {
                    if (sessionManager.addXboxFriend(person.xuid)) {
                        logger.info("Added " + person.displayName + " (" + person.xuid + ") as a friend");
                    } else {
                        logger.warning("Failed to add " + person.displayName + " (" + person.xuid + ") as a friend");
                    }
                }

                // Unfollow the person
                if (config.autoUnfollow && !person.isFollowingCaller && person.isFollowedByCaller) {
                    if (sessionManager.removeXboxFriend(person.xuid)) {
                        logger.info("Removed " + person.displayName + " (" + person.xuid + ") as a friend");
                    } else {
                        logger.warning("Failed to remove " + person.displayName + " (" + person.xuid + ") as a friend");
                    }
                }
            }
        } catch (XboxFriendsException e) {
            logger.error("Failed to sync friends", e);
        }
    }
}
