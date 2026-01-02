package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.models.auth.PlayfabLoginBody;
import com.rtm516.mcxboxbroadcast.core.models.auth.SisuAuthorizeBody;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;
import com.rtm516.mcxboxbroadcast.core.models.auth.XstsAuthData;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import java.io.IOException;

import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.HttpContentRequest;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.PlayFabResponseHandler;
import net.raphimc.minecraftauth.responsehandler.XblResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.msa.MsaCodeStep;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.xbl.StepXblDeviceToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken.XblXstsToken;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonContent;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import net.raphimc.minecraftauth.util.logging.ILogger;

public class AuthManager {
    private final NotificationManager notificationManager;
    private final StorageManager storageManager;
    private final Logger logger;

    private StepXblSisuAuthentication.XblSisuTokens xboxToken;
    private XboxTokenInfo xboxTokenInfo;
    private String playfabSessionTicket;
    private Runnable onDeviceTokenRefreshCallback;


    /**
     * Create an instance of AuthManager
     *
     * @param notificationManager The notification manager to use for sending messages
     * @param storageManager The storage manager to use for storing data
     * @param logger The logger to use for outputting messages
     */
    public AuthManager(NotificationManager notificationManager, StorageManager storageManager, Logger logger) {
        this.notificationManager = notificationManager;
        this.storageManager = storageManager;
        this.logger = logger.prefixed("Auth");

        this.xboxToken = null;
    }

    /**
     * Get a xsts token from a given set of credentials
     *
     * @param email The email to use for authentication
     * @param password The password to use for authentication
     * @param logger The logger to use for outputting messages
     * @return The XSTS token data
     * @throws Exception If an error occurs while getting the token
     */
    public static XstsAuthData fromCredentials(String email, String password, ILogger logger) throws Exception {
        AbstractStep.ApplicationDetails appDetails = new MsaCodeStep.ApplicationDetails(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH, null, OAuthEnvironment.LIVE.getNativeClientUrl(), OAuthEnvironment.LIVE);
        StepMsaToken initialAuth = new StepMsaToken(new StepCredentialsMsaCode(appDetails));
        StepInitialXblSession xblAuth = new StepInitialXblSession(initialAuth, new StepXblDeviceToken("Android"));
        StepXblSisuAuthentication xstsAuth = new StepXblSisuAuthentication(xblAuth, MicrosoftConstants.XBL_XSTS_RELYING_PARTY);

        HttpClient httpClient = MinecraftAuth.createHttpClient();
        return new XstsAuthData(xstsAuth.getFromInput(logger, httpClient, new StepCredentialsMsaCode.MsaCredentials(email, password)), xstsAuth);
    }

    /**
     * Follow the auth flow to get the Xbox token and store it
     */
    private void initialise() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();

        // Try to load xboxToken from cache.json if is not already loaded
        if (xboxToken == null) {
            try {
                String cacheData = storageManager.cache();
                if (!cacheData.isBlank()) xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.fromJson(JsonUtil.parseString(cacheData).getAsJsonObject());
            } catch (Exception e) {
                logger.error("Failed to load cache.json", e);
            }
        }

