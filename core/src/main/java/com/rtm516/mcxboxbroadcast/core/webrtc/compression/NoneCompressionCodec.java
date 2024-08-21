package com.rtm516.mcxboxbroadcast.core.webrtc.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NoneCompressionCodec implements CompressionCodec {
    @Override
    public ByteBuf encode(ByteBuf msg) throws Exception {
        return Unpooled.buffer(msg.readableBytes() + 1)
                .writeByte(compressionIdentifier())
                .writeBytes(msg);
    }

    @Override
    public ByteBuf decode(ByteBuf msg) throws Exception {
        if (msg.readUnsignedByte() != compressionIdentifier()) {
            throw new IllegalArgumentException("Unexpected compression identifier");
        }
        return msg;
    }

    @Override
    public int compressionIdentifier() {
        return 0xFF;
    }
}
