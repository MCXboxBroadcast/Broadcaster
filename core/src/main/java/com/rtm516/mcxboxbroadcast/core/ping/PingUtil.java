package com.rtm516.mcxboxbroadcast.core.ping;

import com.rtm516.mcxboxbroadcast.core.Constants;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPClientSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.PongData;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PingUtil {
    /**
     * Get the server status from its NetherNet HTTP signaling
     *
     * @param server The server to ping
     * @param timeout The time to wait for a response
     * @param timeUnit The unit of the timeout
     * @return The server status
     */
    public static CompletableFuture<PongData> ping(InetSocketAddress server, long timeout, TimeUnit timeUnit) {
        return NetherNetHTTPClientSignaling.probe(server, Constants.HTTP_SIGNALING, Long.toUnsignedString(ThreadLocalRandom.current().nextLong()))
            .orTimeout(timeout, timeUnit)
            .thenApply(NetherNetHTTPClientSignaling.Probe::motd);
    }
}
