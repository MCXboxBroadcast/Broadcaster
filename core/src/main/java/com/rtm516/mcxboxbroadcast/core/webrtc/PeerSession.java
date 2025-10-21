package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import io.jsonwebtoken.lang.Collections;
import org.bouncycastle.tls.DTLSClientProtocol;
import org.bouncycastle.tls.DTLSTransport;
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
import org.ice4j.ice.harvest.CandidateHarvester;
import org.opentelecoms.javax.sdp.NistSdpFactory;
import pe.pi.sctp4j.sctp.small.CachedThreadedAssociation;

import javax.sdp.Attribute;
import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class PeerSession {
    private final RtcWebsocketClient rtcWebsocket;
    private final SessionManagerCore sessionManager;

    private final Agent agent;
    private Component component;
    private DTLSTransport dtlsTransport;
    private String sessionId;

    private boolean hadFirstCandidate = false;
    private long lastCandidateTime = 0;

    public PeerSession(RtcWebsocketClient rtcWebsocket, List<CandidateHarvester> candidateHarvesters, SessionManagerCore sessionManager) {
        this.rtcWebsocket = rtcWebsocket;
        this.sessionManager = sessionManager;

        this.agent = new Agent(new IceLogger(rtcWebsocket.logger()));
        for (CandidateHarvester harvester : candidateHarvesters) {
            agent.addCandidateHarvester(harvester);
        }
    }

    public void receiveOffer(BigInteger from, String sessionId, String message) {
        this.sessionId = sessionId;
        try {
            NistSdpFactory factory = new NistSdpFactory();

            SessionDescription offer = factory.createSessionDescription(message);

            IceMediaStream stream = agent.createMediaStream("application");
            String fingerprint = null;
            for (Object mediaDescription : offer.getMediaDescriptions(false)) {
                MediaDescription description = (MediaDescription) mediaDescription;
                for (Object descriptionAttribute : description.getAttributes(false)) {
                    Attribute attribute = (Attribute) descriptionAttribute;
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

            component = agent.createComponent(stream, KeepAliveStrategy.SELECTED_ONLY, true);

            CustomDatagramTransport transport = new CustomDatagramTransport();

            DtlsClient client = new DtlsClient(new JcaTlsCryptoProvider().create(SecureRandom.getInstanceStrong()), fingerprint, rtcWebsocket.logger());

            SessionDescription answer = factory.createSessionDescription();
            answer.setOrigin(factory.createOrigin("-", Math.abs(new Random().nextLong()), 2L, "IN", "IP4", "127.0.0.1"));

            Vector<Attribute> attributes = new Vector<>();
            attributes.add(factory.createAttribute("group", "BUNDLE 0"));
            attributes.add(factory.createAttribute("extmap-allow-mixed", ""));
            attributes.add(factory.createAttribute("msid-semantic", " WMS"));
            answer.setAttributes(attributes);

            MediaDescription media = factory.createMediaDescription("application", 9, 0, "UDP/DTLS/SCTP", new String[]{"webrtc-datachannel"});
            media.setConnection(factory.createConnection("IN", "IP4", "0.0.0.0"));
            media.setAttribute("ice-ufrag", agent.getLocalUfrag());
            media.setAttribute("ice-pwd", agent.getLocalPassword());
            media.setAttribute("ice-options", "trickle");
            media.setAttribute("fingerprint", "sha-256 " + client.getClientFingerprint());
            media.setAttribute("setup", "active");
            media.setAttribute("mid", "0");
            media.setAttribute("sctp-port", String.valueOf(getComponentPort(5000)));
            media.setAttribute("max-message-size", "262144");
            answer.setMediaDescriptions(new Vector<>(Collections.of(media)));

            String json = Constants.GSON.toJson(new WsToMessage(
                1, from, "CONNECTRESPONSE " + sessionId + " " + answer
            ));
            rtcWebsocket.send(json);

            // If we don't receive a candidate within 15 seconds, disconnect
            rtcWebsocket.scheduledExecutorService().schedule(() -> {
                if (!hadFirstCandidate) {
                    disconnect();
                }
            }, 15, TimeUnit.SECONDS);

            int i = 0;
            for (LocalCandidate candidate : component.getLocalCandidates()) {
                String jsonAdd = Constants.GSON.toJson(new WsToMessage(
                    1, from, "CANDIDATEADD " + sessionId + " " + candidate.toString() + " generation 0 ufrag " + agent.getLocalUfrag() + " network-id " + i + " network-cost 0"
                ));
                i++;
                rtcWebsocket.send(jsonAdd);
            }

            agent.addStateChangeListener(evt -> {
                if (!"IceProcessingState".equals(evt.getPropertyName())) return;
                if (IceProcessingState.COMPLETED.equals(evt.getNewValue())) {
                    transport.init(component);
                    try {
                        dtlsTransport = new DTLSClientProtocol().connect(client, transport);
//                        Log.setLevel(Log.DEBUG);

                        // Log the remote public IP
//                        component.getRemoteCandidates().forEach(remoteCandidate -> {
//                            if (remoteCandidate.getType() == CandidateType.SERVER_REFLEXIVE_CANDIDATE) {
//                                System.out.println("Remote public IP: " + remoteCandidate.getTransportAddress().getHostAddress());
//                            }
//                        });

                        // TODO Pass some form of close handler to the association so we can clean up properly in the RtcWebsocketClient
                        new CachedThreadedAssociation(dtlsTransport, new SctpAssociationListener(rtcWebsocket.sessionInfo(), rtcWebsocket.logger(), this::disconnect, sessionManager), this.rtcWebsocket.scheduledExecutorService());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (IceProcessingState.FAILED.equals(evt.getNewValue())) {
                    rtcWebsocket.logger().error("Failure to establish ICE connection, likely due to a network issue. Please check Xbox Live status and firewall configuration.");
                    disconnect();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addCandidate(String message) {
        component.addRemoteCandidate(parseCandidate(message, component.getParentStream()));
        lastCandidateTime = System.currentTimeMillis();

        if (!hadFirstCandidate) {
            hadFirstCandidate = true;
            new Thread(() -> {
                try {
                    while (System.currentTimeMillis() - lastCandidateTime < 200) {
                        Thread.sleep(200);
                    }
                    agent.startConnectivityEstablishment();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private RemoteCandidate parseCandidate(String value, IceMediaStream stream) {
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

    /**
     * Get the port of the first host candidate.
     *
     * @param fallback the port to return if no host candidate is found
     * @return the port of the first host candidate or the fallback port
     */
    private int getComponentPort(int fallback) {
        int port = fallback;
        for (LocalCandidate localCandidate : component.getLocalCandidates()) {
            if (localCandidate.getType() == CandidateType.HOST_CANDIDATE) {
                port = localCandidate.getTransportAddress().getPort();
                break;
            }
        }

        return port;
    }

    private void disconnect() {
        try {
            if (dtlsTransport != null) dtlsTransport.close();
            agent.free();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        rtcWebsocket.handleDisconnect(sessionId);
    }
}
