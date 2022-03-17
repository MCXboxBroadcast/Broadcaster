package com.rtm516.mcxboxbroadcast.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.rtm516.mcxboxbroadcast.core.models.GenericAuthenticationRequest;
import com.rtm516.mcxboxbroadcast.core.models.GenericAuthenticationResponse;
import com.rtm516.mcxboxbroadcast.core.models.JsonJWK;
import com.rtm516.mcxboxbroadcast.core.models.XboxTokenCache;
import com.rtm516.mcxboxbroadcast.core.models.XboxTokenInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

public class XboxTokenManager {
    private final Path cache;
    private final HttpClient httpClient;
    private final ECKey jwk;
    private final Logger logger;

    public XboxTokenManager(String cache, HttpClient httpClient, Logger logger) {
        this.cache = Paths.get(cache, "xbox_token.json");
        this.httpClient = httpClient;
        this.logger = logger;

        ECKey jwk = null;
        try {
            jwk = new ECKeyGenerator(Curve.P_256)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.ES256)
                .generate();
        } catch (JOSEException e) {
            logger.error("Failed to setup xbox jwk", e);
        }
        this.jwk = jwk;
    }

    public boolean verifyTokens() {
        XboxTokenCache tokenCache = getCache();

        if (tokenCache.userToken == null || tokenCache.xstsToken == null ||
            tokenCache.userToken.NotAfter == null || tokenCache.xstsToken.expiresOn == null) {
            return false;
        }

        long userExpiry = Instant.parse(tokenCache.userToken.NotAfter).toEpochMilli() - Instant.now().toEpochMilli();
        boolean userValid = userExpiry > 1000;

        long xstsExpiry = Instant.parse(tokenCache.xstsToken.expiresOn).toEpochMilli() - Instant.now().toEpochMilli();
        boolean xstsValid = xstsExpiry > 1000;

        return userValid && xstsValid;
    }

    public XboxTokenInfo getCachedXstsToken() {
        return getCache().xstsToken;
    }

    public String getUserToken(String msaToken) {
        try {
            GenericAuthenticationRequest requestContent = new GenericAuthenticationRequest(
                "http://auth.xboxlive.com",
                "JWT",
                new GenericAuthenticationRequest.UserProperties(
                    "RPS",
                    "user.auth.xboxlive.com",
                    "t=" + msaToken,
                    new JsonJWK(jwk.toPublicJWK())
                )
            );

            HttpRequest authRequest = HttpRequest.newBuilder()
                .uri(Constants.USER_AUTHENTICATE_REQUEST)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Constants.OBJECT_MAPPER.writeValueAsString(requestContent)))
                .build();

            GenericAuthenticationResponse tokenResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString()).body(), GenericAuthenticationResponse.class);

            updateCache(new XboxTokenCache(tokenResponse, getCache().xstsToken));

            return tokenResponse.Token;
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to get user authentication token", e);
            return null;
        }
    }

    public String getDeviceToken() {
        try {
            GenericAuthenticationRequest requestContent = new GenericAuthenticationRequest(
                "http://auth.xboxlive.com",
                "JWT",
                new GenericAuthenticationRequest.DeviceProperties(
                    "ProofOfPossession",
                    "{" + UUID.randomUUID() + "}",
                    "Nintendo",
                    "{" + UUID.randomUUID() + "}",
                    "0.0.0",
                    new JsonJWK(jwk.toPublicJWK())
                )
            );

            String requestContentString = Constants.OBJECT_MAPPER.writeValueAsString(requestContent);

            HttpRequest authRequest = HttpRequest.newBuilder()
                .uri(Constants.DEVICE_AUTHENTICATE_REQUEST)
                .header("Content-Type", "application/json")
                .header("Signature", sign(Constants.DEVICE_AUTHENTICATE_REQUEST, "", requestContentString))
                .POST(HttpRequest.BodyPublishers.ofString(requestContentString))
                .build();

            GenericAuthenticationResponse tokenResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString()).body(), GenericAuthenticationResponse.class);

            return tokenResponse.Token;
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to get device authentication token", e);
            return null;
        }
    }

    public String getTitleToken(String msaToken, String deviceToken) {
        try {
            GenericAuthenticationRequest requestContent = new GenericAuthenticationRequest(
                "http://auth.xboxlive.com",
                "JWT",
                new GenericAuthenticationRequest.TitleProperties(
                    "RPS",
                    deviceToken,
                    "t=" + msaToken,
                    "user.auth.xboxlive.com",
                    new JsonJWK(jwk.toPublicJWK())
                )
            );

            String requestContentString = Constants.OBJECT_MAPPER.writeValueAsString(requestContent);

            HttpRequest authRequest = HttpRequest.newBuilder()
                .uri(Constants.TITLE_AUTHENTICATE_REQUEST)
                .header("Content-Type", "application/json")
                .header("Signature", sign(Constants.TITLE_AUTHENTICATE_REQUEST, "", requestContentString))
                .POST(HttpRequest.BodyPublishers.ofString(requestContentString))
                .build();

            GenericAuthenticationResponse tokenResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString()).body(), GenericAuthenticationResponse.class);

            return tokenResponse.Token;
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to get title authentication token", e);
            return null;
        }
    }

    public XboxTokenInfo getXSTSToken(String userToken, String deviceToken, String titleToken) {
        try {
            GenericAuthenticationRequest requestContent = new GenericAuthenticationRequest(
                Constants.RELAYING_PARTY,
                "JWT",
                new GenericAuthenticationRequest.XSTSProperties(
                    Collections.singletonList(userToken),
                    deviceToken,
                    titleToken,
                    "RETAIL",
                    new JsonJWK(jwk.toPublicJWK())
                )
            );

            String requestContentString = Constants.OBJECT_MAPPER.writeValueAsString(requestContent);

            HttpRequest authRequest = HttpRequest.newBuilder()
                .uri(Constants.XSTS_AUTHENTICATE_REQUEST)
                .header("Content-Type", "application/json")
                .header("Signature", sign(Constants.XSTS_AUTHENTICATE_REQUEST, "", requestContentString))
                .POST(HttpRequest.BodyPublishers.ofString(requestContentString))
                .build();

            GenericAuthenticationResponse tokenResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString()).body(), GenericAuthenticationResponse.class);

            XboxTokenInfo xboxTokenInfo = new XboxTokenInfo(
                tokenResponse.DisplayClaims.xui.get(0).xid,
                tokenResponse.DisplayClaims.xui.get(0).uhs,
                tokenResponse.Token,
                tokenResponse.NotAfter);

            updateCache(new XboxTokenCache(getCache().userToken, xboxTokenInfo));

            return xboxTokenInfo;
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to get XSTS authentication token", e);
            return null;
        }
    }

    // From https://github.com/PrismarineJS/prismarine-auth/blob/b2890e09d1767a3ac501c62ea7f094a7f5d4231b/src/TokenManagers/XboxTokenManager.js#L100
    private String sign(URI uri, String authorizationToken, String payload) {
        try {
            // Their backend servers use Windows epoch timestamps, account for that. The server is very picky,
            // bad percision or wrong epoch may fail the request.
            long windowsTimestamp = (((System.currentTimeMillis() / 1000)) + 11644473600L) * 10000000L;
            // Only the /uri?and-query-string
            String pathAndQuery = uri.getPath();

            // Allocate the buffer for signature, TS, path, tokens and payload and NUL termination
            int allocSize = /* sig */ 5 + /* ts */ 9 + /* POST */ 5 + pathAndQuery.length() + 1 + authorizationToken.length() + 1 + payload.length() + 1;
            ByteBuffer buf = ByteBuffer.allocate(allocSize);
            buf.putInt(1); // Policy Version
            buf.put((byte) 0x0);
            buf.putLong(windowsTimestamp);
            buf.put((byte) 0x0);
            buf.put("POST".getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0x0);
            buf.put(pathAndQuery.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0x0);
            buf.put(authorizationToken.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0x0);
            buf.put(payload.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0x0);

            buf.rewind();

            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(jwk.toECPrivateKey());
            signature.update(buf);

            byte[] arrSignature = signature.sign();

            buf = ByteBuffer.allocate(arrSignature.length + 12);
            buf.putInt(1); // Policy Version
            buf.putLong(windowsTimestamp);
            buf.put(arrSignature);

            buf.rewind();
            byte[] arrFinal = new byte[buf.remaining()];
            buf.get(arrFinal);

            return Base64.getEncoder().encodeToString(arrFinal);
        } catch (NoSuchAlgorithmException | JOSEException | InvalidKeyException | SignatureException e) {
            logger.error("Failed to get create signature for message", e);
        }

        return null;
    }

    private XboxTokenCache getCache() {
        try {
            return Constants.OBJECT_MAPPER.readValue(Files.readString(cache), XboxTokenCache.class);
        } catch (IOException e) {
            return new XboxTokenCache();
        }
    }

    private void updateCache(XboxTokenCache updatedCache) {
        try (FileWriter writer = new FileWriter(cache.toString(), StandardCharsets.UTF_8)) {
            Constants.OBJECT_MAPPER.writeValue(writer, updatedCache);
        } catch (IOException e) {
            logger.error("Failed to update xbox token cache", e);
        }
    }
}
