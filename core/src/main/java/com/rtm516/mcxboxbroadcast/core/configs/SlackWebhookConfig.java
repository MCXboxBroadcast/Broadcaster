package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SlackWebhookConfig(
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("webhook-url") String webhookUrl,
        @JsonProperty("session-expired-message") String sessionExpiredMessage,
        @JsonProperty("friend-restriction-message") String friendRestrictionMessage) {
}
