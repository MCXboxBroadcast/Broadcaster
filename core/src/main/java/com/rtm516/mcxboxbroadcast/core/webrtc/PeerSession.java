package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStream;
import java.math.BigInteger;

public class PeerSession implements PeerConnectionObserver {
    private final RtcWebsocketClient rtcWebsocket;

    private final RTCPeerConnection peerConnection;

    private String sessionId;
    private BigInteger from;

    public PeerSession(RtcWebsocketClient rtcWebsocket, PeerConnectionFactory factory, RTCConfiguration config) {
        this.rtcWebsocket = rtcWebsocket;

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

                        RTCDataChannel dataChannel = peerConnection.createDataChannel("ReliableDataChannel", new RTCDataChannelInit());
                        dataChannel.registerObserver(new MinecraftDataHandler(dataChannel, Constants.BEDROCK_CODEC, rtcWebsocket.sessionInfo(), rtcWebsocket.logger()));
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
        peerConnection.addIceCandidate(new RTCIceCandidate(null, Integer.parseInt(message.split(" ")[1]), message.substring(message.indexOf("candidate:"))));
    }

    private void disconnect() {
        peerConnection.close();
        rtcWebsocket.handleDisconnect(sessionId);
    }

    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {
        rtcWebsocket.logger().debug("ICE candidate: " + candidate);

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

        if (state == RTCPeerConnectionState.DISCONNECTED || state == RTCPeerConnectionState.FAILED) {
            disconnect();
        }
    }

    @Override
    public void onIceConnectionChange(RTCIceConnectionState state) {
        rtcWebsocket.logger().debug("ICE connection state change: " + state);

    }

    @Override
    public void onStandardizedIceConnectionChange(RTCIceConnectionState state) {
        rtcWebsocket.logger().debug("Standardized ICE connection state change: " + state);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        rtcWebsocket.logger().debug("ICE connection receiving change: " + receiving);
    }

    @Override
    public void onIceGatheringChange(RTCIceGatheringState state) {
        rtcWebsocket.logger().debug("ICE gathering state change: " + state);

        // Check if we finished gathering and didn't initialise any connections
        if (state == RTCIceGatheringState.COMPLETE && peerConnection.getConnectionState() == RTCPeerConnectionState.NEW) {
            disconnect();
        }
    }

    @Override
    public void onIceCandidateError(RTCPeerConnectionIceErrorEvent event) {
        rtcWebsocket.logger().error("ICE candidate error: " + event);
    }

    @Override
    public void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {
        rtcWebsocket.logger().debug("ICE candidates removed: " + candidates);
    }

    @Override
    public void onDataChannel(RTCDataChannel dataChannel) {
        rtcWebsocket.logger().debug("Data channel: " + dataChannel.getLabel());
    }

    @Override
    public void onRenegotiationNeeded() {
        rtcWebsocket.logger().debug("Renegotiation needed");
    }
}
