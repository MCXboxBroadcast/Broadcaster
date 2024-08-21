package com.rtm516.mcxboxbroadcast.core.webrtc.compression;

import io.netty.buffer.ByteBuf;

public interface CompressionCodec {
    ByteBuf encode(ByteBuf msg) throws Exception;

    ByteBuf decode(ByteBuf msg) throws Exception;

    int compressionIdentifier();
}
