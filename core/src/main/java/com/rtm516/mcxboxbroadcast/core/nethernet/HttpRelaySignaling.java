package com.rtm516.mcxboxbroadcast.core.nethernet;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;
import io.netty.util.concurrent.ScheduledFuture;
import org.cloudburstmc.netty.channel.nethernet.NetherNetConstants;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetHTTPClientSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetXboxRpcSignaling;
import org.cloudburstmc.netty.util.nethernet.IdentityUtils;
import org.jose4j.jwt.JwtClaims;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Relays offers from Xbox signaling to the target server's HTTP signaling and sends back its answer
 *
 * @see <a href="https://mojang.github.io/bedrock-protocol-docs/guides/nether-net-onboarding-guide/">NetherNet Onboarding Guide</a>
 */
public class HttpRelaySignaling extends NetherNetXboxRpcSignaling {
    /**
     * How long to wait after the last candidate before relaying the offer
     */
    private static final long GATHER_QUIET_MS = 500;

    /**
     * The longest to wait for candidates before relaying the offer
     */
    private static final long GATHER_MAX_MS = 3000;

    /**
     * Connect error codes, mapped from the client's disconnect handling
     * The client shows these as InitialConnection-XX
     *
     * @see <a href="https://mojang.github.io/bedrock-protocol-docs/latest/types/disconnect-fail-reason/">DisconnectFailReason</a>
     */
    private static final int ERROR_SERVER_NOT_FOUND = 1; // InitialConnection-81 (NetherNetSessionNotFound)
    private static final int ERROR_CONNECT_REQUEST = 6; // InitialConnection-84 (NetherNetConnectRequest)

    private final InetSocketAddress target;
    private final StorageManager.PlayerHistoryStorage playerHistory;
    private final Logger logger;

    private final Map<String, PendingOffer> pendingOffers = new ConcurrentHashMap<>();

    /**
     * @param localNetworkId The local Network ID to use
     * @param xboxToken The Minecraft Bedrock Session authorization header ('MCToken ***')
     * @param target The server to relay offers to
     * @param playerHistory The player history to record joins in
     * @param logger The logger to use
     */
    public HttpRelaySignaling(long localNetworkId, String xboxToken, InetSocketAddress target, StorageManager.PlayerHistoryStorage playerHistory, Logger logger) {
        super(localNetworkId, xboxToken);
        this.target = target;
        this.playerHistory = playerHistory;
        this.logger = logger;
    }

    @Override
    protected void dispatchSignalToPipeline(String sender, String rawMsg) {
        NetherNetConstants.Signal signal = NetherNetConstants.parseSignal(rawMsg);
        if (signal == null) {
            return;
        }

        switch (signal.type()) {
            case NetherNetConstants.RTC_NEGOTIATION_CONNECT_REQUEST -> {
                logger.debug("Received offer from " + sender + ":\n" + signal.payload());

                PendingOffer previous = pendingOffers.put(signal.connectionId(), new PendingOffer(sender, signal.connectionId(), signal.payload()));
                if (previous != null) {
                    previous.cancel();
                }
            }
            case NetherNetConstants.RTC_NEGOTIATION_CANDIDATE_ADD -> {
                PendingOffer offer = pendingOffers.get(signal.connectionId());
                if (offer == null) {
                    logger.debug("Dropping candidate for " + signal.connectionId() + " as its offer has already been relayed");
                    return;
                }

                offer.addCandidate(signal.payload());
            }
            case NetherNetConstants.RTC_NEGOTIATION_CONNECT_ERROR -> {
                PendingOffer offer = pendingOffers.remove(signal.connectionId());
                if (offer != null) {
                    offer.cancel();
                }
                logger.debug("Client " + sender + " reported connect error: " + signal.payload());
            }
            default -> logger.debug("Unhandled signal from " + sender + ": " + signal.type());
        }
    }

