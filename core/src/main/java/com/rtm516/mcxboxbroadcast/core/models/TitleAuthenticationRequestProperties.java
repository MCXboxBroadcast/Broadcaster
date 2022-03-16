package com.rtm516.mcxboxbroadcast.core.models;

public final class TitleAuthenticationRequestProperties implements GenericAuthenticationRequestProperties {
    public final String AuthMethod;
    public final String DeviceToken;
    public final String RpsTicket;
    public final String SiteName;
    public final JsonJWK ProofKey;

    public TitleAuthenticationRequestProperties(String AuthMethod, String DeviceToken, String RpsTicket, String SiteName, JsonJWK ProofKey) {
        this.AuthMethod = AuthMethod;
        this.DeviceToken = DeviceToken;
        this.RpsTicket = RpsTicket;
        this.SiteName = SiteName;
        this.ProofKey = ProofKey;
    }
}
