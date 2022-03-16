package com.rtm516.mcxboxbroadcast.core.models;

public final class Xui {
    private final String uhs;
    private final String xid;
    private final String gtg;

    public Xui(String uhs, String xid, String gtg) {
        this.uhs = uhs;
        this.xid = xid;
        this.gtg = gtg;
    }

    public String uhs() {
        return uhs;
    }

    public String xid() {
        return xid;
    }

    public String gtg() {
        return gtg;
    }
}
