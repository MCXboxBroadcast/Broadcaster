package com.rtm516.mcxboxbroadcast.core.models;

import java.util.List;

public final class XSTSAuthenticationRequestProperties implements GenericAuthenticationRequestProperties {
    public final List<String> UserTokens;
    public final String DeviceToken;
    public final String TitleToken;
    public final String SandboxId;
    public final JsonJWK ProofKey;

    public XSTSAuthenticationRequestProperties(List<String> UserTokens, String DeviceToken, String TitleToken, String SandboxId, JsonJWK ProofKey) {
        this.UserTokens = UserTokens;
        this.DeviceToken = DeviceToken;
        this.TitleToken = TitleToken;
        this.SandboxId = SandboxId;
        this.ProofKey = ProofKey;
    }
}
