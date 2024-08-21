package com.rtm516.mcxboxbroadcast.core.webrtc.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.common.util.Zlib;

public class ZlibCompressionCodec implements CompressionCodec {
    private static final int MAX_DECOMPRESSED_BYTES = Integer.getInteger("bedrock.maxDecompressedBytes", 1024 * 1024 * 10);

    private final Zlib zlib = Zlib.RAW;
    private final int level = 7;

    @Override
    public ByteBuf encode(ByteBuf msg) throws Exception {
        ByteBuf outBuf = Unpooled.buffer(msg.readableBytes() << 3 + 1);
        outBuf.writeByte(compressionIdentifier());

        try {
            zlib.deflate(msg, outBuf, level);
            return outBuf.retain();
        } finally {
            outBuf.release();
        }
    }

    @Override
    public ByteBuf decode(ByteBuf msg) throws Exception {
        if (msg.readUnsignedByte() != compressionIdentifier()) {
            throw new IllegalArgumentException("Unexpected compression identifier");
        }
        return zlib.inflate(msg, MAX_DECOMPRESSED_BYTES);
    }

    @Override
    public int compressionIdentifier() {
        return 0x00;
    }
}
