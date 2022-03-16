package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.models.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.CreateHandleRequestSessionRef;
import com.rtm516.mcxboxbroadcast.core.models.CreateSessionRequest;
import com.rtm516.mcxboxbroadcast.core.models.XboxTokenInfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SessionManager {
    private final LiveTokenManager liveTokenManager;
    private final XboxTokenManager xboxTokenManager;
    private final HttpClient httpClient;

    private RtaWebsocketClient rtaWebsocket;
    private ExpandedSessionInfo sessionInfo;

    public SessionManager(String cache) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.liveTokenManager = new LiveTokenManager(cache, httpClient);
        this.xboxTokenManager = new XboxTokenManager(cache, httpClient);

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
                e.printStackTrace();
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

        this.sessionInfo = new ExpandedSessionInfo(connectionId, tokenInfo.userXUID(), sessionInfo);

        updateSession();

        CreateHandleRequest createHandleContent = new CreateHandleRequest(
                1,
                "activity",
                new CreateHandleRequestSessionRef(
                        Constants.SERVICE_CONFIG_ID,
                        "MinecraftLobby",
                        this.sessionInfo.getSessionId()
                )
        );

        HttpRequest createHandleRequest = HttpRequest.newBuilder()
                .uri(Constants.CREATE_HANDLE)
                .header("Content-Type", "application/json")
                .header("Authorization", token)
                .header("x-xbl-contract-version", "107")
                .POST(HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(createHandleContent)))
                .build();

        HttpResponse<String> createHandleResponse = null;
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

        HttpRequest createSessionRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.CREATE_SESSION + this.sessionInfo.getSessionId()))
                .header("Content-Type", "application/json")
                .header("Authorization", getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .PUT(HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(createSessionContent)))
                .build();

        HttpResponse<String> createSessionResponse;
        try {
            createSessionResponse = httpClient.send(createSessionRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new SessionUpdateException(e.getMessage());
        }

        if (createSessionResponse.statusCode() != 200 && createSessionResponse.statusCode() != 201) {
            throw new SessionUpdateException("Unable to update session, got status " + createSessionResponse.statusCode() + " trying to update");
        }
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
