package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phono.srtplight.Log;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsFromMessage;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCConfiguration;
import io.jsonwebtoken.lang.Collections;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.sdp.Attribute;
import javax.sdp.MediaDescription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tls.DTLSClientProtocol;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;
import org.ice4j.ice.KeepAliveStrategy;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.RemoteCandidate;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.opentelecoms.javax.sdp.NistSdpFactory;
import pe.pi.sctp4j.sctp.small.ThreadedAssociation;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtcWebsocketClient extends WebSocketClient {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger;
    private final ScheduledExecutorService scheduledExecutorService;

    private RTCConfiguration rtcConfig;
    public PeerConnectionFactory peerFactory;
    private Agent agent;
    private Component component;
    private Map<String, PeerSession> activeSessions = new HashMap<>();
    private PeerSession pendingSession;
    private ScheduledFuture<?> heartbeatFuture;

    /**
     * Create a new websocket and add the Authorization header
     *
     * @param authenticationToken      The token to use for authentication
     * @param scheduledExecutorService
     */
    public RtcWebsocketClient(String authenticationToken, ExpandedSessionInfo sessionInfo, Logger logger, ScheduledExecutorService scheduledExecutorService) {
        super(URI.create(Constants.RTC_WEBSOCKET_FORMAT.formatted(sessionInfo.getWebrtcNetworkId())));
        addHeader("Authorization", authenticationToken);
        // both seem random
        addHeader("Session-Id", UUID.randomUUID().toString());
        addHeader("Request-Id", UUID.randomUUID().toString());

        this.logger = logger;
        this.scheduledExecutorService = scheduledExecutorService;

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
        // Set up the heartbeat
        heartbeatFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            send(Constants.GSON.toJson(new WsToMessage(0, null, null)));
        }, 60, 60, TimeUnit.SECONDS);
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
            try {
                handleCandidateAdd(sessionId, content);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleConnectRequest(BigInteger from, String sessionId, String message) {
        try {
            var factory = new NistSdpFactory();

            var offer = factory.createSessionDescription(message);

            var stream = agent.createMediaStream("application");
            String fingerprint = null;
            for (Object mediaDescription : offer.getMediaDescriptions(false)) {
                var description = (MediaDescription) mediaDescription;
                for (Object descriptionAttribute : description.getAttributes(false)) {
                    var attribute = (Attribute) descriptionAttribute;
                    switch (attribute.getName()) {
                        case "ice-ufrag":
                            stream.setRemoteUfrag(attribute.getValue());
                            break;
                        case "ice-pwd":
                            stream.setRemotePassword(attribute.getValue());
                            break;
                        case "fingerprint":
                            fingerprint = attribute.getValue().split(" ")[1];
                            break;
                    }
                }
            }

            component = agent.createComponent(stream, 5000, 5000, 6000, KeepAliveStrategy.SELECTED_ONLY, true);

            var transport = new CustomDatagramTransport();

            var client = new DtlsClient(new JcaTlsCryptoProvider().create(SecureRandom.getInstanceStrong()), fingerprint);

            var answer = factory.createSessionDescription();
            answer.setOrigin(factory.createOrigin("-", Math.abs(new Random().nextLong()), 2L, "IN", "IP4", "127.0.0.1"));

            var attributes = new Vector<>();
            attributes.add(factory.createAttribute("group", "BUNDLE 0"));
            attributes.add(factory.createAttribute("extmap-allow-mixed", ""));
            attributes.add(factory.createAttribute("msid-semantic", " WMS"));
            answer.setAttributes(attributes);

            var media = factory.createMediaDescription("application", 9, 0, "UDP/DTLS/SCTP", new String[]{"webrtc-datachannel"});
            media.setConnection(factory.createConnection("IN", "IP4", "0.0.0.0"));
            media.setAttribute("ice-ufrag", agent.getLocalUfrag());
            media.setAttribute("ice-pwd", agent.getLocalPassword());
            media.setAttribute("ice-options", "trickle");
            media.setAttribute("fingerprint", "sha-256 " + client.getClientFingerprint());
            media.setAttribute("setup", "active");
            media.setAttribute("mid", "0");
            media.setAttribute("sctp-port", "5000");
            media.setAttribute("max-message-size", "262144");
            answer.setMediaDescriptions(new Vector<>(Collections.of(media)));

            var json = Constants.GSON.toJson(new WsToMessage(
                1, from, "CONNECTRESPONSE " + sessionId + " " + answer
            ));
//            System.out.println(json);
            send(json);

            int i = 0;
            for (LocalCandidate candidate : component.getLocalCandidates()) {
                var jsonAdd = Constants.GSON.toJson(new WsToMessage(
                    1, from, "CANDIDATEADD " + sessionId + " " + candidate.toString() + " generation 0 ufrag " + agent.getLocalUfrag() + " network-id " + i + " network-cost 0"
                ));
                i++;
//                System.out.println(jsonAdd);
                send(jsonAdd);
            }

            agent.addStateChangeListener(evt -> {
//                System.out.println("state change! " + evt);
                if ("IceProcessingState".equals(evt.getPropertyName()) && IceProcessingState.COMPLETED.equals(evt.getNewValue())) {
                    transport.init(component);
                    try {
                        var dtlsTransport = new DTLSClientProtocol().connect(client, transport);
//                        Log.setLevel(Log.DEBUG);

                        // Log the remote public IP
                        component.getRemoteCandidates().forEach(remoteCandidate -> {
                            if (remoteCandidate.getType() == CandidateType.SERVER_REFLEXIVE_CANDIDATE) {
                                System.out.println("Remote public IP: " + remoteCandidate.getTransportAddress().getHostAddress());
                            }
                        });

                        new ThreadedAssociation(dtlsTransport, new SctpAssociationListener());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

//        } catch (SdpException | FileNotFoundException | CertificateException | NoSuchAlgorithmException e) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        var session = pendingSession;
//        pendingSession = null;
//
//        activeSessions.put(sessionId, session);
//        session.receiveOffer(from, sessionId, message);
    }

    int candidateCount = 0;
    private void handleCandidateAdd(String sessionId, String message) throws UnknownHostException {
//        agent.candidate
//        activeSessions.get(sessionId).addCandidate(message);
        component.addUpdateRemoteCandidates(parseCandidate(message, component.getParentStream()));
        candidateCount++;

        if (candidateCount == 4) {
            component.updateRemoteCandidates();
            agent.startConnectivityEstablishment();
        }
    }

    public static RemoteCandidate parseCandidate(String value, IceMediaStream stream) {
        StringTokenizer tokenizer = new StringTokenizer(value);

        //XXX add exception handling.
        String foundation = tokenizer.nextToken();
        int componentID = Integer.parseInt( tokenizer.nextToken() );
        Transport transport = Transport.parse(tokenizer.nextToken());
        long priority = Long.parseLong(tokenizer.nextToken());
        String address = tokenizer.nextToken();
        int port = Integer.parseInt(tokenizer.nextToken());

        TransportAddress transAddr = new TransportAddress(address, port, transport);

        tokenizer.nextToken(); //skip the "typ" String
        CandidateType type = CandidateType.parse(tokenizer.nextToken());

        Component component = stream.getComponent(componentID);

        if(component == null)
            return null;

        // check if there's a related address property

        RemoteCandidate relatedCandidate = null;
        String ufrag = null;
        while (tokenizer.countTokens() >= 2) {
            String key = tokenizer.nextToken();
            String val = tokenizer.nextToken();

            if (key.equals("ufrag")) {
                ufrag = val;
                break;
            } else if (key.equals("raddr")) {
                tokenizer.nextToken(); // skip the rport element
                int relatedPort = Integer.parseInt(tokenizer.nextToken());

                TransportAddress raddr = new TransportAddress(val, relatedPort, Transport.UDP);

                relatedCandidate = component.findRemoteCandidate(raddr);
            }
        }

        return new RemoteCandidate(transAddr, component, type, foundation, priority, relatedCandidate, ufrag);
    }

    private void initialize(JsonObject message) {
        agent = new Agent();

        var turnAuthServers = message.getAsJsonArray("TurnAuthServers");
        for (JsonElement authServerElement : turnAuthServers) {
            var authServer = authServerElement.getAsJsonObject();
            var username = authServer.get("Username").getAsString();
            var password = authServer.get("Password").getAsString();
            authServer.getAsJsonArray("Urls").forEach(url -> {
                var parts = url.getAsString().split(":");
                String type = parts[0];
                String host = parts[1];
                int port = Integer.parseInt(parts[2]);

                if ("stun".equals(type)) {
                    agent.addCandidateHarvester(new StunCandidateHarvester(new TransportAddress(host, port, Transport.UDP)));
                } else if ("turn".equals(type)) {
                    agent.addCandidateHarvester(new TurnCandidateHarvester(
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

        logger.info("RTCWebsocket disconnected: " + reason + " (" + code + ")");
    }

    @Override
    public void onError(Exception ex) {
        logger.info("RTCWebsocket error: " + ex.getMessage());
    }

//    protected PeerConnectionFactory peerFactory() {
//        return peerFactory;
//    }
}