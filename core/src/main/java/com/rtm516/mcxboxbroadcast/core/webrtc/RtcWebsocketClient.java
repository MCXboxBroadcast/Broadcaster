package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsFromMessage;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import java.math.BigInteger;
import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dev.kastle.webrtc.PeerConnectionFactory;
import dev.kastle.webrtc.RTCConfiguration;
import dev.kastle.webrtc.RTCIceServer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtcWebsocketClient extends WebSocketClient {

    private final Logger logger;
    private final String requestId;
    private final String rtaConnectionId;
    private final SessionInfo sessionInfo;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<String, PeerSession> activeSessions;
    private final SessionManagerCore sessionManager;

    private ScheduledFuture<?> heartbeatFuture;
    private CompletableFuture<Void> onOpenFuture = new CompletableFuture<>();

    private PeerConnectionFactory factory = new PeerConnectionFactory();
    private List<RTCIceServer> iceServers = new ArrayList<>();

    /**
     * Create a new websocket and add the Authorization header
     *
     * @param authenticationToken      The token to use for authentication
     * @param sessionInfo The session info to use for the connection
     * @param logger The logger to use for outputting messages
     * @param scheduledExecutorService The executor service to use for scheduling tasks
     */
    public RtcWebsocketClient(String authenticationToken, ExpandedSessionInfo sessionInfo, Logger logger, ScheduledExecutorService scheduledExecutorService, SessionManagerCore sessionManager) {
        super(URI.create(Constants.RTC_WEBSOCKET_FORMAT.formatted(sessionInfo.getNetherNetId())));
        addHeader("Authorization", authenticationToken);
        addHeader("Session-Id", UUID.randomUUID().toString());

        this.requestId = UUID.randomUUID().toString();
        addHeader("Request-Id", requestId);

        this.rtaConnectionId = sessionInfo.getConnectionId();
        this.logger = logger;
        this.sessionInfo = sessionInfo;
        this.scheduledExecutorService = scheduledExecutorService;
        this.sessionManager = sessionManager;

        this.activeSessions = new HashMap<>();
    }

    public CompletableFuture<Void> onOpenFuture() {
        return onOpenFuture;
    }

    /**
     * When the web socket connects start the heartbeat to keep the connection alive
     *
     * @see WebSocketClient#onOpen(ServerHandshake)
     *
     * @param serverHandshake The handshake of the websocket instance
     */
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        // Set up the heartbeat, the official client sends a heartbeat every ~40 seconds
        heartbeatFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            send(Constants.GSON.toJson(new WsToMessage(0, null, null)));
        }, 40, 40, TimeUnit.SECONDS);
        onOpenFuture.complete(null);
    }

    /**
     * When we get a message parse its json and handle it
     *
     * @see WebSocketClient#onMessage(String)
     *
     * @param message The UTF-8 decoded message that was received.
     */
    @Override
    public void onMessage(String message) {
        logger.debug("[" + rtaConnectionId + "] RTC Websocket [" + requestId + "] received: " + message);
        WsFromMessage messageWrapper = Constants.GSON.fromJson(message, WsFromMessage.class);

        if (messageWrapper.Type() == 2) {
            initialize(messageWrapper.message());
            return;
        }
        if (messageWrapper.Type() == 1) {
            handleDataAction(new BigInteger(messageWrapper.From()), messageWrapper.Message());
        }
    }

    @Override
    public void send(String text) {
        super.send(text);
        logger.debug("[" + rtaConnectionId + "] RTC Websocket [" + requestId + "] sent: " + text);
    }

    private void handleDataAction(BigInteger from, String message) {
        int typeIndex = message.indexOf(' ');
        String type = message.substring(0, typeIndex);
        int sessionIdIndex = message.indexOf(' ', typeIndex + 1);
        String sessionId = message.substring(typeIndex + 1, sessionIdIndex);
        String content = message.substring(sessionIdIndex + 1);

        if ("CONNECTREQUEST".equals(type)) {
            handleConnectRequest(from, sessionId, content);
        } else if ("CANDIDATEADD".equals(type)) {
            handleCandidateAdd(sessionId, content);
        }
    }

    private void handleConnectRequest(BigInteger from, String sessionId, String message) {
        RTCConfiguration config = new RTCConfiguration();
        config.iceServers.addAll(iceServers);

        PeerSession session = new PeerSession(this, factory, config, sessionManager);
        activeSessions.put(sessionId, session);
        session.receiveOffer(from, sessionId, message); // TODO Make this part of the constructor?
    }

    private void handleCandidateAdd(String sessionId, String message) {
        // Check the session exists, sometimes we get candidates after the session has been disconnected
        PeerSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.addCandidate(message);
        }
    }

    public void handleDisconnect(String sessionId) {
        logger.debug("[" + rtaConnectionId + "] RTC Websocket [" + requestId + "] disconnecting session: " + sessionId);
        activeSessions.remove(sessionId);
    }

    private void initialize(JsonObject message) {
        // In the event we are sent another set of auth servers, clear the current server
        JsonArray turnAuthServers = message.getAsJsonArray("TurnAuthServers");
        for (JsonElement authServerElement : turnAuthServers) {
            JsonObject authServer = authServerElement.getAsJsonObject();
            String username = authServer.get("Username").getAsString();
            String password = authServer.get("Password").getAsString();

            RTCIceServer iceServer = new RTCIceServer();
            iceServer.username = username;
            iceServer.password = password;

            authServer.getAsJsonArray("Urls").forEach(url -> {
                iceServer.urls.add(url.getAsString());
            });

            iceServers.add(iceServer);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
        if (!onOpenFuture.isDone()) {
            onOpenFuture.completeExceptionally(new IllegalStateException("RTC Websocket [" + requestId + "] disconnected before onOpen was called"));
        }

        String reasonString = reason.isEmpty() && code == 1000 ? "Normal close" : reason;
        logger.info("[" + rtaConnectionId + "] RTC Websocket [" + requestId + "] disconnected: " + reasonString + " (" + code + ")");
    }

    @Override
    public void onError(Exception ex) {
        logger.error("[" + rtaConnectionId + "] RTC Websocket [" + requestId + "] error: " + ex.getMessage(), ex);
    }

    public SessionInfo sessionInfo() {
        return sessionInfo;
    }

    public Logger logger() {
        return logger;
    }

    public ScheduledExecutorService scheduledExecutorService() {
        return scheduledExecutorService;
    }
}
