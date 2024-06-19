package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.MsaCodeStep;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCodeMsaCode;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.xbl.StepXblDeviceToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.OAuthEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AuthManager {
    private final Path cache;
    private final Path oldLiveAuth;
    private final Path oldXboxAuth;
    private final Logger logger;

    private final HttpClient httpClient;
    private final MsaCodeStep.ApplicationDetails appDetails;
    private final StepMsaToken initialAuth;
    private final StepXblDeviceToken stepDeviceToken;
    private final StepXblSisuAuthentication xstsAuth;

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

        this.httpClient = MinecraftAuth.createHttpClient();

        // Setup the authentication steps
        this.appDetails = new MsaCodeStep.ApplicationDetails(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH, null, null, OAuthEnvironment.LIVE);
        this.initialAuth = new StepMsaToken(new StepMsaDeviceCodeMsaCode(new StepMsaDeviceCode(this.appDetails), 120 * 1000));
        this.stepDeviceToken = new StepXblDeviceToken("Android");

        StepInitialXblSession xblAuth = new StepInitialXblSession(this.initialAuth, new StepXblDeviceToken("Android"));

        this.xstsAuth = new StepXblSisuAuthentication(xblAuth, MicrosoftConstants.XBL_XSTS_RELYING_PARTY);

        this.xstsToken = null;
    }

    /**
     * Convert old auth token to new format if it exists
     */
    private void importLiveTokens() {
        if (Files.exists(oldLiveAuth)) {
            logger.info("Trying to convert from old live_token.json to new cache.json");
            try {
                JsonObject liveToken = JsonUtil.parseString(Files.readString(oldLiveAuth)).getAsJsonObject();
                JsonObject tokenData = liveToken.getAsJsonObject("token");

                JsonObject empty = new JsonObject();
                empty.addProperty("expireTimeMs", 0);
                empty.addProperty("token", "");
                empty.addProperty("titleId", "");
                empty.addProperty("userHash", "");

                JsonObject msaToken = new JsonObject();
                msaToken.addProperty("expireTimeMs", 0);//liveToken.get("obtainedOn").getAsLong() + tokenData.get("expires_in").getAsLong() * 1000);
                msaToken.addProperty("accessToken", tokenData.get("access_token").getAsString());
                msaToken.addProperty("refreshToken", tokenData.get("refresh_token").getAsString());

                JsonObject msaCodeTemp = new JsonObject();
                msaCodeTemp.addProperty("clientId", appDetails.getClientId());
                msaCodeTemp.addProperty("scope", appDetails.getScope());
                msaCodeTemp.addProperty("oAuthEnvironment", appDetails.getOAuthEnvironment().name());
                msaToken.add("msaCode", msaCodeTemp);

                JsonObject initialXblSession = new JsonObject();
                initialXblSession.add("msaToken", initialAuth.toJson(initialAuth.refresh(httpClient, initialAuth.fromJson(msaToken))));
                initialXblSession.add("xblDeviceToken", stepDeviceToken.toJson(stepDeviceToken.applyStep(httpClient, null)));

                JsonObject convertedLiveToken = new JsonObject();
                convertedLiveToken.add("initialXblSession", initialXblSession);
                convertedLiveToken.add("titleToken", empty);
                convertedLiveToken.add("userToken", empty);
                convertedLiveToken.add("xstsToken", empty);

                Files.writeString(cache, JsonUtil.GSON.toJson(convertedLiveToken));
                Files.delete(oldLiveAuth);
                if (Files.exists(oldXboxAuth)) Files.delete(oldXboxAuth);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert old auth token", e);
            }
        }
    }

    /**
     * Follow the auth flow to get the Xbox token and store it
     */
    private void initialise() {
        importLiveTokens();

        // Load in cache.json
        try {
            if (Files.exists(cache)) xstsToken = xstsAuth.fromJson(JsonUtil.parseString(Files.readString(cache)).getAsJsonObject());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load cache.json", e);
        }

        try {
            // Get the XSTS token or refresh it if it's expired
            if (xstsToken == null) {
                xstsToken = xstsAuth.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                    logger.info("To sign in, use a web browser to open the page " + msaDeviceCode.getVerificationUri() + " and enter the code " + msaDeviceCode.getUserCode() + " to authenticate.");
                }));
            } else if (xstsToken.isExpired()) {
                xstsToken = xstsAuth.refresh(httpClient, xstsToken);
            }

            // Save to cache.json
            Files.writeString(cache, JsonUtil.GSON.toJson(xstsAuth.toJson(xstsToken)));

            // Construct and store the Xbox token info
            xboxTokenInfo = new XboxTokenInfo(xstsToken.getDisplayClaims().get("xid"), xstsToken.getUserHash(), xstsToken.getDisplayClaims().get("gtg"), xstsToken.getToken(), String.valueOf(xstsToken.getExpireTimeMs()));
        } catch (Exception e) {
            throw new RuntimeException(e);
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
