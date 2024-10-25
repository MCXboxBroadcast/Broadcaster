package com.rtm516.mcxboxbroadcast.core.notifications;

public interface NotificationManager {
    void sendSessionExpiredNotification(String verificationUri, String userCode);

    void sendFriendRestrictionNotification(String username, String xuid);
}
