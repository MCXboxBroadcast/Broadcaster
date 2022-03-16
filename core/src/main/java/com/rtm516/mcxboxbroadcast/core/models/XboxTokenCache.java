package com.rtm516.mcxboxbroadcast.core.models;

public class XboxTokenCache {
    public GenericAuthenticationResponse userToken;
    public XboxTokenInfo xstsToken;

    public XboxTokenCache() {
    }

    public XboxTokenCache(GenericAuthenticationResponse userToken, XboxTokenInfo xstsToken) {
        this.userToken = userToken;
        this.xstsToken = xstsToken;
    }
}