package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtm516.mcxboxbroadcast.core.exceptions.AgeVerificationException;
import com.rtm516.mcxboxbroadcast.core.models.auth.XblUsersMeProfileRequest;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.util.MinecraftAuth4To5Migrator;
import net.raphimc.minecraftauth.util.holder.listener.BasicChangeListener;
import net.raphimc.minecraftauth.util.http.exception.InformativeHttpRequestException;

import java.io.IOException;
import java.util.function.Consumer;

public class AuthManager {
    private final NotificationManager notificationManager;
    private final StorageManager storageManager;
    private final Logger logger;

    private BedrockAuthManager authManager;
    private Runnable onDeviceTokenRefreshCallback;

    private String gamertag;
    private String xuid;

    /**
     * Create an instance of AuthManager
     *
     * @param notificationManager The notification manager to use for sending messages
     * @param storageManager      The storage manager to use for storing data
     * @param logger              The logger to use for outputting messages
     */
    public AuthManager(NotificationManager notificationManager, StorageManager storageManager, Logger logger) {
        this.notificationManager = notificationManager;
        this.storageManager = storageManager;
        this.logger = logger.prefixed("Auth");

        this.authManager = null;
    }

    /**
     * Follow the auth flow to get the Xbox token and store it
     */
    private void initialise() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();

        // Try to load xboxToken from cache.json if is not already loaded
        if (authManager == null) {
            try {
                String cacheData = storageManager.cache();
                if (!cacheData.isBlank()) {
                    JsonObject json = JsonParser.parseString(cacheData).getAsJsonObject();
                    
                    // v4
                    if (!json.has("_saveVersion")) {
                        logger.info("Migrating auth data from v4 to v5...");
                        try {
                            json = MinecraftAuth4To5Migrator.migrateBedrockSave(json);
                        } catch (Throwable e) {
                            logger.error("Failed to migrate auth data", e);
                            json = null; // Force re-login
                        }
                    }

                    if (json != null) {
                        authManager = BedrockAuthManager.fromJson(httpClient, Constants.BEDROCK_CODEC.getMinecraftVersion(), json);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load cache.json", e);
            }
        }

        try {
            // Login if not already loaded
            if (authManager == null) {
                // Explicitly define the callback to assist type inference for the generic T
                Consumer<MsaDeviceCode> deviceCodeCallback = msaDeviceCode -> {
                    logger.info("To sign in, use a web browser to open the page " + msaDeviceCode.getVerificationUri() + " and enter the code " + msaDeviceCode.getUserCode() + " to authenticate.");
                    notificationManager.sendSessionExpiredNotification(msaDeviceCode.getVerificationUri(), msaDeviceCode.getUserCode());
                };

                authManager = BedrockAuthManager.create(httpClient, Constants.BEDROCK_CODEC.getMinecraftVersion())
                        .login(DeviceCodeMsaAuthService::new, deviceCodeCallback);
            }

            // Ensure tokens are fresh
            refreshTokens();

            // Set up listener for saving
            // Explicitly cast to BasicChangeListener to resolve ambiguity with Runnable
            authManager.getChangeListeners().add((BasicChangeListener) this::saveToCache);
            saveToCache();

            // Setup device token refresh callback
            if (onDeviceTokenRefreshCallback != null) {
                authManager.getXblDeviceToken().getChangeListeners().add((BasicChangeListener) onDeviceTokenRefreshCallback::run);
            }

        } catch (Exception e) {
            // Dont log age verification errors as they are handled elsewhere
            if (e instanceof AgeVerificationException) {
                return;
            }

            logger.error("Failed to get/refresh auth token", e);
        }
    }

    private void refreshTokens() throws IOException, AgeVerificationException {
        try {
            // Requesting up-to-date tokens will automatically refresh them if expired
            authManager.getXboxLiveXstsToken().getUpToDate();
            authManager.getPlayFabToken().getUpToDate();
            updateProfileInfo();
        } catch (InformativeHttpRequestException e) {
            if (e.getMessage().contains("agecheck")) {
                throw new AgeVerificationException("Authentication failed due to age verification requirement", e);
            } else {
                throw e; // Rethrow if it's a different error
            }
        }
    }

    private void updateProfileInfo() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();

        try {
            XblUsersMeProfileRequest.Response response = httpClient.executeAndHandle(new XblUsersMeProfileRequest(authManager.getXboxLiveXstsToken().getUpToDate()));
            XblUsersMeProfileRequest.Response.ProfileUser profileUser = response.profileUsers().get(0);
            gamertag = profileUser.settings().get("Gamertag");
            xuid = profileUser.id();
        } catch (IOException e) {
            logger.error("Failed to get Xbox profile info", e);
        }
    }

    private void saveToCache() {
        try {
            storageManager.cache(BedrockAuthManager.toJson(authManager).toString());
        } catch (Exception e) {
            logger.error("Failed to save auth cache", e);
        }
    }

    /**
     * Get the authenticated BedrockAuthManager.
     * Initializes the manager if it hasn't been already.
     *
     * @return The BedrockAuthManager
     */
    public BedrockAuthManager getManager() {
        if (authManager == null) {
            initialise();
        }

        try {
            // Ensure we have fresh tokens
            refreshTokens();
        } catch (IOException e) {
            logger.error("Failed to refresh tokens", e);
            // Try to re-initialize (force login if refresh failed fatally)
            initialise();
        }
        return authManager;
    }

    public String getPlayfabSessionTicket() {
        if (authManager == null) {
            initialise();
        }
        try {
            return authManager.getPlayFabToken().getUpToDate().getSessionTicket();
        } catch (IOException e) {
            logger.error("Failed to get PlayFab session ticket", e);
            return null;
        }
    }

    /**
     * Set a callback to be executed when the device token has been refreshed.
     *
     * @param onDeviceTokenRefreshCallback The callback to execute on device token refresh
     */
    public void setOnDeviceTokenRefreshCallback(Runnable onDeviceTokenRefreshCallback) {
        this.onDeviceTokenRefreshCallback = onDeviceTokenRefreshCallback;
        if (authManager != null) {
            authManager.getXblDeviceToken().getChangeListeners().add((BasicChangeListener) onDeviceTokenRefreshCallback::run);
        }
    }

    /**
     * Get the Gamertag of the current user
     *
     * @return The Gamertag of the current user
     */
    public String getGamertag() {
        return gamertag;
    }

    /**
     * Get the XUID of the current user
     *
     * @return The XUID of the current user
     */
    public String getXuid() {
        return xuid;
    }
}