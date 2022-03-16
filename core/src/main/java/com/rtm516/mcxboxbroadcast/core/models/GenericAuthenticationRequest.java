package com.rtm516.mcxboxbroadcast.core.models;

import java.util.List;

public class GenericAuthenticationRequest {
    public String RelyingParty;
    public String TokenType;
    public GenericAuthenticationRequestProperties Properties;

    public GenericAuthenticationRequest(String RelyingParty, String TokenType, GenericAuthenticationRequestProperties Properties) {
        this.RelyingParty = RelyingParty;
        this.TokenType = TokenType;
        this.Properties = Properties;
    }

    public static class UserProperties implements GenericAuthenticationRequestProperties {
        public String AuthMethod;
        public String SiteName;
        public String RpsTicket;
        public JsonJWK ProofKey;

        public UserProperties(String AuthMethod, String SiteName, String RpsTicket, JsonJWK ProofKey) {
            this.AuthMethod = AuthMethod;
            this.SiteName = SiteName;
            this.RpsTicket = RpsTicket;
            this.ProofKey = ProofKey;
        }
    }

    public static class DeviceProperties implements GenericAuthenticationRequestProperties {
        public String AuthMethod;
        public String Id;
        public String DeviceType;
        public String SerialNumber;
        public String Version;
        public JsonJWK ProofKey;

        public DeviceProperties(String AuthMethod, String Id, String DeviceType, String SerialNumber, String Version, JsonJWK ProofKey) {
            this.AuthMethod = AuthMethod;
            this.Id = Id;
            this.DeviceType = DeviceType;
            this.SerialNumber = SerialNumber;
            this.Version = Version;
            this.ProofKey = ProofKey;
        }
    }

    public static class TitleProperties implements GenericAuthenticationRequestProperties {
        public String AuthMethod;
        public String DeviceToken;
        public String RpsTicket;
        public String SiteName;
        public JsonJWK ProofKey;

        public TitleProperties(String AuthMethod, String DeviceToken, String RpsTicket, String SiteName, JsonJWK ProofKey) {
            this.AuthMethod = AuthMethod;
            this.DeviceToken = DeviceToken;
            this.RpsTicket = RpsTicket;
            this.SiteName = SiteName;
            this.ProofKey = ProofKey;
        }
    }

    public static class XSTSProperties implements GenericAuthenticationRequestProperties {
        public List<String> UserTokens;
        public String DeviceToken;
        public String TitleToken;
        public String SandboxId;
        public JsonJWK ProofKey;

        public XSTSProperties(List<String> UserTokens, String DeviceToken, String TitleToken, String SandboxId, JsonJWK ProofKey) {
            this.UserTokens = UserTokens;
            this.DeviceToken = DeviceToken;
            this.TitleToken = TitleToken;
            this.SandboxId = SandboxId;
            this.ProofKey = ProofKey;
        }
    }
}
