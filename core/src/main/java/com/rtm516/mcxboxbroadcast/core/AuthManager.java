package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.models.auth.XstsAuthData;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.msa.MsaCodeStep;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCodeMsaCode;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.msa.StepRefreshTokenMsaCode;
import net.raphimc.minecraftauth.step.xbl.StepXblDeviceToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AuthManager {
    private final StorageManager storageManager;
    private final Logger logger;

    private StepXblSisuAuthentication.XblSisuTokens xstsToken;
    private XboxTokenInfo xboxTokenInfo;

    /**
     * Create an instance of AuthManager
     *
     * @param storageManager The storage manager to use for storing data
     * @param logger The logger to use for outputting messages
     */
    public AuthManager(StorageManager storageManager, Logger logger) {
        this.storageManager = storageManager;
        this.logger = logger.prefixed("Auth");

        this.xstsToken = null;
    }

    private static StepXblSisuAuthentication sisuAuthentication(AbstractStep<?, MsaCodeStep.MsaCode> msaInput) {
        StepMsaToken initialAuth = new StepMsaToken(msaInput);
        StepInitialXblSession xblAuth = new StepInitialXblSession(initialAuth, new StepXblDeviceToken("Android"));
        return new StepXblSisuAuthentication(xblAuth, MicrosoftConstants.XBL_XSTS_RELYING_PARTY);
    }

    private static MsaCodeStep.ApplicationDetails appDetails() {
        return new MsaCodeStep.ApplicationDetails(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH, null, OAuthEnvironment.LIVE.getNativeClientUrl(), OAuthEnvironment.LIVE);
    }

    public static XstsAuthData fromCredentials(String email, String password, ILogger logger) throws Exception {
        StepXblSisuAuthentication xstsAuth = sisuAuthentication(new StepCredentialsMsaCode(appDetails()));

        HttpClient httpClient = MinecraftAuth.createHttpClient();
        return new XstsAuthData(xstsAuth.getFromInput(logger, httpClient, new StepCredentialsMsaCode.MsaCredentials(email, password)), xstsAuth);
    }

    /**
     * Follow the auth flow to get the Xbox token and store it
     */
    private void initialise() {
        // Setup the authentication steps
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        MsaCodeStep.ApplicationDetails appDetails = appDetails();
        StepXblSisuAuthentication xstsAuth = sisuAuthentication(new StepMsaDeviceCodeMsaCode(new StepMsaDeviceCode(appDetails), 120 * 1000));

        // Check if we have an old live_token.json file and try to import the refresh token from it
        String liveTokenData = "";
        try {
            liveTokenData = storageManager.liveToken();
        } catch (IOException e) {
            // Ignore
        }
        if (!liveTokenData.isBlank()) {
            logger.info("Trying to convert from old live_token.json to new cache.json");
            try {
                JsonObject liveToken = JsonUtil.parseString(liveTokenData).getAsJsonObject();
                JsonObject tokenData = liveToken.getAsJsonObject("token");

                StepXblSisuAuthentication convertXstsAuth = sisuAuthentication(new StepRefreshTokenMsaCode(appDetails));

                xstsToken = convertXstsAuth.getFromInput(logger, httpClient, new StepRefreshTokenMsaCode.RefreshToken(tokenData.get("refresh_token").getAsString()));

                storageManager.xboxToken("");
            } catch (Exception e) {
                logger.error("Failed to convert old auth token, if this keeps happening please remove live_token.json and reauthenticate", e);
            }
        }

        // Load in cache.json if we haven't loaded one from the old auth token
        try {
            String cacheData = storageManager.cache();
            if (xstsToken == null && !cacheData.isBlank()) xstsToken = xstsAuth.fromJson(JsonUtil.parseString(cacheData).getAsJsonObject());
        } catch (IOException e) {
            logger.error("Failed to load cache.json", e);
        }

        try {
            // Get the XSTS token or refresh it if it's expired
            if (xstsToken == null) {
                xstsToken = xstsAuth.getFromInput(logger, httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                    logger.info("To sign in, use a web browser to open the page " + msaDeviceCode.getVerificationUri() + " and enter the code " + msaDeviceCode.getUserCode() + " to authenticate.");
                }));
            } else if (xstsToken.isExpired()) {
                xstsToken = xstsAuth.refresh(logger, httpClient, xstsToken);
            }

            // Save to cache.json
            storageManager.cache(Constants.GSON.toJson(xstsAuth.toJson(xstsToken)));

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
