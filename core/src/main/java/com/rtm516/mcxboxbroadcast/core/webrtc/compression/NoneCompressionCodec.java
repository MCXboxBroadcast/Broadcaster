package com.rtm516.mcxboxbroadcast.core.webrtc.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NoneCompressionCodec implements CompressionCodec {
    @Override
    public ByteBuf encode(ByteBuf msg) {
        return Unpooled.buffer(msg.readableBytes() + 1)
                .writeByte(compressionIdentifier())
                .writeBytes(msg);
    }

    @Override
    public ByteBuf decode(ByteBuf msg) {
        short compIdent = msg.readUnsignedByte();
        if (compIdent != compressionIdentifier()) {
            throw new IllegalArgumentException("Unexpected compression identifier: got " + String.format("0x%02X", compIdent) + ", expected " + String.format("0x%02X", compressionIdentifier()));
        }
        return msg;
    }

    @Override
    public int compressionIdentifier() {
        return 0xFF;
    }
}