    /**
     * Relay a pending offer to the server
     *
     * @param offer The pending offer
     */
    private void flush(PendingOffer offer) {
        // Skip if the other timer already sent it or the offer was replaced
        if (!pendingOffers.remove(offer.connectionId, offer)) {
            return;
        }
        offer.cancel();

        logger.debug("Relaying offer from " + offer.sender + " with " + offer.candidates.size() + " trickled candidates to " + target);

        // Use the connection ID as the network ID
        NetherNetHTTPClientSignaling http = new NetherNetHTTPClientSignaling(Constants.HTTP_SIGNALING, offer.connectionId);

        // The answer is already a connect response
        http.setSignalHandler(offer.connectionId, signal -> {
            http.close();

            logger.debug("Relaying answer from " + target + " to " + offer.sender);
            try {
                sendSignal(offer.sender, signal);
            } catch (IllegalStateException e) {
                logger.error("Failed to send answer to " + offer.sender, e);
                return;
            }

            recordJoin(offer);
        });
        http.setFailureHandler(reason -> {
            http.close();
            relayFailed(offer, ERROR_CONNECT_REQUEST, reason);
        });

        http.connect(target).whenComplete((iceServers, error) -> {
            if (error != null) {
                http.close();
                relayFailed(offer, ERROR_SERVER_NOT_FOUND, (error.getCause() != null ? error.getCause() : error).getMessage());
                return;
            }

            http.sendDescription(offer.sender, offer.completeSdp());
        });
    }

    private void relayFailed(PendingOffer offer, int errorCode, String reason) {
        logger.error("Failed to relay offer from " + offer.sender + " to " + target + ": " + reason);
        
        try {
            sendSignal(offer.sender, NetherNetConstants.RTC_NEGOTIATION_CONNECT_ERROR + " " + offer.connectionId + " " + errorCode);
        } catch (IllegalStateException e) {
            logger.debug("Failed to send connect error to " + offer.sender + ": " + e.getMessage());
        }
    }

    /**
     * Update the player's last seen time using the identity in their offer
     *
     * @param offer The offer that was relayed
     */
    private void recordJoin(PendingOffer offer) {
        try {
            // Validates the token and that it belongs to this offer
            JwtClaims claims = IdentityUtils.validateSdp(offer.sdp);
            String xuid = claims.getStringClaimValue("xid");
            String gamertag = claims.getStringClaimValue("xname");

            logger.info("Relayed bedrock client " + gamertag + " (" + xuid + ") to target server.");
            playerHistory.lastSeen(xuid, Instant.now());
        } catch (Exception e) {
            logger.debug("Unable to record last seen for " + offer.sender + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        pendingOffers.values().forEach(PendingOffer::cancel);
        pendingOffers.clear();
        super.close();
    }

    private class PendingOffer {
        private final String sender;
        private final String connectionId;
        private final String sdp;
        private final List<String> candidates = new ArrayList<>();

        private final ScheduledFuture<?> maxTimer;
        private ScheduledFuture<?> quietTimer;

        private PendingOffer(String sender, String connectionId, String sdp) {
            this.sender = sender;
            this.connectionId = connectionId;
            this.sdp = sdp;

            this.maxTimer = eventLoopGroup.schedule(() -> flush(this), GATHER_MAX_MS, TimeUnit.MILLISECONDS);
            restartQuietTimer();
        }

        private void addCandidate(String candidate) {
            candidates.add(candidate);
            restartQuietTimer();
        }

        private void restartQuietTimer() {
            if (quietTimer != null) {
                quietTimer.cancel(false);
            }
            quietTimer = eventLoopGroup.schedule(() -> flush(this), GATHER_QUIET_MS, TimeUnit.MILLISECONDS);
        }

        private void cancel() {
            maxTimer.cancel(false);
            quietTimer.cancel(false);
        }

        /**
         * Add the trickled candidates to the offer, as HTTP signaling needs them all up front
         *
         * @return The offer with all of its candidates
         */
        private String completeSdp() {
            StringBuilder out = new StringBuilder(sdp.stripTrailing()).append("\r\n");
            for (String candidate : candidates) {
                candidate = candidate.trim();
                if (!candidate.isEmpty()) {
                    out.append(candidate.startsWith("a=") ? candidate : "a=" + candidate).append("\r\n");
                }
            }
            return out.toString();
        }
    }
}
