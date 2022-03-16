package com.rtm516.mcxboxbroadcast.core.models;

public final class GenericAuthenticationRequest {
    public final String RelyingParty;
    public final String TokenType;
    public final GenericAuthenticationRequestProperties Properties;

    public GenericAuthenticationRequest(String RelyingParty, String TokenType, GenericAuthenticationRequestProperties Properties) {
        this.RelyingParty = RelyingParty;
        this.TokenType = TokenType;
        this.Properties = Properties;
    }
}
