package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsFromMessage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.harvest.CandidateHarvester;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.math.BigInteger;
import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

public class TesterRtcWebsocketClient extends WebSocketClient {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger;
    private final ScheduledExecutorService scheduledExecutorService;
    private final List<CandidateHarvester> candidateHarvesters;
    private final BigInteger netherNetId;

    private TesterSession activeSession;

    /**
     * Create a new websocket and add the Authorization header
     *
     * @param authenticationToken      The token to use for authentication
     * @param sessionInfo              The session info to use for the connection
     * @param logger                   The logger to use for outputting messages
     * @param scheduledExecutorService The executor service to use for scheduling tasks
     */
    public TesterRtcWebsocketClient(String authenticationToken, ExpandedSessionInfo sessionInfo, Logger logger, ScheduledExecutorService scheduledExecutorService, BigInteger netherNetId) {
        super(URI.create(Constants.RTC_WEBSOCKET_FORMAT.formatted(sessionInfo.getNetherNetId())));
        addHeader("Authorization", authenticationToken);
        // both seem random
        addHeader("Session-Id", UUID.randomUUID().toString());
        addHeader("Request-Id", UUID.randomUUID().toString());

        this.logger = logger;
        this.scheduledExecutorService = scheduledExecutorService;

        this.candidateHarvesters = new ArrayList<>();

        this.netherNetId = netherNetId;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
//        super.onOpen(serverHandshake);

        activeSession = new TesterSession(this, netherNetId, candidateHarvesters);
        try {
            activeSession.sendOffer();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

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

        if ("CONNECTRESPONSE".equals(type)) {
            handleConnectResponse(from, sessionId, content);
        } else if ("CANDIDATEADD".equals(type)) {
            logger.debug("CANDIDATEADD received (" + sessionId + "): " + content);
            activeSession.addCandidate(content.substring("candidate:".length()));

        }
    }

    private void handleConnectResponse(BigInteger from, String sessionId, String message) {
        // Send candiates
        activeSession.parseResponse(message);
        activeSession.sendCandidates(sessionId);
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
        logger.info("RTC Websocket disconnected: " + reason + " (" + code + ")");
    }

    @Override
    public void onError(Exception ex) {
        logger.info("RTC Websocket error: " + ex.getMessage());
    }

    public Logger logger() {
        return logger;
    }
}
