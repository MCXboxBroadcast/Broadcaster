package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import com.rtm516.mcxboxbroadcast.core.webrtc.CustomDatagramTransport;
import com.rtm516.mcxboxbroadcast.core.webrtc.IceLogger;
import com.rtm516.mcxboxbroadcast.core.webrtc.MinecraftDataHandler;
import com.rtm516.mcxboxbroadcast.core.webrtc.SctpAssociationListener;
import io.jsonwebtoken.lang.Collections;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tls.DTLSClientProtocol;
import org.bouncycastle.tls.DTLSServerProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
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
import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPStream;
import pe.pi.sctp4j.sctp.SCTPStreamListener;
import pe.pi.sctp4j.sctp.small.ThreadedAssociation;

import javax.sdp.Attribute;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

public class TesterSession {
    private final TesterRtcWebsocketClient rtcWebsocket;
    private final Agent agent;
    private final String sessionId;
    private final BigInteger netherNetId;

    private DtlsServer server;
    private Component component;
    private DTLSTransport dtlsTransport;

    private boolean hadFirstCandidate = false;
    private long lastCandidateTime = 0;

    public TesterSession(TesterRtcWebsocketClient rtcWebsocket, BigInteger netherNetId, List<CandidateHarvester> candidateHarvesters) {
        this.rtcWebsocket = rtcWebsocket;
        this.sessionId = String.valueOf(Math.abs(new Random().nextLong()));
        this.netherNetId = netherNetId;

        this.agent = new Agent(new IceLogger(rtcWebsocket.logger()));
        for (CandidateHarvester harvester : candidateHarvesters) {
            agent.addCandidateHarvester(harvester);
        }
    }

    public void sendOffer() throws NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        this.server = new DtlsServer(new JcaTlsCryptoProvider().create(SecureRandom.getInstanceStrong()), rtcWebsocket.logger());

        try {
            sendConnectionRequest();
        } catch (SdpException | CertificateException e) {
            rtcWebsocket.logger().error("Failed to send connection request", e);
            return;
        }
    }

    private void sendConnectionRequest() throws SdpException, CertificateException {
        NistSdpFactory factory = new NistSdpFactory();
        SessionDescription sdp = factory.createSessionDescription();

        // Set origin
        sdp.setOrigin(factory.createOrigin("-", Math.abs(new Random().nextLong()), 2L, "IN", "IP4", "127.0.0.1"));

        // Add session-level attributes
        sdp.setAttribute("group", "BUNDLE 0");
        sdp.setAttribute("extmap-allow-mixed", "");
        sdp.setAttribute("msid-semantic", "WMS");

        // Create and set media description
        MediaDescription media = factory.createMediaDescription("application", 9, 1, "UDP/DTLS/SCTP", new String[] {"webrtc-datachannel"});
        media.setConnection(factory.createConnection("IN", "IP4", "0.0.0.0"));
        media.setAttribute("ice-ufrag", agent.getLocalUfrag());
        media.setAttribute("ice-pwd", agent.getLocalPassword());
        media.setAttribute("ice-options", "trickle");
        media.setAttribute("fingerprint", "sha-256 " + server.getServerFingerprint());
        media.setAttribute("setup", "actpass");
        media.setAttribute("mid", "0");
        media.setAttribute("sctp-port", "5000");
        media.setAttribute("max-message-size", "262144");

        sdp.setMediaDescriptions(new Vector<>(Collections.of(media)));

        // Construct and send the CONNECTREQUEST message
        rtcWebsocket.send(Constants.GSON.toJson(new WsToMessage(1, this.netherNetId, "CONNECTREQUEST " + sessionId + " " + sdp)));

        CustomDatagramTransport transport = new CustomDatagramTransport();

        agent.addStateChangeListener(evt -> {
            if (!"IceProcessingState".equals(evt.getPropertyName())) return;
            if (IceProcessingState.COMPLETED.equals(evt.getNewValue())) {
                transport.init(component);
                try {
                    dtlsTransport = new DTLSServerProtocol().accept(server, transport);
//                        Log.setLevel(Log.DEBUG);

                    // Log the remote public IP
//                        component.getRemoteCandidates().forEach(remoteCandidate -> {
//                            if (remoteCandidate.getType() == CandidateType.SERVER_REFLEXIVE_CANDIDATE) {
//                                System.out.println("Remote public IP: " + remoteCandidate.getTransportAddress().getHostAddress());
//                            }
//                        });

                    // TODO Pass some form of close handler to the association so we can clean up properly in the RtcWebsocketClient
                    new ThreadedAssociation(dtlsTransport, new AssociationListener() {
                        @Override
                        public void onAssociated(Association association) {
                            rtcWebsocket.logger().debug("SCTP session associated");
                            try {
                                SCTPStream reliableDataChannel = association.mkStream("ReliableDataChannel");
                                SCTPStream unreliableDataChannel = association.mkStream("UnreliableDataChannel"); // Not used but without it the vanilla client ignores us

                                TesterMinecraftDataHandler dataHandler = new TesterMinecraftDataHandler(reliableDataChannel, Constants.BEDROCK_CODEC, rtcWebsocket.logger());
                                reliableDataChannel.setSCTPStreamListener(dataHandler);

                                RequestNetworkSettingsPacket requestNetworkSettingsPacket = new RequestNetworkSettingsPacket();
                                requestNetworkSettingsPacket.setProtocolVersion(Constants.BEDROCK_CODEC.getProtocolVersion());
                                dataHandler.sendPacket(requestNetworkSettingsPacket);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onDisAssociated(Association association) {

                        }

                        @Override
                        public void onDCEPStream(SCTPStream sctpStream, String label, int i) throws Exception {
                            if (label == null) {
                                return;
                            }
                            rtcWebsocket.logger().debug("Received DCEP SCTP stream: " + sctpStream.toString());

                            if ("ReliableDataChannel".equals(label)) {
                                rtcWebsocket.logger().info("ReliableDataChannel received");
                            }
                        }

                        @Override
                        public void onRawStream(SCTPStream sctpStream) {

                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
//                rtcWebsocket.logger().debug("ICE CONNECTION ESTABLISHED");
            } else if (IceProcessingState.FAILED.equals(evt.getNewValue())) {
                rtcWebsocket.logger().error("Failure to establish ICE connection, likely due to a network issue. Please check Xbox Live status and firewall configuration.");
//                disconnect();
            }
        });
    }

    public void parseResponse(String message) {
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
        } catch (SdpException | IOException e) {
            rtcWebsocket.logger().error("Failed to parse response", e);
        }
    }

    public void sendCandidates(String sessionId) {
        int i = 0;
        for (LocalCandidate candidate : component.getLocalCandidates()) {
            String jsonAdd = Constants.GSON.toJson(new WsToMessage(
                1, netherNetId, "CANDIDATEADD " + sessionId + " " + candidate.toString() + " generation 0 ufrag " + agent.getLocalUfrag() + " network-id " + i + " network-cost 0"
            ));
            i++;
            rtcWebsocket.send(jsonAdd);
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
}
