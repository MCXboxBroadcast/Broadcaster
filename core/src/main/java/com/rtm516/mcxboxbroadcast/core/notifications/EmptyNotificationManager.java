package com.rtm516.mcxboxbroadcast.core.notifications;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.configs.NotificationConfig;

public class EmptyNotificationManager extends BaseNotificationManager {
    public EmptyNotificationManager(Logger logger) {
        super(logger, new NotificationConfig(false, "", "", ""));
    }

    @Override
    protected void sendNotification(String message) {
        // Do nothing
    }
}
