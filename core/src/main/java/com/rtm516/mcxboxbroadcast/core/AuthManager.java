package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;
import net.raphimc.minecraftauth.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AuthManager {
    private final Path cache;
    private final Path oldLiveAuth;
    private final Path oldXboxAuth;
    private final Logger logger;

    private StepXblSisuAuthentication.XblSisuTokens xstsToken;
    private XboxTokenInfo xboxTokenInfo;

    /**
     * Create an instance of AuthManager
     *
     * @param cache The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public AuthManager(String cache, Logger logger) {
        this.cache = Paths.get(cache, "cache.json");
        this.oldLiveAuth = Paths.get(cache, "live_token.json");
        this.oldXboxAuth = Paths.get(cache, "xbox_token.json");
        this.logger = logger;

        // Replace the default logger with one we control
        MinecraftAuth.LOGGER = logger.prefixed("Auth");

        this.xstsToken = null;
    }
    /**
     * Follow the auth flow to get the Xbox token and store it
     */
    private void initialise() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        // Check if we have an old live_token.json file and try to import the refresh token from it
        if (Files.exists(oldLiveAuth)) {
            logger.info("Trying to convert from old live_token.json to new cache.json");
            try {
                JsonObject liveToken = JsonUtil.parseString(Files.readString(oldLiveAuth)).getAsJsonObject();
                JsonObject tokenData = liveToken.getAsJsonObject("token");

                xstsToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaToken.RefreshToken(tokenData.get("refresh_token").getAsString()));

                Files.delete(oldLiveAuth);
                if (Files.exists(oldXboxAuth)) Files.delete(oldXboxAuth);
            } catch (Exception e) {
                logger.error("Failed to convert old auth token, if this keeps happening please remove " + oldLiveAuth + " and reauthenticate", e);
            }
        }

        // Load in cache.json if we haven't loaded one from the old auth token
        try {
            if (xstsToken == null && Files.exists(cache)) xstsToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.fromJson(JsonUtil.parseString(Files.readString(cache)).getAsJsonObject());
        } catch (IOException e) {
            logger.error("Failed to load cache.json", e);
        }

        try {
            // Get the XSTS token or refresh it if it's expired
            if (xstsToken == null) {
                xstsToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                    logger.info("To sign in, use a web browser to open the page " + msaDeviceCode.getVerificationUri() + " and enter the code " + msaDeviceCode.getUserCode() + " to authenticate.");
                }));
            } else if (xstsToken.isExpired()) {
                xstsToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.refresh(httpClient, xstsToken);
            }

            // Save to cache.json
            Files.writeString(cache, JsonUtil.GSON.toJson(MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.toJson(xstsToken)));

            // Construct and store the Xbox token info
            xboxTokenInfo = new XboxTokenInfo(xstsToken.getDisplayClaims().get("xid"), xstsToken.getUserHash(), xstsToken.getDisplayClaims().get("gtg"), xstsToken.getToken(), String.valueOf(xstsToken.getExpireTimeMs()));
        } catch (Exception e) {
            logger.error("Failed to get/refresh auth token", e);
        }
    }

    /**
     * Get the Xbox token info
     * If the token is expired or missing then refresh it
     *
     * @return The Xbox token info
     */
    public XboxTokenInfo getXboxToken() {
        if (xstsToken == null || xboxTokenInfo == null || xstsToken.isExpired()) {
            initialise();
        }

        return xboxTokenInfo;
    }
}
