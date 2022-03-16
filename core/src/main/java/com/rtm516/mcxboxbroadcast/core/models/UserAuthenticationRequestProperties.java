package com.rtm516.mcxboxbroadcast.core.models;

public final class UserAuthenticationRequestProperties implements GenericAuthenticationRequestProperties {
    public final String AuthMethod;
    public final String SiteName;
    public final String RpsTicket;
    public final JsonJWK ProofKey;

    public UserAuthenticationRequestProperties(String AuthMethod, String SiteName, String RpsTicket, JsonJWK ProofKey) {
        this.AuthMethod = AuthMethod;
        this.SiteName = SiteName;
        this.RpsTicket = RpsTicket;
        this.ProofKey = ProofKey;
    }
}
