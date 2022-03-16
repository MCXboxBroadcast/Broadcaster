package com.rtm516.mcxboxbroadcast.core.models;

import java.util.Objects;

public final class XboxTokenInfo {
    private final String userXUID;
    private final String userHash;
    private final String XSTSToken;
    private final String expiresOn;

    public XboxTokenInfo(String userXUID, String userHash, String XSTSToken, String expiresOn) {
        this.userXUID = userXUID;
        this.userHash = userHash;
        this.XSTSToken = XSTSToken;
        this.expiresOn = expiresOn;
    }

    public String tokenHeader() {
        return "XBL3.0 x=" + this.userHash() + ";" + this.XSTSToken();
    }

    public String userXUID() {
        return userXUID;
    }

    public String userHash() {
        return userHash;
    }

    public String XSTSToken() {
        return XSTSToken;
    }

    public String expiresOn() {
        return expiresOn;
    }
}
