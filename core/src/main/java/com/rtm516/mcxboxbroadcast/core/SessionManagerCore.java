package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.mizosoft.methanol.Methanol;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.SessionRef;
import com.rtm516.mcxboxbroadcast.core.models.session.SocialSummaryResponse;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public abstract class SessionManagerCore {
    private final AuthManager authManager;
    private final FriendManager friendManager;
    protected final HttpClient httpClient;
    protected final Logger logger;
    protected final Logger coreLogger;
    protected final String cache;

    protected RtaWebsocketClient rtaWebsocket;
    protected ExpandedSessionInfo sessionInfo;
    protected String lastSessionResponse;

    protected boolean initialized = false;

    /**
     * Create an instance of SessionManager
     *
     * @param cache The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public SessionManagerCore(String cache, Logger logger) {
        this.httpClient = Methanol.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .requestTimeout(Duration.ofMillis(5000))
            .build();

        this.logger = logger;
        this.coreLogger = logger.prefixed("");
        this.cache = cache;

        this.authManager = new AuthManager(cache, logger);

        this.friendManager = new FriendManager(httpClient, logger, this);

        File directory = new File(cache);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Get the Xbox LIVE friend manager for this session manager
     *
     * @return The friend manager
     */
    public FriendManager friendManager() {
        return friendManager;
    }

    /**
     * Get the scheduled thread pool for this session manager
     *
     * @return The scheduled thread pool
     */
    public abstract ScheduledExecutorService scheduledThread();

    /**
     * Get the session ID for this session manager
     *
     * @return The session ID
     */
    public abstract String getSessionId();

    /**
     * Get the logger for this session manager
     * @return The logger
     */
    public Logger logger() {
        return logger;
    }

    /**
     * Get the MSA token for the cached user or start the authentication process
     *
     * @return The fetched MSA token
     */
//    protected String getMsaToken() {
//        if (liveTokenManager.verifyTokens()) {
//            return liveTokenManager.getAccessToken();
//        } else {
//            try {
//                return liveTokenManager.authDeviceCode().get();
//            } catch (InterruptedException | ExecutionException e) {
//                logger.error("Failed to get authentication token from device code: " + e.getMessage());
//                logger.debug(Utils.getStackTrace(e));
//                return "";
//            }
//        }
//    }

    /**
     * Get the Xbox token information for the current user
     * If there is no current user then the auto process is started
     *
     * @return The information about the Xbox authentication token including the token itself
     */
    protected XboxTokenInfo getXboxToken() {
        return authManager.getXboxToken();
//        return new XboxTokenInfo(
//            "2535469836726755",
//            "6965302177470921877",
//            "CrimpyLace85127",
//            "eyJlbmMiOiJBMTI4Q0JDK0hTMjU2IiwiYWxnIjoiUlNBLU9BRVAiLCJjdHkiOiJKV1QiLCJ6aXAiOiJERUYiLCJ4NXQiOiIxZlVBejExYmtpWklFaE5KSVZnSDFTdTVzX2cifQ.ID9pO8aekb2kb2Kp_6vkoWNYz7PuiAIVVy5lYtb_0AjlEFOeh-bSmPdrnOtMMSV1OaJqvcXiIJi7vCieIhK2-5Q3ZsXsEsmUKwEJPZ1KFCHjGDaW9HNE6BzXqDVKgXxdFiEpmVFvLQaoQXTjpHgSYhjEmrRp4rIcwW_A1fI9YCw.bq83770cQwwfFCWsTw02rA.UBBFcCWaKyAy2JbO2Ao7ZvPv0n1LIIQpnDkRNm0iyJ8HequClOJMf550VIrz1aQ2pKjelUkws4B8o3CWotyoAXaE4piKtB1JUuV4IK5AaBErLhRiRfI4tEw9gg1EHAXks6U1iH6llaFoj89eoB1LLUOMx70UG_A8nGTIosUdgO2dXoEnxIJuM9Zi8IXw315dZ1doJypULRXuBPdlKYVtEQLiW_veCDe_Mg4ROSb31kPAg3Th0POOnHLGxSZkdlzbYIsoX7zwrsnC6ZDQMeNYhTfnZle7wVOHLXpuv-9SCeZiqVgV7dq72mAD9vSrYKBY2_ux6kx2m25d6-1tuBw6Kh9DHNBkfgqzwpTTc6FVNNHOX-bmRHEzCZu88YV1xdt3zg6U8oQmzT3Mcg62QzfPrHX_C6bMshlJlTSMf59nPUDcdbHVgGWtw4dSeTnRuTNUnRBaFpEZDPW1NwhN2dNf-KsfrAqZIgcZMI_jmtfuCDGW8CxlVOK-usPn-ulP6TWMJaxRD39MZvCHrqmBdvfsWN6XvUfI06-jSYvHWlLy7o6F2ia1FOfDSbB5_GBL5XiyPD4QDEyVZ4WADlYVZDOswzuGjHzVFiXBoNqK5-9PtbLosgUqlpdRU_1FBdiQcVa3gbOIx8b4GeLbCDrYNbQWY13wYM5Qj_niCq8Jn0zFmI5efC2FCPDhPjBr2Gx3ekrq0ghTJ6_q9_Wmdr-4UIu2YPSk9UPbMarem3v-Nedl_Fg93DgX4Xzt1MUsOBHK-L0bYqxSBBxu0WGxF6-yMkCxIIJcXy5tr1jiqg12OLTI6bJ9sk97HY9IV6Yn9ycszNLNJPKTGFbAeBkpROp7EHAKkr2YfDEYJ-N_Ha06nlMu810aOl2XqDDAKJJrHt-nAK48fpcJevvy3Y7ZvTyqn1gqMQK9_iGzH1gP8qhePhA9FlL5C6fE13QYcGRu76b671ivZ-VpOmOAWY01z2kCMlJCEa3M7SdCJy14zRZx8kXvJEVFwOVyJ-Lc2OE4xnxGtmTqHTZLaASeoeiFzspkrnKqTd9soAKoUo_SQsiJ-QBQ8bRtranB-Wxnk56Lhq-BOUu7nLg2NsKGuSCYZ2v46Dp45EX3Ug1R9DOThlLz0uMiozq3FoPlXu79-VtidB1FBnfQxeSy8YgA-cfQ2XboV7DjA-HQu1JjTdB7faXStkTbIQ9XPhy4HmMsfBT5ka1KnjF2IWX3KKHqwly-c_ZSjULy6HIsvtNVQSHmHH7ipse2VJwzfgHCgy_btM2fQjNZiAXCinQy5Nnw0BYUJMRkZGtti7oQXOCBxE1dRp4nlURts1tJc6Z39Zz7NGYniazEmAdFmGJZHubnrLedjAQyv2g9xRpqgC4jB9qVS4ZySBmoYmkP1xhUP5BEBBq3NQK5dvpX74XAmu6pptZH9lucv2Nz-6Pfe_5yZYnus4_sQT4RrynZXiVeoNw_ZhkiclOBq6YMEI2nFqUh51yuitXXo3ygQbGGHLsfNzoDMrJGpGv9BoY8wHdVJ1jHRFqGteBX6Pevb2YrPjPiPdrzXkYovmGrrsGcAztOJM8BJnJb19AJaYeHBgGOl5WhtcoGSTGJ3veQO9ywzk8brbxFkDu5h2G8ZNmZ_vQksha8CBCVwakt6XL5W6NYNFt6J658BYzSyqtLis9VtM_gXV3pSOocy-fP00oGEFknRNlFcH7r7uOL8t62XHAIwjEnRDtkYkGFZyDCmTmK8rHsAVrRkL68iyyRaRHE5h2lTMVQqnTzzdLDkpqOO35k_jHHmd09i4BMeYyV7DGIoXyIC0gB3h8QFTJyvAytVOzUKofw2MvYB-IiITqRLZNOvuW35ibpA0zq-THA7DAV2ltVXBB2jp8QL9Q_BUsVNx75o13kBoZ4yM9GyHEhtXHEV9jYxJnRccgwR4Gw38d3LE34PVZnBWzxwrZRtA1ZSJJZC5E8v7Rd4rYyV1OaUW5sb7aKMsz0xSsVplScG9ztFr94-DgxQ353MG61IlkaNoolzHRW_NbDuXoHk0c.jeCstwzB47UNdvANw6Byl6KzddGzd-cpkvLHzYzapnc",
//            "1718858479816");



//        if (xboxTokenManager.verifyTokens()) {
//            return xboxTokenManager.getCachedXstsToken();
//        } else {
//            String msaToken = getMsaToken();
//            if (!msaToken.isEmpty()) {
//                String deviceToken = xboxTokenManager.getDeviceToken();
//                SISUAuthenticationResponse sisuAuthenticationResponse =  xboxTokenManager.getSISUToken(msaToken, deviceToken);
//                if (sisuAuthenticationResponse != null) {
//                    return xboxTokenManager.getXSTSToken(sisuAuthenticationResponse);
//                } else {
//                    logger.info("SISU authentication response is null, please login again");
//                }
//            } else {
//                logger.info("MSA authentication response is null, please login again");
//            }
//        }
//
//        liveTokenManager.clearTokenCache();
//        return getXboxToken();
    }

    /**
     * Initialize the session manager with the given session information
     *
     * @throws SessionCreationException If the session failed to create either because it already exists or some other reason
     * @throws SessionUpdateException   If the session data couldn't be set due to some issue
     */
    public void init() throws SessionCreationException, SessionUpdateException {
        if (this.initialized) {
            throw new SessionCreationException("Already initialized!");
        }

        logger.info("Starting SessionManager...");

        // Make sure we are logged in
        XboxTokenInfo tokenInfo = getXboxToken();
        logger.info("Successfully authenticated as " + tokenInfo.gamertag() + " (" + tokenInfo.userXUID() + ")");

        if (handleFriendship()) {
            logger.info("Waiting for friendship to be processed...");
            try {
                Thread.sleep(5000); // TODO Do a real callback not just wait
            } catch (InterruptedException e) {
                logger.error("Failed to wait for friendship to be processed", e);
            }
        }

        logger.info("Creating Xbox LIVE session...");

        // Create the session
        createSession();

        // Update the presence
        updatePresence();

        // Let the user know we are done
        logger.info("Creation of Xbox LIVE session was successful!");

        initialized = true;
    }

    /**
     * Handle the friendship of the current user to the main session if needed
     *
     * @return True if the friendship is being handled, false otherwise
     */
    protected abstract boolean handleFriendship();

    /**
     * Setup a new session and its prerequisites
     *
     * @throws SessionCreationException If the initial creation of the session fails
     * @throws SessionUpdateException If the updating of the session information fails
     */
    private void createSession() throws SessionCreationException, SessionUpdateException {
        // Get the token for authentication
        XboxTokenInfo tokenInfo = getXboxToken();
        String token = tokenInfo.tokenHeader();

        // We only need a websocket for the primary session manager
        if (this.sessionInfo != null) {
            // Update the current session XUID
            this.sessionInfo.setXuid(tokenInfo.userXUID());

            // Create the RTA websocket connection
            setupWebsocket(token);

            try {
                // Wait and get the connection ID from the websocket
                String connectionId = waitForConnectionId().get();

                // Update the current session connection ID
                this.sessionInfo.setConnectionId(connectionId);
            } catch (InterruptedException | ExecutionException e) {
                throw new SessionCreationException("Unable to get connectionId for session: " + e.getMessage());
            }
        }

        // Push the session information to the session directory
        updateSession();

        // Create the session handle request
        CreateHandleRequest createHandleContent = new CreateHandleRequest(
            1,
            "activity",
            new SessionRef(
                Constants.SERVICE_CONFIG_ID,
                "MinecraftLobby",
                getSessionId()
            )
        );

        // Make the request to create the session handle
        HttpRequest createHandleRequest;
        try {
            createHandleRequest = HttpRequest.newBuilder()
                .uri(Constants.CREATE_HANDLE)
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .header("x-xbl-contract-version", "107")
                .POST(HttpRequest.BodyPublishers.ofString(Constants.OBJECT_MAPPER.writeValueAsString(createHandleContent)))
                .build();
        } catch (JsonProcessingException e) {
            throw new SessionCreationException("Unable to create session handle, error parsing json: " + e.getMessage());
        }

        // Read the handle response
        HttpResponse<String> createHandleResponse;
        try {
            createHandleResponse = httpClient.send(createHandleRequest, HttpResponse.BodyHandlers.ofString());
            if (this.sessionInfo != null) {
                CreateHandleResponse parsedResponse = Constants.OBJECT_MAPPER.readValue(createHandleResponse.body(), CreateHandleResponse.class);
                sessionInfo.setHandleId(parsedResponse.id());
            }
        } catch (IOException | InterruptedException e) {
            throw new SessionCreationException(e.getMessage());
        }

        lastSessionResponse = createHandleResponse.body();

        // Check to make sure the handle was created
        if (createHandleResponse.statusCode() != 200 && createHandleResponse.statusCode() != 201) {
            logger.debug("Failed to create session handle '"  + lastSessionResponse + "' (" + createHandleResponse.statusCode() + ")");
            throw new SessionCreationException("Unable to create session handle, got status " + createHandleResponse.statusCode() + " trying to create");
        }
    }

    /**
     * Update the session information using the stored information
     *
     * @throws SessionUpdateException If the update fails
     */
    protected abstract void updateSession() throws SessionUpdateException;

    /**
     * The internal method for making the web request to update the session
     *
     * @param url The url to send the PUT request containing the session data
     * @param data The data to update the session with
     * @return The response body from the request
     * @throws SessionUpdateException If the update fails
     */
    protected String updateSessionInternal(String url, Object data) throws SessionUpdateException {
        HttpRequest createSessionRequest;
        try {
            createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .PUT(HttpRequest.BodyPublishers.ofString(Constants.OBJECT_MAPPER.writeValueAsString(data)))
                .build();
        } catch (JsonProcessingException e) {
            throw new SessionUpdateException("Unable to update session information, error parsing json: " + e.getMessage());
        }

        HttpResponse<String> createSessionResponse;
        try {
            createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new SessionUpdateException(e.getMessage());
        }

        if (createSessionResponse.statusCode() != 200 && createSessionResponse.statusCode() != 201) {
            logger.debug("Got update session response: " + createSessionResponse.body());
            throw new SessionUpdateException("Unable to update session information, got status " + createSessionResponse.statusCode() + " trying to update");
        }

        return createSessionResponse.body();
    }

    /**
     * Check the connection to the websocket and if its closed re-open it and re-create the session
     * This should be called before any updates to the session otherwise they might fail
     */
    protected void checkConnection() {
        if (this.rtaWebsocket != null && !rtaWebsocket.isOpen()) {
            try {
                logger.info("Connection to websocket lost, re-creating session...");
                createSession();
                logger.info("Re-connected!");
            } catch (SessionCreationException | SessionUpdateException e) {
                logger.error("Session is dead and hit exception trying to re-create it", e);
            }
        }
    }

    /**
     * Use the data in the cache to get the Xbox authentication header
     *
     * @return The formatted XBL3.0 authentication header
     */
    public String getTokenHeader() {
        return getXboxToken().tokenHeader();
    }

    /**
     * Wait for the RTA websocket to receive a connection ID
     *
     * @return The received connection ID
     */
    protected Future<String> waitForConnectionId() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            while (rtaWebsocket.getConnectionId() == null) {
                Thread.sleep(100);
            }
            completableFuture.complete(rtaWebsocket.getConnectionId());

            return null;
        });

        return completableFuture;
    }

    /**
     * Setup the RTA websocket connection
     *
     * @param token The authentication token to use
     */
    protected void setupWebsocket(String token) {
        rtaWebsocket = new RtaWebsocketClient(token, logger);
        rtaWebsocket.connect();
    }

    /**
     * Stop the current session and close the websocket
     */
    public void shutdown() {
        if (rtaWebsocket != null) {
            rtaWebsocket.close();
        }
        this.initialized = false;
    }

    /**
     * Update the presence of the current user on Xbox LIVE
     */
    protected void updatePresence() {
        HttpRequest updatePresenceRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.USER_PRESENCE.formatted(getXboxToken().userXUID())))
            .header("Content-Type", "application/json")
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "3")
            .POST(HttpRequest.BodyPublishers.ofString("{\"state\": \"active\"}"))
            .build();

        int heartbeatAfter = 300;
        try {
            HttpResponse<Void> updatePresenceResponse = httpClient.send(updatePresenceRequest, HttpResponse.BodyHandlers.discarding());

            if (updatePresenceResponse.statusCode() != 200) {
                logger.error("Failed to update presence, got status " + updatePresenceResponse.statusCode());
            } else {
                // Read X-Heartbeat-After header to get the next time we should update presence
                try {
                    heartbeatAfter = Integer.parseInt(updatePresenceResponse.headers().firstValue("X-Heartbeat-After").orElse("300"));
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse heartbeat after header, using default of 300");
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to update presence", e);
        }

        // Schedule the next presence update
        logger.debug("Presence update successful, scheduling presence update in " + heartbeatAfter + " seconds");
        scheduledThread().schedule(this::updatePresence, heartbeatAfter, TimeUnit.SECONDS);
    }

    /**
     * Get the current follower count for the current user
     * @return The current follower count
     */
    public SocialSummaryResponse socialSummary() {
        HttpRequest socialSummaryRequest = HttpRequest.newBuilder()
            .uri(Constants.SOCIAL_SUMMARY)
            .header("Authorization", getTokenHeader())
            .GET()
            .build();


        try {
            return Constants.OBJECT_MAPPER.readValue(httpClient.send(socialSummaryRequest, HttpResponse.BodyHandlers.ofString()).body(), SocialSummaryResponse.class);
        } catch (IOException | InterruptedException e) {
            logger.error("Unable to get current friend count", e);
        }

        return new SocialSummaryResponse(-1, -1, false, false, false, false, "", -1, -1, "");
    }
}
