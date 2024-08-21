package com.rtm516.mcxboxbroadcast.core.webrtc.compression;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;

public class CompressionHandler {
    private final CompressionCodec chosenCodec;
    private final NoneCompressionCodec fallback;
    private final int threshold;

    public CompressionHandler(PacketCompressionAlgorithm algorithm, int threshold) {
        this.chosenCodec = switch (algorithm) {
            case ZLIB -> new ZlibCompressionCodec();
            case NONE -> new NoneCompressionCodec();
            default -> throw new IllegalArgumentException("Unsupported compression algorithm: " + algorithm);
        };

        if (chosenCodec instanceof NoneCompressionCodec codec) {
            this.fallback = codec;
        } else {
            this.fallback = new NoneCompressionCodec();
        }
        this.threshold = threshold;
    }

    public ByteBuf encode(ByteBuf buf) throws Exception {
        if (buf.readableBytes() > threshold) {
            return chosenCodec.encode(buf);
        }
        return fallback.encode(buf);
    }

    public ByteBuf decode(ByteBuf buf) throws Exception {
        if (buf.readableBytes() > threshold) {
            return chosenCodec.decode(buf);
        }
        return fallback.decode(buf);
    }
}
