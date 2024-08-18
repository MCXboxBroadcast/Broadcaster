package com.rtm516.mcxboxbroadcast.core.models.ws;

import java.math.BigInteger;

public record WsToMessage(int Type, BigInteger To, String Message) {
}
