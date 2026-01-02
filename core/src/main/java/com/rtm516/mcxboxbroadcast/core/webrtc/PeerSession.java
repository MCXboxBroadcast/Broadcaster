package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import dev.kastle.webrtc.CreateSessionDescriptionObserver;
import dev.kastle.webrtc.PeerConnectionFactory;
import dev.kastle.webrtc.PeerConnectionObserver;
import dev.kastle.webrtc.RTCAnswerOptions;
import dev.kastle.webrtc.RTCConfiguration;
import dev.kastle.webrtc.RTCDataChannel;
import dev.kastle.webrtc.RTCDataChannelInit;
import dev.kastle.webrtc.RTCIceCandidate;
import dev.kastle.webrtc.RTCIceConnectionState;
import dev.kastle.webrtc.RTCIceGatheringState;
import dev.kastle.webrtc.RTCPeerConnection;
import dev.kastle.webrtc.RTCPeerConnectionIceErrorEvent;
import dev.kastle.webrtc.RTCPeerConnectionState;
import dev.kastle.webrtc.RTCSdpType;
import dev.kastle.webrtc.RTCSessionDescription;
import dev.kastle.webrtc.RTCSignalingState;
import dev.kastle.webrtc.SetSessionDescriptionObserver;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PeerSession implements PeerConnectionObserver {
    private final RtcWebsocketClient rtcWebsocket;
    private final SessionManagerCore sessionManager;

    private final RTCPeerConnection peerConnection;

    private String sessionId;
    private BigInteger from;

    private Set<String> localCandidates = new HashSet<>();
    private Set<String> remoteCandidates = new HashSet<>();

    private boolean hadFirstCandidate = false;
    private long lastCandidateTime = 0;

    public PeerSession(RtcWebsocketClient rtcWebsocket, PeerConnectionFactory factory, RTCConfiguration config, SessionManagerCore sessionManager) {
        this.rtcWebsocket = rtcWebsocket;
        this.sessionManager = sessionManager;

        this.peerConnection = factory.createPeerConnection(config, this);
    }

    public void receiveOffer(BigInteger from, String sessionId, String message) {
        this.sessionId = sessionId;
        this.from = from;

        peerConnection.setRemoteDescription(new RTCSessionDescription(RTCSdpType.OFFER, message), new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                rtcWebsocket.logger().debug("Successfully set remote description");
            }

            @Override
            public void onFailure(String error) {
                rtcWebsocket.logger().error("Failed to set remote description: " + error);
            }
        });

        peerConnection.createAnswer(new RTCAnswerOptions(), new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        String json = Constants.GSON.toJson(new WsToMessage(
                            1, from, "CONNECTRESPONSE " + sessionId + " " + description.sdp
                        ));
                        rtcWebsocket.send(json);

                        // Schedule a check in 15 seconds to see if we have had a candidate, if not disconnect
                        rtcWebsocket.scheduledExecutorService().schedule(() -> {
                            if (remoteCandidates.isEmpty()) {
                                rtcWebsocket.logger().error("No candidates sent by the client after 15s, please reconnect and try again");
                                disconnect();
                            }
                        }, 15, TimeUnit.SECONDS);

                        RTCDataChannel dataChannel = peerConnection.createDataChannel("ReliableDataChannel", new RTCDataChannelInit());
                        dataChannel.registerObserver(new MinecraftDataHandler(dataChannel, Constants.BEDROCK_CODEC, rtcWebsocket.sessionInfo(), rtcWebsocket.logger(), sessionManager));
                    }

                    @Override
                    public void onFailure(String error) {
                        rtcWebsocket.logger().error("Failed to set local description: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                rtcWebsocket.logger().error("Failed to create answer: " + error);
            }
        });
    }

    public void addCandidate(String message) {
        RTCIceCandidate candidate = new RTCIceCandidate(null, Integer.parseInt(message.split(" ")[1]), message.substring(message.indexOf("candidate:")));
        remoteCandidates.add(message);
        peerConnection.addIceCandidate(candidate);
    }

    private void disconnect() {
        peerConnection.close();
        rtcWebsocket.handleDisconnect(sessionId);
    }

    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {
        localCandidates.add(candidate.sdp);
        String jsonAdd = Constants.GSON.toJson(new WsToMessage(
            1, from, "CANDIDATEADD " + sessionId + " " + candidate.sdp
        ));
        rtcWebsocket.send(jsonAdd);
    }

    @Override
    public void onSignalingChange(RTCSignalingState state) {
        rtcWebsocket.logger().debug("Signaling state change: " + state);
    }

    @Override
    public void onConnectionChange(RTCPeerConnectionState state) {
        rtcWebsocket.logger().debug("Connection state change: " + state);

        if (state == RTCPeerConnectionState.CONNECTING) {
            // Schedule a check in 15 seconds to see if we are connected, if not disconnect
            rtcWebsocket.scheduledExecutorService().schedule(() -> {
                if (peerConnection.getConnectionState() == RTCPeerConnectionState.CONNECTING) {
                    rtcWebsocket.logger().error("Failure to create connection to the client after 15s, please reconnect and try again");
                    disconnect();
                }
            }, 15, TimeUnit.SECONDS);
        }

        if (state == RTCPeerConnectionState.FAILED) {
            rtcWebsocket.logger().error("Failure to establish ICE connection, likely due to a network issue. Please check Xbox Live status and firewall configuration.");
        }

        if (state == RTCPeerConnectionState.DISCONNECTED || state == RTCPeerConnectionState.FAILED) {
            disconnect();
        }
    }

    @Override
    public void onIceConnectionChange(RTCIceConnectionState state) {
        rtcWebsocket.logger().debug("ICE connection state change: " + state);

    }

    @Override
    public void onIceGatheringChange(RTCIceGatheringState state) {
        rtcWebsocket.logger().debug("ICE gathering state change: " + state);

        // When we complete the gathering then log all candidates for debugging
        if (state == RTCIceGatheringState.COMPLETE) {
            StringBuilder sb = new StringBuilder();
            sb.append("ICE gathering complete:\n");

            sb.append(localCandidates.size()).append(" Local candidates:\n");
            localCandidates.forEach(localCandidate -> {
                sb.append(localCandidate).append("\n");
            });

            sb.append(remoteCandidates.size()).append(" Remote candidates:\n");
            remoteCandidates.forEach(localCandidate -> {
                sb.append(localCandidate).append("\n");
            });

            rtcWebsocket.logger().debug(sb.toString());
        }

        // Check if we finished gathering and didn't initialise any connections
        if (state == RTCIceGatheringState.COMPLETE && peerConnection.getConnectionState() == RTCPeerConnectionState.NEW) {
            disconnect();
        }
    }

    @Override
    public void onIceCandidateError(RTCPeerConnectionIceErrorEvent event) {
        if (event.getErrorCode() == 701) {
            // Ignore 701 errors as they are expected when a network adapter doesn't have internet access
            return;
        }

        rtcWebsocket.logger().error("ICE candidate error: " + event.getAddress() + ":"  + event.getPort() + " -> " + event.getUrl() + " = " + event.getErrorCode() + ": " + event.getErrorText());
    }

    @Override
    public void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {
        rtcWebsocket.logger().debug("ICE candidates removed: " + candidates);
    }

    @Override
    public void onDataChannel(RTCDataChannel dataChannel) {
        rtcWebsocket.logger().debug("Received data channel: " + dataChannel.getLabel());
    }

    @Override
    public void onRenegotiationNeeded() {
        rtcWebsocket.logger().debug("Renegotiation needed");
    }
}
