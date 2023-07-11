package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.configs.ExtensionConfig;
import com.rtm516.mcxboxbroadcast.core.configs.StandaloneConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.FollowerResponse;


public class FriendUtils {

    public static void autoFriend(SessionManager sessionManager, Logger logger, ExtensionConfig config) {
        // Make sure the connection is still active
        sessionManager.checkConnection();

        // Auto Friend Checker
        try {
            for (FollowerResponse.Person person : sessionManager.getXboxFriends(config.friendSyncConfig.autoFollow, config.friendSyncConfig.autoUnfollow)) {
                // Follow the person back
                if (config.friendSyncConfig.autoFollow && person.isFollowingCaller && !person.isFollowedByCaller) {
                    logger.info("Added " + person.displayName + " (" + person.xuid + ") as a friend");
                    sessionManager.addXboxFriend(person.xuid);
                }

                // Unfollow the person
                if (config.friendSyncConfig.autoUnfollow && !person.isFollowingCaller && person.isFollowedByCaller) {
                    logger.info("Removed " + person.displayName + " (" + person.xuid + ") as a friend");
                    sessionManager.removeXboxFriend(person.xuid);
                }
            }
        } catch (XboxFriendsException e) {
            logger.error("Failed to sync friends", e);
        }

    }
    public static void autoFriend(SessionManager sessionManager, Logger logger, StandaloneConfig config) {

        // Make sure the connection is still active
        sessionManager.checkConnection();

        // Auto Friend Checker
        try {
            for (FollowerResponse.Person person : sessionManager.getXboxFriends(config.friendSyncConfig.autoFollow, config.friendSyncConfig.autoUnfollow)) {
                // Follow the person back
                if (config.friendSyncConfig.autoFollow && person.isFollowingCaller && !person.isFollowedByCaller) {
                    logger.info("Added " + person.displayName + " (" + person.xuid + ") as a friend");
                    sessionManager.addXboxFriend(person.xuid);
                }

                // Unfollow the person
                if (config.friendSyncConfig.autoUnfollow && !person.isFollowingCaller && person.isFollowedByCaller) {
                    logger.info("Removed " + person.displayName + " (" + person.xuid + ") as a friend");
                    sessionManager.removeXboxFriend(person.xuid);
                }
            }
        } catch (XboxFriendsException e) {
            logger.error("Failed to sync friends", e);
        }

    }

}