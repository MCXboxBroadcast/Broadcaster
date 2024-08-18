package com.rtm516.mcxboxbroadcast.core.models.ws;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;

public record WsFromMessage(int Type, String From, String Message) {
    public JsonObject message() {
        return Constants.GSON.fromJson(Message, JsonObject.class);
    }
}
