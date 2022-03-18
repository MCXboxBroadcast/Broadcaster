package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.util.Map;

/**
 * Handle the connection and authentication with the RTA websocket
 */
public class RtaWebsocketClient extends WebSocketClient {
    private String connectionId;

    /**
     * Create a new websocket and add the Authorization header
     *
     * @param authenticationToken The token to use for authentication
     */
    public RtaWebsocketClient(String authenticationToken) {
        super(Constants.RTA_WEBSOCKET);
        addHeader("Authorization", authenticationToken);
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
        if (message.contains("ConnectionId")) {
            try {
                Object[] parts = Constants.OBJECT_MAPPER.readValue(message, Object[].class);
                connectionId = ((Map<String, String>) parts[4]).get("ConnectionId");
            } catch (JsonProcessingException ignored) {
            }
        }
    }

    /**
     * @see WebSocketClient#onClose(int, String, boolean) 
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    /**
     * @see WebSocketClient#onError(Exception)
     **/
    @Override
    public void onError(Exception ex) {

    }
}
