package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.FollowerResponse;
import com.rtm516.mcxboxbroadcast.core.models.SISUAuthenticationResponse;
import com.rtm516.mcxboxbroadcast.core.models.XboxTokenInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Simple manager to authenticate and create sessions on Xbox
 */
public class SessionManager {
    private final LiveTokenManager liveTokenManager;
    private final XboxTokenManager xboxTokenManager;
    private final HttpClient httpClient;
    private final Logger logger;
    private final String cache;

    private RtaWebsocketClient rtaWebsocket;
    private ExpandedSessionInfo sessionInfo;

    private String lastSessionResponse;

    /**
     * Create an instance of SessionManager using default values
     */
    public SessionManager() {
        this("./cache");
    }

    /**
     * Create an instance of SessionManager using default values
     *
     * @param cache The directory to store the cached tokens in
     */
    public SessionManager(String cache) {
        this(cache, new GenericLoggerImpl());
    }

    /**
     * Create an instance of SessionManager
     *
     * @param cache The directory to store the cached tokens in
     * @param logger The logger to use for outputting messages
     */
    public SessionManager(String cache, Logger logger) {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        this.logger = logger;
        this.cache = cache;

        this.liveTokenManager = new LiveTokenManager(cache, httpClient, logger);
        this.xboxTokenManager = new XboxTokenManager(cache, httpClient, logger);

        File directory = new File(cache);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Get the MSA token for the cached user or start the authentication process
     *
     * @return The fetched MSA token
     */
    private String getMsaToken() {
        if (liveTokenManager.verifyTokens()) {
            return liveTokenManager.getAccessToken();
        } else {
            try {
                return liveTokenManager.authDeviceCode().get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Failed to get authentication token from device code", e);
                return "";
            }
        }
    }

    /**
     * Get the Xbox token information for the current user
     * If there is no current user then the auto process is started
     *
     * @return The information about the Xbox authentication token including the token itself
     */
    public XboxTokenInfo getXboxToken() {
        if (xboxTokenManager.verifyTokens()) {
            return xboxTokenManager.getCachedXstsToken();
        } else {
            String msaToken = getMsaToken();
            String deviceToken = xboxTokenManager.getDeviceToken();
            SISUAuthenticationResponse sisuAuthenticationResponse =  xboxTokenManager.getSISUToken(msaToken, deviceToken);
            if (sisuAuthenticationResponse == null) {
                logger.info("SISU authentication response is null, please login again");
                liveTokenManager.clearTokenCache();
                return getXboxToken();
            }
            return xboxTokenManager.getXSTSToken(sisuAuthenticationResponse);
        }
    }

    /**
     * Create a new session for the given session information
     *
     * @param sessionInfo The information to create the session with
     * @throws SessionCreationException If the session failed to create either because it already exists or some other reason
     * @throws SessionUpdateException If the session data couldn't be set due to some issue
     */
    public void createSession(SessionInfo sessionInfo) throws SessionCreationException, SessionUpdateException {
        if (this.sessionInfo != null) {
            throw new SessionCreationException("Session already created!");
        }

        // Set the internal session information based on the session info
        this.sessionInfo = new ExpandedSessionInfo("", "", sessionInfo);

        // Create the session
        createSession();
    }

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

        // Update the current session infos XUID
        this.sessionInfo.setXuid(tokenInfo.userXUID);

        // Create the RTA websocket connection
        setupWebsocket(token);

        // Wait and get the connection ID from the websocket
        String connectionId;
        try {
            connectionId = waitForConnectionId().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SessionCreationException("Unable to get connectionId for session: " + e.getMessage());
        }

        // Update the current session infos connection ID
        this.sessionInfo.setConnectionId(connectionId);

        // Push the session information to the session directory
        updateSession();

        // Create the session handle request
        CreateHandleRequest createHandleContent = new CreateHandleRequest(
            1,
            "activity",
            new CreateHandleRequest.SessionRef(
                Constants.SERVICE_CONFIG_ID,
                "MinecraftLobby",
                this.sessionInfo.getSessionId()
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
        } catch (IOException | InterruptedException e) {
            throw new SessionCreationException(e.getMessage());
        }

        // Check to make sure the handle was created
        if (createHandleResponse.statusCode() != 200 && createHandleResponse.statusCode() != 201) {
            logger.debug("Failed to create session handle '"  + createHandleResponse.body() + "' (" + createHandleResponse.statusCode() + ")");
            throw new SessionCreationException("Unable to create session handle, got status " + createHandleResponse.statusCode() + " trying to create");
        }
    }

    /**
     * Update the current session with new information
     *
     * @param sessionInfo The information to update the session with
     * @throws SessionUpdateException If the update failed
     */
    public void updateSession(SessionInfo sessionInfo) throws SessionUpdateException {
        this.sessionInfo.updateSessionInfo(sessionInfo);
        updateSession();
    }

    /**
     * Update the session information using the stored information
     *
     * @throws SessionUpdateException If the update fails
     */
    private void updateSession() throws SessionUpdateException {
        CreateSessionRequest createSessionContent = new CreateSessionRequest(this.sessionInfo);

        HttpRequest createSessionRequest;
        try {
            createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.CREATE_SESSION + this.sessionInfo.getSessionId()))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .PUT(HttpRequest.BodyPublishers.ofString(Constants.OBJECT_MAPPER.writeValueAsString(createSessionContent)))
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

        lastSessionResponse = createSessionResponse.body();

        if (createSessionResponse.statusCode() != 200 && createSessionResponse.statusCode() != 201) {
            logger.debug("Got session response: " + lastSessionResponse);
            throw new SessionUpdateException("Unable to update session information, got status " + createSessionResponse.statusCode() + " trying to update");
        }
    }

