package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsFromMessage;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCIceServer;
import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtcWebsocketClient extends WebSocketClient {
    private final Logger logger;

    private RTCConfiguration rtcConfig;
    private PeerConnectionFactory peerFactory;
    private Map<String, PeerSession> activeSessions = new HashMap<>();
    private PeerSession pendingSession;

    /**
     * Create a new websocket and add the Authorization header
     *
     * @param authenticationToken The token to use for authentication
     */
    public RtcWebsocketClient(String authenticationToken, ExpandedSessionInfo sessionInfo, Logger logger) {
        super(URI.create(Constants.RTC_WEBSOCKET_FORMAT.formatted(sessionInfo.getWebrtcNetworkId())));
        addHeader("Authorization", authenticationToken);
        // both seem random
        addHeader("Session-Id", UUID.randomUUID().toString());
        addHeader("Request-Id", UUID.randomUUID().toString());

        this.logger = logger;

        this.peerFactory = new PeerConnectionFactory();
    }

    /**
     * When the web socket connects send the request for the connection ID
     * 
     * @see WebSocketClient#onOpen(ServerHandshake)
     * 
     * @param serverHandshake The handshake of the websocket instance
     */
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    /**
     * When we get a message check if it's a connection ID message
     * and handle otherwise ignore it
     * 
     * @see WebSocketClient#onMessage(String) 
     * 
     * @param data The UTF-8 decoded message that was received.
     */
    @Override
    public void onMessage(String data) {
        logger.info(data);
        var messageWrapper = Constants.GSON.fromJson(data, WsFromMessage.class);

        if (messageWrapper.Type() == 2) {
            initialize(messageWrapper.message());
            return;
        }
        if (messageWrapper.Type() == 1) {
            handleDataAction(new BigInteger(messageWrapper.From()), messageWrapper.Message());
        }
    }

    private void handleDataAction(BigInteger from, String message) {
        var typeIndex = message.indexOf(' ');
        var type = message.substring(0, typeIndex);
        var sessionIdIndex = message.indexOf(' ', typeIndex + 1);
        var sessionId = message.substring(typeIndex + 1, sessionIdIndex);
        var content = message.substring(sessionIdIndex + 1);

        if ("CONNECTREQUEST".equals(type)) {
            handleConnectRequest(from, sessionId, content);
        } else if ("CANDIDATEADD".equals(type)) {
            handleCandidateAdd(sessionId, content);
        }
    }

    private void handleConnectRequest(BigInteger from, String sessionId, String message) {
        var session = pendingSession;
        pendingSession = null;

        activeSessions.put(sessionId, session);
        session.receiveOffer(from, sessionId, message);
    }

    private void handleCandidateAdd(String sessionId, String message) {
        activeSessions.get(sessionId).addCandidate(message);
    }

    private void initialize(JsonObject message) {
        var turnAuthServers = message.getAsJsonArray("TurnAuthServers");

        rtcConfig = new RTCConfiguration();
        for (JsonElement authServerElement : turnAuthServers) {
            var authServer = authServerElement.getAsJsonObject();
            var server = new RTCIceServer();
            server.username = authServer.get("Username").getAsString();
            server.password = authServer.get("Password").getAsString();
            authServer.getAsJsonArray("Urls").forEach(url -> server.urls.add(url.getAsString()));
            rtcConfig.iceServers.add(server);
        }

        pendingSession = new PeerSession(this, rtcConfig);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("RTCWebsocket disconnected: " + reason + " (" + code + ")");
    }

    @Override
    public void onError(Exception ex) {
        logger.info("RTCWebsocket error: " + ex.getMessage());
    }

    protected PeerConnectionFactory peerFactory() {
        return peerFactory;
    }
}
