package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.webrtc.bedrock.RedirectPacketHandler;
import com.rtm516.mcxboxbroadcast.core.webrtc.encryption.BedrockEncryptionEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javax.crypto.SecretKey;
import org.bouncycastle.util.encoders.Hex;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.util.VarInts;
import pe.pi.sctp4j.sctp.SCTPByteStreamListener;
import pe.pi.sctp4j.sctp.SCTPStream;

public class MinecraftDataHandler implements SCTPByteStreamListener {
    private final BedrockPacketCodec packetCodec = new BedrockPacketCodec_v3();
    private final SCTPStream sctpStream;
    private final BedrockCodec codec;
    private final BedrockCodecHelper helper;
    private final RedirectPacketHandler redirectPacketHandler;

    private boolean compressionEnabled;
    private BedrockEncryptionEncoder encryptionEncoder;

    private ByteBuf concat;
    private int expectedLength;

    public MinecraftDataHandler(SCTPStream sctpStream, BedrockCodec codec) {
        this.sctpStream = sctpStream;
        this.codec = codec;
        this.helper = codec.createHelper();

        this.redirectPacketHandler = new RedirectPacketHandler(this);
    }

    @Override
    public void onMessage(SCTPStream sctpStream, byte[] bytes) {
        try {
//            System.out.println("binary message (" + sctpStream.getLabel() + "): " + Hex.toHexString(bytes));
            if (bytes.length == 0) {
                throw new IllegalStateException("Expected at least 2 bytes");
            }
            //todo only do this if segmentcount > 0
            var buf = Unpooled.buffer(bytes.length);
            buf.writeBytes(bytes);

            byte remainingSegments = buf.readByte();
            System.out.println("first 100 bytes: " + Hex.toHexString(bytes, 0, Math.min(100, bytes.length)));

            if (concat == null) {
                if (compressionEnabled) {
                    if (0xff != buf.readUnsignedByte()) {
                        throw new IllegalStateException("Expected none compression!");
                    }
                }
                expectedLength = VarInts.readUnsignedInt(buf);
            }

            if (remainingSegments > 0) {
                if (concat == null) {
                    concat = buf;
                } else {
                    concat.writeBytes(buf);
                }
                return;
            }

            if (concat != null) {
                concat.writeBytes(buf);
                buf = concat;
                concat = null;
            }

//            if (buf.readableBytes() != expectedLength) {
//                System.out.println("expected " + expectedLength + " bytes but got " + buf.readableBytes());
//                var disconnect = new DisconnectPacket();
//                disconnect.setReason(DisconnectFailReason.BAD_PACKET);
//                disconnect.setKickMessage("");
//                sendPacket(disconnect);
//                return;
//            }

            var packet = readPacket(buf);

            if (!(packet instanceof LoginPacket)) {
                System.out.println("C -> S: " + packet);
            }

            packet.handle(redirectPacketHandler);
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

    public void sendPacket(BedrockPacket packet) {
        System.out.println("S -> C: " + packet);
        try {
            ByteBuf dataBuf = Unpooled.buffer(128);
            var shiftedBytes = (compressionEnabled ? 1 : 0) + 5; // leave enough room for compression byte & data length
            dataBuf.writerIndex(shiftedBytes);

            int packetId = codec.getPacketDefinition(packet.getClass()).getId();
//            System.out.println("packet id: " + packetId);
            packetCodec.encodeHeader(
                    dataBuf,
                    BedrockPacketWrapper.create(packetId, 0, 0, null, null)
            );
            codec.tryEncode(helper, dataBuf, packet);

            var lastPacketByte = dataBuf.writerIndex();
            dataBuf.readerIndex(shiftedBytes);
            System.out.println("packet: " + Hex.toHexString(encode(dataBuf)));

            var packetLength = lastPacketByte - shiftedBytes;
            // read from the first actual byte
            dataBuf.readerIndex(5 - Utils.varintSize(packetLength));
            dataBuf.writerIndex(dataBuf.readerIndex());

            if (compressionEnabled) {
                dataBuf.writeByte(0xFF);
            }
            VarInts.writeUnsignedInt(dataBuf, packetLength);
            dataBuf.writerIndex(lastPacketByte);

            var ri = dataBuf.readerIndex();
            System.out.println("encoding: " + Hex.toHexString(encode(dataBuf)));
            dataBuf.readerIndex(ri);

            if (encryptionEncoder != null) {
                dataBuf = encryptionEncoder.encode(dataBuf);

                ri = dataBuf.readerIndex();
                System.out.println("encrypted: " + Hex.toHexString(encode(dataBuf)));
                dataBuf.readerIndex(ri);
            }

            int segmentCount = (int) Math.ceil(dataBuf.readableBytes() / 10_000f);
            for (int remainingSegements = segmentCount - 1; remainingSegements >= 0; remainingSegements--) {
                int segmentLength = (remainingSegements == 0 ? dataBuf.readableBytes() : 10_000);
                var sendBuf = Unpooled.buffer(segmentLength + 1 + 5);
                sendBuf.writeByte(remainingSegements);
                sendBuf.writeBytes(dataBuf, segmentLength);

                var data = encode(sendBuf);
                System.out.println("final: " + Hex.toHexString(data));
                sctpStream.send(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] encode(ByteBuf buf) {
        byte[] send = new byte[buf.readableBytes()];
        buf.readBytes(send);
        return send;
    }

    private BedrockPacket readPacket(ByteBuf buf) {
        BedrockPacketWrapper wrapper = BedrockPacketWrapper.create();
        packetCodec.decodeHeader(buf, wrapper);
//        System.out.println("sender/target: " + wrapper.getSenderSubClientId() + " " + wrapper.getTargetSubClientId());
        var packet = codec.tryDecode(helper, buf.slice(), wrapper.getPacketId());
        // release it
        wrapper.getHandle().recycle(wrapper);
        return packet;
    }

    public void enableCompression() {
        compressionEnabled = true;
    }

    public void enableEncryption(SecretKey secretKey) {
        encryptionEncoder = new BedrockEncryptionEncoder(secretKey, EncryptionUtils.createCipher(true, true, secretKey));
    }
}
