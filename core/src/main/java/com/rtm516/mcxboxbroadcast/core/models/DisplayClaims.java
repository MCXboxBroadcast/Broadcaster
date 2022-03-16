package com.rtm516.mcxboxbroadcast.core.models;

import java.util.List;

public final class DisplayClaims {
    private final List<Xui> xui;

    public DisplayClaims(List<Xui> xui) {
        this.xui = xui;
    }

    public List<Xui> xui() {
        return xui;
    }
}
