package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.exceptions.LiveAuthenticationException;
import com.rtm516.mcxboxbroadcast.core.models.LiveDeviceCodeResponse;
import com.rtm516.mcxboxbroadcast.core.models.LiveTokenCache;
import com.rtm516.mcxboxbroadcast.core.models.LiveTokenResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LiveTokenManager {
    private final Path cache;
    private final HttpClient httpClient;
    private final Logger logger;

    public LiveTokenManager(String cache, HttpClient httpClient, Logger logger) {
        this.cache = Paths.get(cache, "live_token.json");
        this.httpClient = httpClient;
        this.logger = logger;
    }

    public boolean verifyTokens() {
        LiveTokenCache tokenCache = getCache();

        if (tokenCache.obtainedOn == 0L) {
            return false;
        }

        long expiry = tokenCache.obtainedOn + (tokenCache.token.expires_in * 1000L);
        boolean valid = (expiry - System.currentTimeMillis()) > 1000;

        if (!valid) {
            try {
                refreshToken();
            } catch (Exception e) {
                logger.error("Failed to refresh live token", e);
                return false;
            }
        }

        return true;
    }

    private void refreshToken() throws Exception {
        String refreshToken = getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new Exception("No refresh token");
        }

        HttpRequest tokenRequest = HttpRequest.newBuilder()
            .uri(Constants.LIVE_TOKEN_REQUEST)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("scope=" + Constants.SCOPE + "&client_id=" + Constants.AUTH_TITLE + "&grant_type=refresh_token&refresh_token=" + refreshToken))
            .build();

        LiveTokenResponse tokenResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString()).body(), LiveTokenResponse.class);
        updateCache(tokenResponse);
    }

    public String getAccessToken() {
        return getCache().token.access_token;
    }

    private String getRefreshToken() {
        return getCache().token.refresh_token;
    }

    public Future<String> authDeviceCode() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            HttpRequest codeRequest = HttpRequest.newBuilder()
                .uri(Constants.LIVE_DEVICE_CODE_REQUEST)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("scope=" + Constants.SCOPE + "&client_id=" + Constants.AUTH_TITLE + "&response_type=device_code"))
                .build();

            LiveDeviceCodeResponse codeResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(codeRequest, HttpResponse.BodyHandlers.ofString()).body(), LiveDeviceCodeResponse.class);

            long expireTime = System.currentTimeMillis() + (codeResponse.expires_in * 1000L);

            logger.info("To sign in, use a web browser to open the page " + codeResponse.verification_uri + " and enter the code " + codeResponse.user_code + " to authenticate.");

            while (System.currentTimeMillis() < expireTime) {
                try {
                    Thread.sleep(codeResponse.interval * 1000L);

                    HttpRequest tokenRequest = HttpRequest.newBuilder()
                        .uri(Constants.LIVE_TOKEN_REQUEST)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("device_code=" + codeResponse.device_code + "&client_id=" + Constants.AUTH_TITLE + "&grant_type=urn:ietf:params:oauth:grant-type:device_code"))
                        .build();

                    HttpResponse<String> response = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

                    LiveTokenResponse tokenResponse = Constants.OBJECT_MAPPER.readValue(response.body(), LiveTokenResponse.class);

                    if (tokenResponse.error != null && !tokenResponse.error.isEmpty()) {
                        if (!tokenResponse.error.equals("authorization_pending")) {
                            completableFuture.completeExceptionally(new LiveAuthenticationException("Failed to get authentication token while waiting for device: " + tokenResponse.error_description + " (" + tokenResponse.error + ")"));
                            break;
                        }
                    } else {
                        updateCache(tokenResponse);
                        completableFuture.complete(tokenResponse.access_token);
                        break;
                    }
                } catch (Exception e) {
                    completableFuture.completeExceptionally(e);
                }
            }

            return null;
        });

        return completableFuture;
    }

    private LiveTokenCache getCache() {
        try {
            return Constants.OBJECT_MAPPER.readValue(Files.readString(cache), LiveTokenCache.class);
        } catch (IOException e) {
            return new LiveTokenCache();
        }
    }

    private void updateCache(LiveTokenResponse tokenResponse) {
        try (FileWriter writer = new FileWriter(cache.toString(), StandardCharsets.UTF_8)) {
            Constants.OBJECT_MAPPER.writeValue(writer, new LiveTokenCache(System.currentTimeMillis(), tokenResponse));
        } catch (IOException e) {
            logger.error("Failed to update live token cache", e);
        }
    }
}
