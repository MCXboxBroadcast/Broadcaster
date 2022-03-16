package com.rtm516.mcxboxbroadcast.core.models;

public final class DeviceAuthenticationRequestProperties implements GenericAuthenticationRequestProperties {
    public final String AuthMethod;
    public final String Id;
    public final String DeviceType;
    public final String SerialNumber;
    public final String Version;
    public final JsonJWK ProofKey;

    public DeviceAuthenticationRequestProperties(String AuthMethod, String Id, String DeviceType, String SerialNumber, String Version, JsonJWK ProofKey) {
        this.AuthMethod = AuthMethod;
        this.Id = Id;
        this.DeviceType = DeviceType;
        this.SerialNumber = SerialNumber;
        this.Version = Version;
        this.ProofKey = ProofKey;
    }
}
