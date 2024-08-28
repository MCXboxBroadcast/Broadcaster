package com.rtm516.mcxboxbroadcast.core.models.auth;

import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;

public record XboxTokenInfo(
    String userXUID,
    String userHash,
    String gamertag,
    String XSTSToken,
    String expiresOn) {

    public XboxTokenInfo(StepXblSisuAuthentication.XblSisuTokens xboxToken) {
        this(xboxToken.getDisplayClaims().get("xid"), xboxToken.getUserHash(), xboxToken.getDisplayClaims().get("gtg"), xboxToken.getToken(), String.valueOf(xboxToken.getExpireTimeMs()));
    }

    public String tokenHeader() {
        return "XBL3.0 x=" + this.userHash + ";" + this.XSTSToken;
    }
}
