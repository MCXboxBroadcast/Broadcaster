package com.rtm516.mcxboxbroadcast.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class XboxTokenInfo {
    public String userXUID;
    public String userHash;
    public String XSTSToken;
    public String expiresOn;

    public XboxTokenInfo() {
    }

    public XboxTokenInfo(String userXUID, String userHash, String XSTSToken, String expiresOn) {
        this.userXUID = userXUID;
        this.userHash = userHash;
        this.XSTSToken = XSTSToken;
        this.expiresOn = expiresOn;
    }

    @JsonIgnore
    public String tokenHeader() {
        return "XBL3.0 x=" + this.userHash + ";" + this.XSTSToken;
    }
}
