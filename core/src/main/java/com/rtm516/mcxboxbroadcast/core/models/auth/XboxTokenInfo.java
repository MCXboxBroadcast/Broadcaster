package com.rtm516.mcxboxbroadcast.core.models.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record XboxTokenInfo(
    String userXUID,
    String userHash,
    String gamertag,
    String XSTSToken,
    String expiresOn) {

    @JsonIgnore
    public String tokenHeader() {
        return "XBL3.0 x=" + this.userHash + ";" + this.XSTSToken;
    }
}
