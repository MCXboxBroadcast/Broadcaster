package com.rtm516.mcxboxbroadcast.core.notifications;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.configs.NotificationConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SlackNotificationManager extends BaseNotificationManager {
    public SlackNotificationManager(Logger logger, NotificationConfig config) {
        super(logger, config);
    }

    public void sendNotification(String message) {
        if (!config.enabled()) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.webhookUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\": \"" + message.replace("\n", "\\n") + "\"}"))
                .build();

        // TODO When moving to Java 21 use try-with-resources
        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to send a slack notification: %s".formatted(message), e);
        }
    }
}
