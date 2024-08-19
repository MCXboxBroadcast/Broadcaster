package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import dev.onvoid.webrtc.CreateSessionDescriptionObserver;
import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCAnswerOptions;
import dev.onvoid.webrtc.RTCBundlePolicy;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelInit;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCIceGatheringState;
import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.RTCPeerConnectionIceErrorEvent;
import dev.onvoid.webrtc.RTCPeerConnectionState;
import dev.onvoid.webrtc.RTCRtcpMuxPolicy;
import dev.onvoid.webrtc.RTCRtpReceiver;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.RTCSdpType;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.RTCSignalingState;
import dev.onvoid.webrtc.SetSessionDescriptionObserver;
import dev.onvoid.webrtc.media.MediaStream;
import java.math.BigInteger;
import java.util.Arrays;

public class PeerSession implements PeerConnectionObserver {
    private final RtcWebsocketClient rtcClient;
    private RTCPeerConnection peerConnection;
    private BigInteger from;
    private String sessionId;

    public PeerSession(RtcWebsocketClient client, RTCConfiguration config) {
        this.rtcClient = client;
//        this.peerConnection = client.peerFactory.createPeerConnection(config, this);
    }

    public void receiveOffer(BigInteger from, String sessionId, String offer) {
        this.from = from;
        this.sessionId = sessionId;

        peerConnection.setRemoteDescription(new RTCSessionDescription(RTCSdpType.OFFER, offer), new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                peerConnection.createAnswer(new RTCAnswerOptions(), new CreateSessionDescriptionObserver() {
                    @Override
                    public void onSuccess(RTCSessionDescription description) {
                        System.out.println("answer success!");

                        peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                            @Override
                            public void onSuccess() {
                                System.out.println(description.sdp);
                                var json = Constants.GSON.toJson(new WsToMessage(
                                        1, from, "CONNECTRESPONSE " + sessionId + " " + description.sdp
                                ));
                                System.out.println(json);
                                rtcClient.send(json);
                            }

                            @Override
                            public void onFailure(String error) {
                                System.out.println("failure " + error);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        System.out.println("failure " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                System.out.println("remote description error: " + error);
            }
        });
    }

    public void addCandidate(String candidate) {
        peerConnection.addIceCandidate(new RTCIceCandidate(null, 0, candidate));
    }

    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {
        System.out.println("on ice candidate: " + candidate);
        rtcClient.send(Constants.GSON.toJson(new WsToMessage(1, from, "CANDIDATEADD " + sessionId + " " + candidate.sdp)));
    }

    @Override
    public void onConnectionChange(RTCPeerConnectionState state) {
        System.out.println("state change: " + state);
        if (state == RTCPeerConnectionState.CONNECTED) {
            new Thread(() -> {
                try {
                    Thread.sleep(2_500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("closing!");
                peerConnection.close();
            }).start();
        }
    }

    @Override
    public void onIceConnectionChange(RTCIceConnectionState state) {
        System.out.println("ice state change: " + state);
    }

    @Override
    public void onSignalingChange(RTCSignalingState state) {
        System.out.println("signaling state change: " + state);
    }

    @Override
    public void onStandardizedIceConnectionChange(RTCIceConnectionState state) {
        System.out.println("ice connection state change: " + state);
    }

    @Override
    public void onTrack(RTCRtpTransceiver transceiver) {
        System.out.println("track: " + transceiver);
    }

    @Override
    public void onDataChannel(RTCDataChannel dataChannel) {
        System.out.println("received data channel! ");
        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(long previousAmount) {
                System.out.println("buffer amount changed!");
            }

            @Override
            public void onStateChange() {
                System.out.println("state changed! " + dataChannel.getState());
            }

            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                System.out.println("received data channel buffer!\n" + buffer.data.toString());
            }
        });
    }

    @Override
    public void onIceGatheringChange(RTCIceGatheringState state) {
        System.out.println("ice gathering state change: " + state);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        System.out.println("ice connection state change: " + receiving);
    }

    @Override
    public void onIceCandidateError(RTCPeerConnectionIceErrorEvent event) {
//        System.out.println("ice candidate error: " + event);
        System.out.println(event.getErrorCode() + " " + event.getAddress() + " " + event.getUrl() + " " + event.getPort() + " " + event.getErrorText());
    }

    @Override
    public void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {
        System.out.println("ice candidates removed: " + Arrays.toString(candidates));
    }

    @Override
    public void onRenegotiationNeeded() {
        System.out.println("renegotiation needed!");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        System.out.println("add stream: " + stream);
    }

    @Override
    public void onAddTrack(RTCRtpReceiver receiver, MediaStream[] mediaStreams) {
        System.out.println("add track: " + Arrays.toString(mediaStreams));
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        System.out.println("remove stream: " + stream);
    }

    @Override
    public void onRemoveTrack(RTCRtpReceiver receiver) {
        System.out.println("remote track: " + receiver);
    }
}
