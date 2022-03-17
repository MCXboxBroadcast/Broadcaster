package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.GenericAuthenticationResponse;
import com.rtm516.mcxboxbroadcast.core.models.PeopleResponse;
import com.rtm516.mcxboxbroadcast.core.models.XboxTokenInfo;

import java.io.File;
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

public class SessionManager {
    private final LiveTokenManager liveTokenManager;
    private final XboxTokenManager xboxTokenManager;
    private final HttpClient httpClient;
    private final Logger logger;

    private RtaWebsocketClient rtaWebsocket;
    private ExpandedSessionInfo sessionInfo;

    public SessionManager(String cache, Logger logger) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.logger = logger;

        this.liveTokenManager = new LiveTokenManager(cache, httpClient, logger);
        this.xboxTokenManager = new XboxTokenManager(cache, httpClient, logger);

        File directory = new File(cache);
        if (! directory.exists()) {
            directory.mkdirs();
        }
    }

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

    public XboxTokenInfo getXboxToken() {
        if (xboxTokenManager.verifyTokens()) {
            return xboxTokenManager.getCachedXstsToken();
        } else {
            String msaToken = getMsaToken();
            String userToken = xboxTokenManager.getUserToken(msaToken);
            String deviceToken = xboxTokenManager.getDeviceToken();
            String titleToken = xboxTokenManager.getTitleToken(msaToken, deviceToken);
            XboxTokenInfo xsts = xboxTokenManager.getXSTSToken(userToken, deviceToken, titleToken);
            return xsts;
        }
    }

    public void createSession(SessionInfo sessionInfo) throws SessionCreationException, SessionUpdateException {
        if (this.sessionInfo != null) {
            throw new SessionCreationException("Session already created!");
        }

        XboxTokenInfo tokenInfo = getXboxToken();
        String token = tokenInfo.tokenHeader();

        setupWebsocket(token);

        String connectionId;
        try {
            connectionId = waitForConnectionId().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SessionCreationException("Unable to get connectionId for session: " + e.getMessage());
        }

        this.sessionInfo = new ExpandedSessionInfo(connectionId, tokenInfo.userXUID, sessionInfo);

        updateSession();

        CreateHandleRequest createHandleContent = new CreateHandleRequest(
                1,
                "activity",
                new CreateHandleRequest.SessionRef(
                        Constants.SERVICE_CONFIG_ID,
                        "MinecraftLobby",
                        this.sessionInfo.getSessionId()
                )
        );

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

        HttpResponse<String> createHandleResponse;
        try {
            createHandleResponse = httpClient.send(createHandleRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new SessionCreationException(e.getMessage());
        }

        if (createHandleResponse.statusCode() != 200 && createHandleResponse.statusCode() != 201) {
            throw new SessionCreationException("Unable to create session handle, got status " + createHandleResponse.statusCode() + " trying to create");
        }
    }

    public void updateSession(SessionInfo sessionInfo) throws SessionUpdateException {
        this.sessionInfo.updateSessionInfo(sessionInfo);
        updateSession();
    }

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

        if (createSessionResponse.statusCode() != 200 && createSessionResponse.statusCode() != 201) {
            throw new SessionUpdateException("Unable to update session information, got status " + createSessionResponse.statusCode() + " trying to update");
        }
    }

    /**
     * Get a list of friends xuids
     *
     * @param includeFollowing Include users that are following us and not full friends
     * @param includeFollowedBy Include users that we are following and not full friends
     * @return
     */
    public List<String> getXboxFriends(boolean includeFollowing, boolean includeFollowedBy) throws XboxFriendsException {
        List<String> xuids = new ArrayList<>();

        HttpRequest xboxPeopleRequest = HttpRequest.newBuilder()
            .uri(Constants.PEOPLE)
            .header("Authorization", getTokenHeader())
            .GET()
            .build();

        try {
            PeopleResponse xboxPeopleResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(xboxPeopleRequest, HttpResponse.BodyHandlers.ofString()).body(), PeopleResponse.class);

            for (PeopleResponse.Person person : xboxPeopleResponse.people) {
                if ((person.isFollowedByCaller && person.isFollowingCaller)
                    || (includeFollowing && person.isFollowingCaller)
                    || (includeFollowedBy && person.isFollowedByCaller)) {
                    xuids.add(person.xuid);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new XboxFriendsException(e.getMessage());
        }

        return xuids;
    }

    /**
     * @see #getXboxFriends(boolean, boolean)
     */
    public List<String> getXboxFriends() throws XboxFriendsException {
        return getXboxFriends(false, false);
    }

    private String getTokenHeader() {
        return getXboxToken().tokenHeader();
    }

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

    private void setupWebsocket(String token) {
        rtaWebsocket = new RtaWebsocketClient(token);
        rtaWebsocket.connect();
    }
}
