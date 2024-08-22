package com.rtm516.mcxboxbroadcast.core.ping;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Promise;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.RakPing;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.java_websocket.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Licenced under GPL-2.0
 * Modified to fit MCXboxBroadcast
 *
 * https://github.com/WaterdogPE/WaterdogPE/blob/4e2b3cd1a3d5e4d3599476fd907b6bd3186783eb/src/main/java/dev/waterdog/waterdogpe/network/serverinfo/BedrockServerInfo.java
 */
public class PingUtil {
    private static EventLoopGroup workerEventLoopGroup;

    static {
        workerEventLoopGroup = new NioEventLoopGroup(0, new NamedThreadFactory("MCXboxBroadcast Ping Thread"));
    }

    public static Promise<BedrockPong> ping(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
        EventLoop eventLoop = workerEventLoopGroup.next();
        Promise<BedrockPong> promise = eventLoop.newPromise();

        new Bootstrap()
            .channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
            .group(workerEventLoopGroup)
            .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong())
            .handler(new ClientPingHandler(promise, timeout, timeUnit))
            .bind(0)
            .addListener((ChannelFuture future) -> {
                if (future.cause() != null) {
                    promise.tryFailure(future.cause());
                    future.channel().close();
                } else {
                    RakPing ping = new RakPing(System.currentTimeMillis(), server);
                    future.channel().writeAndFlush(ping).addListener(future1 -> {
                        if (future1.cause() != null) {
                            promise.tryFailure(future1.cause());
                            future.channel().close();
                        }
                    });
                }
            });
        return promise;
    }
}
