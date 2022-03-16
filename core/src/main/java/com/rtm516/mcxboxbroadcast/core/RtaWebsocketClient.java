package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.internal.LinkedTreeMap;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.util.Map;

public class RtaWebsocketClient extends WebSocketClient {
    private String connectionId;

    public RtaWebsocketClient(String authenticationToken) {
        super(Constants.RTA_WEBSOCKET);
        addHeader("Authorization", authenticationToken);
    }

    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        send("[1,1,\"https://sessiondirectory.xboxlive.com/connections/\"]");
    }

    @Override
    public void onMessage(String message) {
        if (message.contains("ConnectionId")) {
            Object[] parts = Constants.GSON.fromJson(message, Object[].class);
            connectionId = ((LinkedTreeMap<String, String>)parts[4]).get("ConnectionId");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception e) {

    }
}
