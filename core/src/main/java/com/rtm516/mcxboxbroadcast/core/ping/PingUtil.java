package com.rtm516.mcxboxbroadcast.core.ping;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PingUtil {
    private static final EventLoopGroup workerEventLoopGroup;
    private static boolean webPingEnabled = false;

    static {
        workerEventLoopGroup = new NioEventLoopGroup(0, new NamedThreadFactory("MCXboxBroadcast Ping Thread"));
    }

    public static Promise<BedrockPong> ping(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
        EventLoop eventLoop = workerEventLoopGroup.next();
        Promise<BedrockPong> promise = eventLoop.newPromise();

        raknetPing(server, timeout, timeUnit).addListener(future -> {
            if (future.isSuccess()) {
                promise.trySuccess((BedrockPong) future.get());
            } else if (webPingEnabled) {
                webPing(server, timeout, timeUnit).thenAccept(promise::trySuccess).exceptionally(e -> {
                    promise.tryFailure(e);
                    return null;
                });
            } else {
                promise.tryFailure(future.cause());
            }
        });

        return promise;
    }

    /**
     * Licenced under GPL-2.0
     * Modified to fit MCXboxBroadcast
     *
     * https://github.com/WaterdogPE/WaterdogPE/blob/4e2b3cd1a3d5e4d3599476fd907b6bd3186783eb/src/main/java/dev/waterdog/waterdogpe/network/serverinfo/BedrockServerInfo.java
     */
    private static Promise<BedrockPong> raknetPing(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
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

    private static CompletableFuture<BedrockPong> webPing(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
        CompletableFuture<BedrockPong> future = new CompletableFuture<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://checker.geysermc.org/ping?hostname=" + server.getHostString() + "&port=" + server.getPort()))
            .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).orTimeout(timeout, timeUnit).thenAccept(response -> {
            if (response.statusCode() != 200) {
                future.completeExceptionally(new Exception("WebAPI: Failed to ping server"));
                return;
            }

            // Parse response and check it succeeded
            JsonObject data = Constants.GSON.fromJson(response.body(), JsonObject.class);
            if (!data.get("success").getAsBoolean()) {
                future.completeExceptionally(new Exception("WebAPI: Server is offline"));
                return;
            }

            // Parse the pong data, the checker api uses this class on the backend so we can directly parse it
            BedrockPong pong = Constants.GSON.fromJson(data.getAsJsonObject("ping").get("pong"), BedrockPong.class);
            future.complete(pong);
        }).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });


        return future;
    }

    public static void setWebPingEnabled(boolean enabled) {
        PingUtil.webPingEnabled = enabled;
    }
}
