package com.rtm516.mcxboxbroadcast.core.models;

import com.nimbusds.jose.jwk.ECKey;

public final class JsonJWK {
    public final String kty;
    public final String x;
    public final String y;
    public final String crv;
    public final String alg;
    public final String use;

    public JsonJWK(ECKey ecKey) {
        this.kty = ecKey.getKeyType().getValue();
        this.x = ecKey.getX().toString();
        this.y = ecKey.getY().toString();
        this.crv = ecKey.getCurve().getName();
        this.alg = ecKey.getAlgorithm().getName();
        this.use = ecKey.getKeyUse().identifier();
    }
}
