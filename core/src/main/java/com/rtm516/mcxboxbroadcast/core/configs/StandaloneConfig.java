package com.rtm516.mcxboxbroadcast.core.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StandaloneConfig(
    @JsonProperty("session") SessionConfig session,
    @JsonProperty("debug-log") boolean debugLog,
    @JsonProperty("suppress-session-update-info") boolean suppressSessionUpdateInfo,
    @JsonProperty("friend-sync") FriendSyncConfig friendSync,
    @JsonProperty("slack-webhook") SlackWebhookConfig slackWebhook) {
}
