package com.rtm516.mcxboxbroadcast.core.models;

import com.nimbusds.jose.jwk.ECKey;

public class JsonJWK {
    public String kty;
    public String x;
    public String y;
    public String crv;
    public String alg;
    public String use;

    public JsonJWK(ECKey ecKey) {
        this.kty = ecKey.getKeyType().getValue();
        this.x = ecKey.getX().toString();
        this.y = ecKey.getY().toString();
        this.crv = ecKey.getCurve().getName();
        this.alg = ecKey.getAlgorithm().getName();
        this.use = ecKey.getKeyUse().identifier();
    }
}
