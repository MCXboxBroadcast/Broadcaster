package com.rtm516.mcxboxbroadcast.core.models;

public record XboxTokenCache (XboxTokenInfo xstsToken) {
    public XboxTokenCache() {
        this(null);
    }
}