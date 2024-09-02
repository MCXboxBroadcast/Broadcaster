package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.models.auth.PlayfabLoginBody;
import com.rtm516.mcxboxbroadcast.core.models.auth.SisuAuthorizeBody;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;
import com.rtm516.mcxboxbroadcast.core.models.auth.XstsAuthData;
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
    private final SlackNotificationManager slackNotificationManager;
    private final StorageManager storageManager;
    private final Logger logger;

    private StepXblSisuAuthentication.XblSisuTokens xboxToken;
    private XboxTokenInfo xboxTokenInfo;
    private String playfabSessionTicket;

    /**
     * Create an instance of AuthManager
     *
     * @param slackNotificationManager The Slack notification manager to use for sending messages
     * @param storageManager The storage manager to use for storing data
     * @param logger The logger to use for outputting messages
     */
    public AuthManager(SlackNotificationManager slackNotificationManager, StorageManager storageManager, Logger logger) {
        this.slackNotificationManager = slackNotificationManager;
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

                xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(logger, httpClient, new StepMsaToken.RefreshToken(tokenData.get("refresh_token").getAsString()));

                storageManager.xboxToken("");
            } catch (Exception e) {
                logger.error("Failed to convert old auth token, if this keeps happening please remove live_token.json and reauthenticate", e);
            }
        }

        // Load in cache.json if we haven't loaded one from the old auth token
        try {
            String cacheData = storageManager.cache();
            if (xboxToken == null && !cacheData.isBlank()) xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.fromJson(JsonUtil.parseString(cacheData).getAsJsonObject());
        } catch (IOException e) {
            logger.error("Failed to load cache.json", e);
        }

        try {
            // Get the XSTS token or refresh it if it's expired
            if (xboxToken == null) {
                xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(logger, httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                    logger.info("To sign in, use a web browser to open the page " + msaDeviceCode.getVerificationUri() + " and enter the code " + msaDeviceCode.getUserCode() + " to authenticate.");
                    slackNotificationManager.sendSessionExpiredNotification(msaDeviceCode.getVerificationUri(), msaDeviceCode.getUserCode());
                }));
            } else if (xboxToken.isExpired()) {
                xboxToken = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.refresh(logger, httpClient, xboxToken);
            }

            // Save to cache.json
            storageManager.cache(Constants.GSON.toJson(MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.toJson(xboxToken)));

            // Construct and store the Xbox token info
            xboxTokenInfo = new XboxTokenInfo(xboxToken);

            playfabSessionTicket = fetchPlayfabSessionTicket(httpClient);
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
            initialise();
        }
        return xboxTokenInfo;
    }

    public String getPlayfabSessionTicket() {
        if (playfabSessionTicket == null) {
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
}
