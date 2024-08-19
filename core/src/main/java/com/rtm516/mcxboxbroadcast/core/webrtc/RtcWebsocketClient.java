package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;
import javax.sdp.Attribute;
import javax.sdp.MediaDescription;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.DTLSClientProtocol;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.opentelecoms.javax.sdp.NistSdpFactory;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtcWebsocketClient extends WebSocketClient {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger;

    private RTCConfiguration rtcConfig;
    public PeerConnectionFactory peerFactory;
    private Agent agent;
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
//        agent.startCandidateTrickle(iceCandidates -> {
//            iceCandidates
//        });

        try {
            var factory = new NistSdpFactory();

            var offer = factory.createSessionDescription(message);

            String userFragment;
            String password;
            String fingerprint;
            for (Object mediaDescription : offer.getMediaDescriptions(false)) {
                var description = (MediaDescription) mediaDescription;
                for (Object descriptionAttribute : description.getAttributes(false)) {
                    var attribute = (Attribute) descriptionAttribute;
                    switch (attribute.getName()) {
                        case "ice-ufrag":
                            userFragment = attribute.getValue();
                            break;
                        case "ice-pwd":
                            password = attribute.getValue();
                            break;
                        case "fragment":
                            fingerprint = attribute.getValue();
                            break;
                    }
                }
            }

            var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            var keyPair = keyPairGenerator.generateKeyPair();

            var certGen = new X509V3CertificateGenerator();
            certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
            certGen.setIssuerDN(new X509Name("CN=Test Certificate"));
            certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000L));
            certGen.setNotAfter(new Date(System.currentTimeMillis() + 31536000000L));
            certGen.setSubjectDN(new X509Name("CN=Test Certificate"));
            certGen.setPublicKey(keyPair.getPublic());
            certGen.setSignatureAlgorithm("SHA256WithRSA");

            var crypto = new JcaTlsCryptoProvider().create(SecureRandom.getInstanceStrong());

            var client = new DefaultTlsClient(crypto) {
                @Override
                public TlsAuthentication getAuthentication() throws IOException {
                    return new TlsAuthentication() {
                        @Override
                        public void notifyServerCertificate(TlsServerCertificate serverCertificate) throws IOException {
                            if (serverCertificate == null || serverCertificate.getCertificate() == null) {
                                throw new TlsFatalAlert(AlertDescription.handshake_failure);
                            }
//                            var status =

//                            System.out.println("status type: " + serverCertificate.);
                        }

                        @Override
                        public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException {
//                            return new JcaDefaultTlsCredentialedSigner();
                            return null;
                        }
                    };
                }

                @Override
                protected ProtocolVersion[] getSupportedVersions() {
                    return new ProtocolVersion[]{ProtocolVersion.DTLSv12};
                }
            };

            IceMediaStream stream = agent.createMediaStream("data");
            Component component = agent.createComponent(stream, 5000, 5000, 6000);

            CustomDatagramTransport datagramTransport = new CustomDatagramTransport(component);

            var answer = factory.createSessionDescription();
            long answerSessionId = new Random().nextLong();
            while (answerSessionId < 0) {
                answerSessionId = new Random().nextLong();
            }
            answer.setOrigin(factory.createOrigin("-", answerSessionId, 2L, "IN", "IP4", "127.0.0.1"));

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
            answer.setMediaDescriptions(new Vector<>(Collections.of(media)));

            var json = Constants.GSON.toJson(new WsToMessage(
                1, from, "CONNECTRESPONSE " + sessionId + " " + answer
            ));
            System.out.println(json);
            send(json);


            component.getLocalCandidates().forEach(candidate -> {
                var jsonAdd = Constants.GSON.toJson(new WsToMessage(
                    1, from, "CANDIDATEADD " + sessionId + " " + candidate.toString() + " generation 0 ufrag " + agent.getLocalUfrag() + " network-id " + candidate.getFoundation()
                ));
                System.out.println(jsonAdd);
                send(jsonAdd);
            });

            // Move this since it errors since the socket isnt open, I assume bc we havent sent the CANDIDATEADD responses
//            new DTLSClientProtocol().connect(client, datagramTransport);

//        } catch (SdpException | FileNotFoundException | CertificateException | NoSuchAlgorithmException e) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("LETS GOOOO CONNECTREQUEST!!!!!");
//        var session = pendingSession;
//        pendingSession = null;
//
//        activeSessions.put(sessionId, session);
//        session.receiveOffer(from, sessionId, message);
    }

    private void handleCandidateAdd(String sessionId, String message) {
//        agent.candidate
//        activeSessions.get(sessionId).addCandidate(message);
    }

    private void initialize(JsonObject message) {
        var turnAuthServers = message.getAsJsonArray("TurnAuthServers");

//        rtcConfig = new RTCConfiguration();
//        for (JsonElement authServerElement : turnAuthServers) {
//            var authServer = authServerElement.getAsJsonObject();
//            var server = new RTCIceServer();
//            server.username = authServer.get("Username").getAsString();
//            server.password = authServer.get("Password").getAsString();
//            authServer.getAsJsonArray("Urls").forEach(url -> server.urls.add(url.getAsString()));
//            rtcConfig.iceServers.add(server);
//        }
//
//        pendingSession = new PeerSession(this, rtcConfig);

        agent = new Agent();

        for (JsonElement authServerElement : turnAuthServers) {
            var authServer = authServerElement.getAsJsonObject();
            var username = authServer.get("Username").getAsString();
            var password = authServer.get("Password").getAsString();
            authServer.getAsJsonArray("Urls").forEach(url -> {
                var parts = url.getAsString().split(":");
                String type = parts[0];
                String host = parts[1];
                int port = Integer.parseInt(parts[2]);

                agent.addCandidateHarvester(switch (type) {
                    case "stun":
                        yield new StunCandidateHarvester(new TransportAddress(host, port, Transport.UDP));
                    case "turn":
                        yield new TurnCandidateHarvester(
                            new TransportAddress(host, port, Transport.UDP),
                            new LongTermCredential(username, password)
                        );
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                });
            });
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
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