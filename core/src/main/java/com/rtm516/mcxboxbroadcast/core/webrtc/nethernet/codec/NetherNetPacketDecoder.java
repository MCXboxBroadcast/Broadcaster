package com.rtm516.mcxboxbroadcast.core.webrtc.nethernet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.cloudburstmc.protocol.common.util.VarInts;

import java.util.List;

public class NetherNetPacketDecoder extends ByteToMessageDecoder {
    public static final String NAME = "nethernet-decoder";

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!in.isReadable()) return;

        in.markReaderIndex();
        int length;
        try {
            length = VarInts.readUnsignedInt(in);
        } catch (Exception e) {
            // Not enough bytes for VarInt or invalid
            in.resetReaderIndex();
            return;
        }

        if (in.readableBytes() < length) {
            // Not enough bytes for the full packet
            in.resetReaderIndex();
            return;
        }

        out.add(in.readRetainedSlice(length));
    }
}
