package com.rtm516.mcxboxbroadcast.core.models.auth;

public record XboxTokenCache (XboxTokenInfo xstsToken) {
    public XboxTokenCache() {
        this(null);
    }
}