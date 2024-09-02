package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.configs.SlackWebhookConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SlackNotificationManager {
    private final Logger logger;
    private final SlackWebhookConfig config;

    public SlackNotificationManager(Logger logger, SlackWebhookConfig config) {
        this.logger = logger;
        this.config = config;
    }

    /**
     * Sends the Slack notification for when the session is expired and needs to be updated
     *
     * @param verificationUri The verification URI to use
     * @param userCode The user code to use
     */
    public void sendSessionExpiredNotification(String verificationUri, String userCode) {
        sendNotification(config.sessionExpiredMessage().formatted(verificationUri, userCode));
    }

    /**
     * Sends the Slack notification for when a friend has restrictions in place that prevent them from be friend with our account
     *
     * @param username The username of the user
     * @param xuid The XUID of the user
     */
    public void sendFriendRestrictionNotification(String username, String xuid) {
        sendNotification(config.friendRestrictionMessage().formatted(username, xuid));
    }

    private void sendNotification(String message) {
        if (!config.enabled()) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.webhookUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\": \"" + message + "\"}"))
                .build();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to send a slack notification: %s".formatted(message), e);
        }
    }

}
