package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.harvest.CandidateHarvester;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtcWebsocketClient extends WebSocketClient {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger;
    private final SessionInfo sessionInfo;
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<String, PeerSession> activeSessions;
    private final List<CandidateHarvester> candidateHarvesters;

    private ScheduledFuture<?> heartbeatFuture;

    /**
     * Create a new websocket and add the Authorization header
     *
     * @param authenticationToken      The token to use for authentication
     * @param sessionInfo The session info to use for the connection
     * @param logger The logger to use for outputting messages
     * @param scheduledExecutorService The executor service to use for scheduling tasks
     */
    public RtcWebsocketClient(String authenticationToken, ExpandedSessionInfo sessionInfo, Logger logger, ScheduledExecutorService scheduledExecutorService) {
        super(URI.create(Constants.RTC_WEBSOCKET_FORMAT.formatted(sessionInfo.getWebrtcNetworkId())));
        addHeader("Authorization", authenticationToken);
        // both seem random
        addHeader("Session-Id", UUID.randomUUID().toString());
        addHeader("Request-Id", UUID.randomUUID().toString());

        this.logger = logger;
        this.sessionInfo = sessionInfo;
        this.scheduledExecutorService = scheduledExecutorService;

        this.activeSessions = new HashMap<>();
        this.candidateHarvesters = new ArrayList<>();
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
        logger.debug("RTC Websocket received: " + message);
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
        logger.debug("RTC Websocket sent: " + text);
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
        PeerSession session = new PeerSession(this, candidateHarvesters);
        activeSessions.put(sessionId, session);
        session.receiveOffer(from, sessionId, message);
    }

    private void handleCandidateAdd(String sessionId, String message) {
        // Check the session exists, sometimes we get candidates after the session has been disconnected
        PeerSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.addCandidate(message);
        }
    }

    public void handleDisconnect(String sessionId) {
        logger.debug("Disconnecting session: " + sessionId);
        activeSessions.remove(sessionId);
    }

    private void initialize(JsonObject message) {
        // In the event we are sent another set of auth servers, clear the current list
        candidateHarvesters.clear();

        JsonArray turnAuthServers = message.getAsJsonArray("TurnAuthServers");
        for (JsonElement authServerElement : turnAuthServers) {
            JsonObject authServer = authServerElement.getAsJsonObject();
            String username = authServer.get("Username").getAsString();
            String password = authServer.get("Password").getAsString();
            authServer.getAsJsonArray("Urls").forEach(url -> {
                String[] parts = url.getAsString().split(":");
                String type = parts[0];
                String host = parts[1];
                int port = Integer.parseInt(parts[2]);

                if ("stun".equals(type)) {
                    candidateHarvesters.add(new StunCandidateHarvester(new TransportAddress(host, port, Transport.UDP)));
                } else if ("turn".equals(type)) {
                    candidateHarvesters.add(new TurnCandidateHarvester(
                            new TransportAddress(host, port, Transport.UDP),
                            new LongTermCredential(username, password)
                    ));
                } else {
                    throw new IllegalStateException("Unexpected value: " + type);
                }
            });
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        heartbeatFuture.cancel(true);

        logger.info("RTC Websocket disconnected: " + reason + " (" + code + ")");
    }

    @Override
    public void onError(Exception ex) {
        logger.info("RTC Websocket error: " + ex.getMessage());
    }

    public SessionInfo sessionInfo() {
        return sessionInfo;
    }

    public Logger logger() {
        return logger;
    }
}