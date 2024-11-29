package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.models.ws.MessageType;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.util.Map;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtaWebsocketClient extends WebSocketClient {
    private final SessionManagerCore sessionManager;

    private String connectionId;
    private final Logger logger;
    private final String xuid;
    private boolean firstConnectionId = true;

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
    @Override
    public void onMessage(String message) {
        if (message.contains("ConnectionId") && firstConnectionId) {
            Object[] parts = Constants.GSON.fromJson(message, Object[].class);
            connectionId = ((Map<String, String>) parts[4]).get("ConnectionId");
            firstConnectionId = false;

            // Let xbox know we want friend updates
            send("[1,2,\"https://social.xboxlive.com/users/xuid(" + this.xuid + ")/friends\"]");
        } else {
            // [Type, SequenceId, ...]
            // Subscribe: [type, sequenceId, status, subscriptionId, data]
            // Unsubscribe: [type, sequenceId, status]
            // Event: [type, sequenceId, data]

            // Handle the message
            Object[] parts = Constants.GSON.fromJson(message, Object[].class);
            MessageType type = MessageType.fromValue(((Double) parts[0]).intValue());
            switch (type) {
                case Subscribe:
                    logger.debug("RTA Websocket subscribed message: " + message);
                    break;
                case Unsubscribe:
                    logger.debug("RTA Websocket unsubscribed message: " + message);
                    break;
                case Event:
                    logger.debug("RTA Websocket event message: " + message);
                    // Get data json object
                    Map<String, Object> data = (Map<String, Object>) parts[2];
                    if (data.getOrDefault("NotificationType", "").equals("IncomingFriendRequestCountChanged")) {
                        logger.debug("RTA Websocket friend request message: " + message);
                        sessionManager.friendManager().acceptPendingFriendRequests();
                    }
                    break;
                case Resync:
                    logger.debug("RTA Websocket resync message: " + message);
                    break;
                default:
                    logger.debug("RTA Websocket unknown message: " + message);
                    break;
            }
        }
    }

    /**
     * @see WebSocketClient#onClose(int, String, boolean) 
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.debug("RTA Websocket disconnected: " + reason + " (" + code + ")");
    }

    /**
     * @see WebSocketClient#onError(Exception)
     **/
    @Override
    public void onError(Exception ex) {
        logger.debug("RTA Websocket error: " + ex.getMessage());
    }
}