        try {
            String oldDeviceTokenId = (xboxToken != null) ? xboxToken.getInitialXblSession().getXblDeviceToken().getDeviceId() : null;

            // Get the XSTS token or refresh it if it's expired
            if (xboxToken == null) {
                xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(logger, httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                    logger.info("To sign in, use a web browser to open the page " + msaDeviceCode.getVerificationUri() + " and enter the code " + msaDeviceCode.getUserCode() + " to authenticate.");
                    notificationManager.sendSessionExpiredNotification(msaDeviceCode.getVerificationUri(), msaDeviceCode.getUserCode());
                }));
            } else if (xboxToken.isExpired()) {
                xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.refresh(logger, httpClient, xboxToken);
            }

            // Save to cache.json
            storageManager.cache(Constants.GSON.toJson(MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.toJson(xboxToken)));

            // Construct and store the Xbox token info
            xboxTokenInfo = new XboxTokenInfo(xboxToken);

            playfabSessionTicket = fetchPlayfabSessionTicket(httpClient);

            // If the device token has changed, run the callback
            String newDeviceTokenId =  xboxToken.getInitialXblSession().getXblDeviceToken().getDeviceId();
            if (oldDeviceTokenId != null && newDeviceTokenId != null && !newDeviceTokenId.equals(oldDeviceTokenId)) {
                logger.debug("Device token has changed");
                if (onDeviceTokenRefreshCallback != null) {
                    onDeviceTokenRefreshCallback.run();
                }
            }

        } catch (Exception e) {
            logger.error("Failed to get/refresh auth token", e);
        }
    }

    private String fetchPlayfabSessionTicket(HttpClient httpClient) throws IOException {
        // TODO Use minecraftauth library using StepPlayFabToken
        StepInitialXblSession.InitialXblSession initialSession = xboxToken.getInitialXblSession();

        HttpContentRequest authorizeRequest = new PostRequest(StepXblSisuAuthentication.XBL_SISU_URL)
            .setContent(new JsonContent(SisuAuthorizeBody.create(initialSession, MicrosoftConstants.BEDROCK_PLAY_FAB_XSTS_RELYING_PARTY)));
        authorizeRequest.setHeader(CryptUtil.getSignatureHeader(authorizeRequest, initialSession.getXblDeviceToken().getPrivateKey()));
        JsonObject authorizeResponse = httpClient.execute(authorizeRequest, new XblResponseHandler());

        XblXstsToken tokens = XblXstsToken.fromMicrosoftJson(authorizeResponse.getAsJsonObject("AuthorizationToken"), null);

        HttpContentRequest playfabRequest = new PostRequest(Constants.PLAYFAB_LOGIN)
            .setContent(new JsonContent(PlayfabLoginBody.create(tokens.getServiceToken())));

        JsonObject playfabResponse = httpClient.execute(playfabRequest, new PlayFabResponseHandler());

        return playfabResponse.getAsJsonObject("data").get("SessionTicket").getAsString();
    }

    /**
     * Get the Xbox token info
     * If the token is expired or missing then refresh it
     *
     * @return The Xbox token info
     */
    public XboxTokenInfo getXboxToken() {
        if (xboxToken == null || xboxTokenInfo == null || xboxToken.isExpired()) {
            logger.debug("xboxToken Need Refresh. (xboxToken: " + (xboxToken != null) +
                    ", xboxTokenInfo: " + (xboxTokenInfo != null) +
                    ", xboxToken.isExpired: " + (xboxToken != null && xboxToken.isExpired()) + ")");
            initialise();
        }
        return xboxTokenInfo;
    }

    public String getPlayfabSessionTicket() {
        if (playfabSessionTicket == null) {
            logger.debug("Playfab Session Ticket Need Refresh. (playfabSessionTicket is null)");
            initialise();
        }
        return playfabSessionTicket;
    }

    public void updateGamertag(String gamertag) {
        try {
            JsonObject xboxTokenJson = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.toJson(xboxToken);
            xboxTokenJson.getAsJsonObject("xstsToken").getAsJsonObject("displayClaims").addProperty("gtg", gamertag);
            xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.fromJson(xboxTokenJson);

            storageManager.cache(Constants.GSON.toJson(MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.toJson(xboxToken)));
            xboxTokenInfo = new XboxTokenInfo(xboxToken);
        } catch (Exception e) {
            logger.error("Failed to update gamertag", e);
        }
    }

    /**
     * Set a callback to be executed when the device token has been refreshed.
     *
     * @param onDeviceTokenRefreshCallback The callback to execute on device token refresh
     */
    public void setOnDeviceTokenRefreshCallback(Runnable onDeviceTokenRefreshCallback) {
        this.onDeviceTokenRefreshCallback = onDeviceTokenRefreshCallback;
    }
}
