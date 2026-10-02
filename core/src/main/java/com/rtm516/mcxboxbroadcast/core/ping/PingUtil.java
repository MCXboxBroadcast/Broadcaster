package com.rtm516.mcxboxbroadcast.core.ping;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;
import org.cloudburstmc.netty.channel.nethernet.signaling.HttpSignalingSettings;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPClientSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.PongData;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PingUtil {
    private static boolean webPingEnabled = false;

    /**
     * Get the server status from its NetherNet HTTP signaling
     *
     * @param server The server to ping
     * @param timeout The time to wait for a response
     * @param timeUnit The unit of the timeout
     * @return The server status
     */
    public static CompletableFuture<PongData> ping(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
        CompletableFuture<PongData> ping = NetherNetHTTPClientSignaling.probe(server, HttpSignalingSettings.DEFAULT)
            .orTimeout(timeout, timeUnit)
            .thenApply(NetherNetHTTPClientSignaling.Probe::motd);

        if (!webPingEnabled) {
            return ping;
        }

        return ping.exceptionallyCompose(e -> webPing(server, timeout, timeUnit));
    }

    private static CompletableFuture<PongData> webPing(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
        CompletableFuture<PongData> future = new CompletableFuture<>();

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

            // The pong is the same status document the server returns from /v1/join
            PongData pong = PongData.fromJson(data.getAsJsonObject("ping").get("pong").toString());
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
