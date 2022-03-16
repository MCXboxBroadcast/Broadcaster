package com.rtm516.mcxboxbroadcast.core.models;

import java.util.List;

public class GenericAuthenticationResponse {
    public String IssueInstant;
    public String NotAfter;
    public String Token;
    public DisplayClaims DisplayClaims;

    public static class DisplayClaims {
        public List<Xui> xui;
        public Xdi xdi;
        public Xti xti;
    }

    public static class Xui {
        public String gtg;
        public String xid;
        public String uhs;
        public String usr;
        public String prv;
        public String agg;
    }

    public static class Xdi {
        public String did;
        public String dcs;
    }

    public static class Xti {
        public String tid;
    }
}
