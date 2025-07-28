package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;
import com.rtm516.mcxboxbroadcast.core.webrtc.Utils;
import com.rtm516.mcxboxbroadcast.core.webrtc.bedrock.RedirectPacketHandler;
import com.rtm516.mcxboxbroadcast.core.webrtc.compression.CompressionHandler;
import com.rtm516.mcxboxbroadcast.core.webrtc.encryption.BedrockEncryptionEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec_v3;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.util.VarInts;
import pe.pi.sctp4j.sctp.SCTPByteStreamListener;
import pe.pi.sctp4j.sctp.SCTPStream;

import javax.crypto.SecretKey;

public class TesterMinecraftDataHandler implements SCTPByteStreamListener {
    private final BedrockPacketCodec packetCodec = new BedrockPacketCodec_v3();
    private final SCTPStream sctpStream;
    private final BedrockCodec codec;
    private final BedrockCodecHelper helper;
    private final Logger logger;
    private final BedrockPacketHandler packetHandler;

    private CompressionHandler compressionHandler;
    private BedrockEncryptionEncoder encryptionEncoder;

    private ByteBuf concat;
    private int expectedLength;

    public TesterMinecraftDataHandler(SCTPStream sctpStream, BedrockCodec codec, Logger logger) {
        this.sctpStream = sctpStream;
        this.codec = codec;
        this.helper = codec.createHelper();
        this.logger = logger.prefixed("TesterMinecraftDataHandler");
        this.packetHandler = new TesterPacketHandler(this, logger);
    }

    @Override
    public void onMessage(SCTPStream sctpStream, byte[] bytes) {
        try {
            if (bytes.length == 0) {
                throw new IllegalStateException("Expected at least 2 bytes");
            }
            // TODO Only do this if segmentcount > 0
            ByteBuf buf = Unpooled.buffer(bytes.length);
            buf.writeBytes(bytes);

            byte remainingSegments = buf.readByte();

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

            if (compressionHandler != null) {
                buf = compressionHandler.decode(buf);
            }

            expectedLength = VarInts.readUnsignedInt(buf);
            // TODO Implement this check
//            if (buf.readableBytes() != expectedLength) {
//                System.out.println("expected " + expectedLength + " bytes but got " + buf.readableBytes());
//                DisconnectPacket disconnect = new DisconnectPacket();
//                disconnect.setReason(DisconnectFailReason.BAD_PACKET);
//                disconnect.setKickMessage("");
//                sendPacket(disconnect);
//                return;
//            }

            BedrockPacket packet = readPacket(buf);

            if (!(packet instanceof LoginPacket)) {
                logger.debug("S -> C: " + packet);
            } else {
                // Don't log the contents of the login packet
                logger.debug("S -> C: LoginPacket");
            }

            packet.handle(packetHandler);
        } catch (Exception e) {
            logger.error("Failed to handle packet from NetherNet", e);
        }
    }

    @Override
    public void onMessage(SCTPStream sctpStream, String s) {
    }

    @Override
    public void close(SCTPStream sctpStream) {
    }

    public void sendPacket(BedrockPacket packet) {
        logger.debug("C -> S: " + packet);
        try {
            ByteBuf dataBuf = Unpooled.buffer(128);
            int shiftedBytes = 5; // leave enough room for data length
            dataBuf.writerIndex(shiftedBytes);

            int packetId = codec.getPacketDefinition(packet.getClass()).getId();
            packetCodec.encodeHeader(
                    dataBuf,
                    BedrockPacketWrapper.create(packetId, 0, 0, null, null)
            );
            codec.tryEncode(helper, dataBuf, packet);

            int lastPacketByte = dataBuf.writerIndex();
            dataBuf.readerIndex(shiftedBytes);

            int packetLength = lastPacketByte - shiftedBytes;
            // read from the first actual byte
            dataBuf.readerIndex(5 - Utils.varintSize(packetLength));
            dataBuf.writerIndex(dataBuf.readerIndex());

            VarInts.writeUnsignedInt(dataBuf, packetLength);
            dataBuf.writerIndex(lastPacketByte);

            if (compressionHandler != null) {
                dataBuf = compressionHandler.encode(dataBuf);
            }

            int ri = dataBuf.readerIndex();
            dataBuf.readerIndex(ri);

            if (encryptionEncoder != null) {
                dataBuf = encryptionEncoder.encode(dataBuf);

                ri = dataBuf.readerIndex();
                dataBuf.readerIndex(ri);
            }

            int segmentCount = (int) Math.ceil(dataBuf.readableBytes() / 10_000f);
            for (int remainingSegements = segmentCount - 1; remainingSegements >= 0; remainingSegements--) {
                int segmentLength = (remainingSegements == 0 ? dataBuf.readableBytes() : 10_000);
                ByteBuf sendBuf = Unpooled.buffer(segmentLength + 1 + 5);
                sendBuf.writeByte(remainingSegements);
                sendBuf.writeBytes(dataBuf, segmentLength);

                byte[] data = encode(sendBuf);
                sctpStream.send(data);
            }
        } catch (Exception e) {
            logger.error("Failed to send packet to NetherNet", e);
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
        BedrockPacket packet = codec.tryDecode(helper, buf.slice(), wrapper.getPacketId());
        // release it
        wrapper.getHandle().recycle(wrapper);
        return packet;
    }

    public void enableCompression(PacketCompressionAlgorithm compressionAlgorithm, int threshold) {
        this.compressionHandler = new CompressionHandler(compressionAlgorithm, threshold);
    }

    public void enableEncryption(SecretKey secretKey) {
        encryptionEncoder = new BedrockEncryptionEncoder(secretKey, EncryptionUtils.createCipher(true, true, secretKey));
    }
}
