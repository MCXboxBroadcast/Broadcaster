package com.rtm516.mcxboxbroadcast.core.webrtc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.util.encoders.Hex;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.ice4j.ice.CandidateType;
import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPByteStreamListener;
import pe.pi.sctp4j.sctp.SCTPStream;

import java.util.HashMap;
import java.util.Map;

public class SctpAssociationListener implements AssociationListener {
    private final Map<String, SCTPStream> streams = new HashMap<>();
    private final BedrockCodec codec = Bedrock_v712.CODEC;
    private final BedrockPacketCodec packetCodec = new BedrockPacketCodec_v3();
    private final BedrockCodecHelper helper = codec.createHelper();

    @Override
    public void onAssociated(Association association) {
        System.out.println("Association associated: " + association.toString());
    }

    @Override
    public void onDisAssociated(Association association) {
        System.out.println("Association disassociated: " + association.toString());
    }

    @Override
    public void onDCEPStream(SCTPStream sctpStream, String label, int i) throws Exception {
        if (label == null) {
            return;
        }
        System.out.println("Received DCEP SCTP stream: " + sctpStream.toString());
        streams.put(sctpStream.getLabel(), sctpStream);

        sctpStream.setSCTPStreamListener(new SCTPByteStreamListener() {
            private ByteBuf concat;

            @Override
            public void onMessage(SCTPStream sctpStream, byte[] bytes) {
                try {
                    System.out.println("binary message (" + sctpStream.getLabel() + "): " + Hex.toHexString(bytes));
                    if (bytes.length == 0) {
                        throw new IllegalStateException("Expected at least 2 bytes");
                    }
                    //todo only do this if segmentcount > 0
                    var buf = Unpooled.buffer(bytes.length);
                    buf.writeBytes(bytes);

                    byte remainingSegments = buf.readByte();
                    int packetLength = VarInts.readInt(buf);

                    if (remainingSegments > 0) {
                        if (concat == null) {
                            concat = buf;
                        } else {
                            concat.writeBytes(buf, packetLength);
                        }
                        return;
                    }

                    if (concat != null) {
                        concat.writeBytes(buf, packetLength);
                        buf = concat;
                        concat = null;
                    }

                    var packet = readPacket(buf);

                    System.out.println("GOT PACKET");
                    System.out.println(packet);
                    if (packet instanceof RequestNetworkSettingsPacket) {
                        var networkSettings = new NetworkSettingsPacket();
                        networkSettings.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);
                        networkSettings.setCompressionThreshold(0);
                        sendPacket(networkSettings, sctpStream);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(SCTPStream sctpStream, String s) {
                System.out.println("string message (" + sctpStream.getLabel() + "): " + s);
            }

            @Override
            public void close(SCTPStream sctpStream) {
                System.out.println("stream closed: " + sctpStream.getLabel());
            }
        });
    }

    @Override
    public void onRawStream(SCTPStream sctpStream) {
        System.out.println("Received raw SCTP stream: " + sctpStream.toString());
    }

    private void sendPacket(BedrockPacket packet, String streamLabel) {
        sendPacket(packet, streams.get(streamLabel));
    }

    private void sendPacket(BedrockPacket packet, SCTPStream stream) {
        try {
            ByteBuf dataBuf = Unpooled.buffer(128);
            int packetId = codec.getPacketDefinition(packet.getClass()).getId();
            System.out.println("packet id: " + packetId);
            packetCodec.encodeHeader(
                dataBuf,
                BedrockPacketWrapper.create(packetId, 0, 0, null, null)
            );
            codec.tryEncode(helper, dataBuf, packet);

            // Segment
            // 00
            // Packet Length
            // 18   - 12
            // Packet Header (VarInt)
            // 8f 01 00 00 00 00
            // Packet Data
            // 00 - Compression threshold
            // 00 - Compression method
            // 00 00 00 00

            // 00
            // 18
            // 8f 01 00
            // 02 00 - Compression threshold
            // 00 00 - Compression method
            // 00 - Client throttle enabled
            // 00 - Client throttle threshold
            // 00 00 00 - Client throttle scalar

            // 00
            // 18
            // 8f 01 00
            // 00 00
            // 00 00
            // 00
            // 00
            // 00 00 00

            // 00
            // 18
            // 8f 01 00
            // 00 00
            // 00 00
            // 00
            // 00
            // 00 00 00



            int segmentCount = (int) Math.ceil(dataBuf.readableBytes() / 10_000f);
            for (int remainingSegements = segmentCount - 1; remainingSegements >= 0; remainingSegements--) {
                int segmentLength = (remainingSegements == 0 ? dataBuf.readableBytes() : 10_000);
                var sendBuf = Unpooled.buffer(1);
                sendBuf.writeByte(1);
                VarInts.writeInt(sendBuf, segmentLength);
                sendBuf.writeBytes(dataBuf, segmentLength);

                byte[] send = new byte[sendBuf.readableBytes()];
                sendBuf.readBytes(send);
//                                        byte[] send = HexFormat.of().parseHex("00188f010000000000000000000000");
                System.out.println("sending: " + Hex.toHexString(send));
                stream.send(send);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BedrockPacket readPacket(ByteBuf buf) {
        BedrockPacketWrapper wrapper = BedrockPacketWrapper.create();
        packetCodec.decodeHeader(buf, wrapper);
        System.out.println("sender/target: " + wrapper.getSenderSubClientId() + " " + wrapper.getTargetSubClientId());
        var packet = codec.tryDecode(helper, buf, wrapper.getPacketId());
        // release it
        wrapper.getHandle().recycle(wrapper);
        return packet;
    }
}