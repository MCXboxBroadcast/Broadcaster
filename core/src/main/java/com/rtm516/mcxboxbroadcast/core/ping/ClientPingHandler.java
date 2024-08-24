package com.rtm516.mcxboxbroadcast.core.ping;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import org.cloudburstmc.netty.channel.raknet.RakPong;
import org.cloudburstmc.protocol.bedrock.BedrockPong;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Licenced under GPL-2.0
 *
 * https://github.com/WaterdogPE/WaterdogPE/blob/4e2b3cd1a3d5e4d3599476fd907b6bd3186783eb/src/main/java/dev/waterdog/waterdogpe/network/connection/codec/client/ClientPingHandler.java
 */
public class ClientPingHandler extends ChannelDuplexHandler {

    private final Promise<BedrockPong> future;

    private final long timeout;
    private final TimeUnit timeUnit;
    private ScheduledFuture<?> timeoutFuture;

    public ClientPingHandler(Promise<BedrockPong> future, long timeout, TimeUnit timeUnit) {
        this.future = future;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    private void onTimeout(Channel channel) {
        channel.close();
        this.future.tryFailure(new TimeoutException());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.timeoutFuture = ctx.channel().eventLoop().schedule(() -> this.onTimeout(ctx.channel()), this.timeout, this.timeUnit);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof RakPong rakPong)) {
            super.channelRead(ctx, msg);
            return;
        }

        if (this.timeoutFuture != null) {
            this.timeoutFuture.cancel(false);
            this.timeoutFuture = null;
        }

        ctx.channel().close();

        this.future.trySuccess(BedrockPong.fromRakNet(rakPong.getPongData()));
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);

        if (this.timeoutFuture != null) {
            this.timeoutFuture.cancel(false);
            this.timeoutFuture = null;
        }
    }
}