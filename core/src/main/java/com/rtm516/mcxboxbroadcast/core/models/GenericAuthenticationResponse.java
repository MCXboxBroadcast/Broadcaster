package com.rtm516.mcxboxbroadcast.core.models;

public final class GenericAuthenticationResponse {
    private final String IssueInstant;
    private final String NotAfter;
    private final String Token;
    private final DisplayClaims DisplayClaims;

    public GenericAuthenticationResponse(String IssueInstant, String NotAfter, String Token, DisplayClaims DisplayClaims) {
        this.IssueInstant = IssueInstant;
        this.NotAfter = NotAfter;
        this.Token = Token;
        this.DisplayClaims = DisplayClaims;
    }

    public String IssueInstant() {
        return IssueInstant;
    }

    public String NotAfter() {
        return NotAfter;
    }

    public String Token() {
        return Token;
    }

    public DisplayClaims DisplayClaims() {
        return DisplayClaims;
    }
}
