package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.models.ws.MessageType;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtaWebsocketClient extends WebSocketClient {
    private final SessionManagerCore sessionManager;

    private String connectionId;
    private final Logger logger;
    private final String xuid;
    private boolean isFirstConnection = true;
    private CompletableFuture<String> connectionIdFuture = new CompletableFuture<>();

    /**
     * Create a new websocket and add the Authorization header
     *
     * @param sessionManager The session manager this websocket is for
     */
    public RtaWebsocketClient(SessionManagerCore sessionManager) {
        super(Constants.RTA_WEBSOCKET);
        addHeader("Authorization", sessionManager.getTokenHeader());
        this.sessionManager = sessionManager;
        this.xuid = sessionManager.userXUID();
        this.logger = sessionManager.logger();
    }

    /**
     * A helper method to get the stored connection ID
     *
     * @return The stored connection ID
     */
    public String getConnectionId() {
        return connectionId;
    }

    public CompletableFuture<String> getConnectionIdFuture() {
        return connectionIdFuture;
    }

    /**
     * When the web socket connects send the request for the connection ID
     *
     * @see WebSocketClient#onOpen(ServerHandshake)
     *
     * @param serverHandshake The handshake of the websocket instance
     */
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        send("[1,1,\"https://sessiondirectory.xboxlive.com/connections/\"]");
    }

    /**
     * When we get a message check if it's a connection ID message
     * and handle otherwise ignore it
     *
     * @see WebSocketClient#onMessage(String)
     *
     * @param message The UTF-8 decoded message that was received.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(String message) {
        // [Type, SequenceId, ...]
        // Subscribe: [type, sequenceId, status, subscriptionId, data]
        // Unsubscribe: [type, sequenceId, status]
        // Event: [type, sequenceId, data]

        // Handle the message
        Object[] parts = Constants.GSON.fromJson(message, Object[].class);
        MessageType type = MessageType.fromValue(((Double) parts[0]).intValue());
        switch (type) {
            case Subscribe:
                logger.debug("RTA Websocket [" + connectionId + "] subscribed: " + message);
                if (message.contains("ConnectionId") && isFirstConnection) {
                    connectionId = ((Map<String, String>) parts[4]).get("ConnectionId");
                    connectionIdFuture.complete(connectionId);
                    isFirstConnection = false;

                    // Let xbox know we want friend updates
                    send("[1,2,\"https://social.xboxlive.com/users/xuid(" + this.xuid + ")/friends\"]");
                }
                break;
            case Unsubscribe:
                logger.debug("RTA Websocket [" + connectionId + "] unsubscribed: " + message);
                break;
            case Event:
                logger.debug("RTA Websocket [" + connectionId + "] event: " + message);
                // Get data json object
                Map<String, Object> data = (Map<String, Object>) parts[2];
                if (data.getOrDefault("NotificationType", "").equals("IncomingFriendRequestCountChanged")) {
                    logger.debug("RTA Websocket [" + connectionId + "] friend request: " + message);
                    sessionManager.friendManager().acceptPendingFriendRequests();
                }
                break;
            case Resync:
                logger.debug("RTA Websocket [" + connectionId + "] resync: " + message);
                break;
            default:
                logger.debug("RTA Websocket [" + connectionId + "] unknown: " + message);
                break;
        }
    }

    /**
     * @see WebSocketClient#onClose(int, String, boolean)
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (!connectionIdFuture.isDone()) {
            connectionIdFuture.completeExceptionally(new Exception("RTA Websocket [" + connectionId + "] disconnected before connectionId was received"));
        }

        String reasonString = reason.isEmpty() && code == 1000 ? "Normal close" : reason;
        logger.debug("RTA Websocket [" + connectionId + "] disconnected: " + reasonString + " (" + code + ")");
    }

    /**
     * @see WebSocketClient#onError(Exception)
     **/
    @Override
    public void onError(Exception ex) {
        logger.error("RTA Websocket [" + connectionId + "] error: " + ex.getMessage(), ex);
    }
}
