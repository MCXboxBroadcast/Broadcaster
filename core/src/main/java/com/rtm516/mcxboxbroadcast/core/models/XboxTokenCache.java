package com.rtm516.mcxboxbroadcast.core.models;

public final class XboxTokenCache {
    private final GenericAuthenticationResponse userToken;
    private final XboxTokenInfo xstsToken;

    public XboxTokenCache(GenericAuthenticationResponse userToken, XboxTokenInfo xstsToken) {
        this.userToken = userToken;
        this.xstsToken = xstsToken;
    }

    public GenericAuthenticationResponse userToken() {
        return userToken;
    }

    public XboxTokenInfo xstsToken() {
        return xstsToken;
    }
}