    /**
     * Check the connection to the websocket and if its closed re-open it and re-create the session
     * This should be called before any updates to the session otherwise they might fail
     */
    public void checkConnection() {
        if (!rtaWebsocket.isOpen()) {
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
     * Get a list of friends XUIDs
     *
     * @param includeFollowing  Include users that are following us and not full friends
     * @param includeFollowedBy Include users that we are following and not full friends
     * @return A list of {@link FollowerResponse.Person} of your friends
     * @throws XboxFriendsException If there was an error getting friends from Xbox Live
     */
    public List<FollowerResponse.Person> getXboxFriends(boolean includeFollowing, boolean includeFollowedBy) throws XboxFriendsException {
        List<FollowerResponse.Person> people = new ArrayList<>();

        // Create the request for getting the people following us and friends
        HttpRequest xboxFollowersRequest = HttpRequest.newBuilder()
            .uri(Constants.FOLLOWERS)
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "5")
            .header("accept-language", "en-GB")
            .GET()
            .build();

        String lastResponse = "";
        try {
            // Get the list of friends from the api
            lastResponse = httpClient.send(xboxFollowersRequest, HttpResponse.BodyHandlers.ofString()).body();
            FollowerResponse xboxFollowerResponse = Constants.OBJECT_MAPPER.readValue(lastResponse, FollowerResponse.class);

            // Parse through the returned list to make sure we are friends and
            // add them to the list to return
            for (FollowerResponse.Person person : xboxFollowerResponse.people) {
                // Make sure they are full friends
                if ((person.isFollowedByCaller && person.isFollowingCaller)
                    || (includeFollowing && person.isFollowingCaller)) {
                    people.add(person);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.debug("Follower request response: " + lastResponse);
            throw new XboxFriendsException(e.getMessage());
        }

        if (includeFollowedBy) {
            // Create the request for getting the people we are following and friends
            HttpRequest xboxSocialRequest = HttpRequest.newBuilder()
                .uri(Constants.SOCIAL)
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "5")
                .header("accept-language", "en-GB")
                .GET()
                .build();

            try {
                // Get the list of people we are following from the api
                FollowerResponse xboxSocialResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(xboxSocialRequest, HttpResponse.BodyHandlers.ofString()).body(), FollowerResponse.class);

                // Parse through the returned list to make sure we are following them and
                // add them to the list to return
                for (FollowerResponse.Person person : xboxSocialResponse.people) {
                    // Make sure we are following them
                    if (person.isFollowedByCaller) {
                        people.add(person);
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.debug("Social request response: " + lastResponse);
                throw new XboxFriendsException(e.getMessage());
            }
        }

        return people;
    }

    /**
     * @see #getXboxFriends(boolean, boolean) 
     */
    public List<FollowerResponse.Person> getXboxFriends() throws XboxFriendsException {
        return getXboxFriends(false, false);
    }

    /**
     * Add a friend from xbox live
     *
     * @param xuid The XUID of the friend to add
     * @return If the request was successful, this will be true even if the user is already your friend, false if something goes wrong
     */
    public boolean addXboxFriend(String xuid) {
        HttpRequest xboxFriendRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.PEOPLE.formatted(xuid)))
            .header("Authorization", getTokenHeader())
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

        try {
            HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Remove a friend from xbox live
     *
     * @param xuid The XUID of the friend to remove
     * @return If the request was successful, this will be true even if the user isn't your friend, false if something goes wrong
     */
    public boolean removeXboxFriend(String xuid) {
        HttpRequest xboxFriendRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.PEOPLE.formatted(xuid)))
            .header("Authorization", getTokenHeader())
            .DELETE()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Use the data in the cache to get the Xbox authentication header
     *
     * @return The formatted XBL3.0 authentication header
     */
    private String getTokenHeader() {
        return getXboxToken().tokenHeader();
    }

    /**
     * Wait for the RTA websocket to receive a connection ID
     *
     * @return The received connection ID
     */
    private Future<String> waitForConnectionId() {
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
    private void setupWebsocket(String token) {
        rtaWebsocket = new RtaWebsocketClient(token, logger);
        rtaWebsocket.connect();
    }

    public void stopSession() {
        rtaWebsocket.close();
        this.sessionInfo.setSessionId(null);
    }

    public void dumpSession() {
        logger.info("Dumping current and last session responses");
        try {
            FileWriter file = new FileWriter(this.cache + "/lastSessionResponse.json");
            file.write(lastSessionResponse);
            file.close();
        } catch (IOException e) {
            logger.error("Error dumping last session: " + e.getMessage());
        }

        HttpRequest createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.CREATE_SESSION + this.sessionInfo.getSessionId()))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .GET()
                .build();

        try {
            HttpResponse<String> createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());

            FileWriter file = new FileWriter(this.cache + "/currentSessionResponse.json");
            file.write(createSessionResponse.body());
            file.close();
        } catch (IOException | InterruptedException e) {
            logger.error("Error dumping current session: " + e.getMessage());
        }

        logger.info("Dumped session responses to 'lastSessionResponse.json' and 'currentSessionResponse.json'");
    }

    public void updatePresence() {
        HttpRequest updatePresenceRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.USER_PRESENCE.formatted(getXboxToken().userXUID)))
            .header("Content-Type", "application/json")
            .header("Authorization", getTokenHeader())
            .header("x-xbl-contract-version", "3")
            .POST(HttpRequest.BodyPublishers.ofString("{\"state\": \"active\"}"))
            .build();

        try {
            HttpResponse<Void> updatePresenceResponse = httpClient.send(updatePresenceRequest, HttpResponse.BodyHandlers.discarding());

            if (updatePresenceResponse.statusCode() != 200) {
                logger.error("Failed to update presence, got status " + updatePresenceResponse.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
