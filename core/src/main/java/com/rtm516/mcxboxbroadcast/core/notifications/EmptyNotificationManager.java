package com.rtm516.mcxboxbroadcast.core.notifications;

import com.rtm516.mcxboxbroadcast.core.Logger;

public class EmptyNotificationManager extends BaseNotificationManager {
    public EmptyNotificationManager(Logger logger) {
        super(logger, null);
    }

    @Override
    protected void sendNotification(String message) {
        // Do nothing
    }
}
