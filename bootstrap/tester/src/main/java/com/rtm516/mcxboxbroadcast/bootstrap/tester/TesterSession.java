package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import com.rtm516.mcxboxbroadcast.core.webrtc.IceLogger;
import io.jsonwebtoken.lang.Collections;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider;
import org.ice4j.ice.Agent;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.KeepAliveStrategy;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.harvest.CandidateHarvester;
import org.opentelecoms.javax.sdp.NistSdpFactory;

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
import java.util.Vector;

public class TesterSession {
    private final TesterRtcWebsocketClient rtcWebsocket;
    private final Agent agent;
    private final String sessionId;
    private final BigInteger netherNetId;

    private DtlsServer server;
    private Component component;

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
}
