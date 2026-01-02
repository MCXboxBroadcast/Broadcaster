package com.rtm516.mcxboxbroadcast.core.webrtc;

import java.security.KeyPair;
import java.security.PublicKey;
import javax.crypto.SecretKey;

import org.cloudburstmc.protocol.bedrock.data.auth.AuthPayload;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

// Partially from https://github.com/GeyserMC/GlobalLinkServer/blob/master/src/main/java/org/geysermc/globallinkserver/util/Utils.java
public class Utils {
    public static ChainValidationResult.IdentityData validateConnection(MinecraftDataHandler session, AuthPayload authPayload, String clientDataJwt) throws Exception {
        ChainValidationResult result = EncryptionUtils.validatePayload(authPayload);
        if (!result.signed()) {
            throw new IllegalArgumentException("Chain is not signed");
        }
        PublicKey identityPublicKey = result.identityClaims().parsedIdentityPublicKey();

        byte[] clientDataPayload = EncryptionUtils.verifyClientData(clientDataJwt, identityPublicKey);
        if (clientDataPayload == null) {
            throw new IllegalStateException("Client data isn't signed by the given chain data");
        }

        //todo encrypt
//        startEncryptionHandshake(session, identityPublicKey);

        return result.identityClaims().extraData;
    }

    private static void startEncryptionHandshake(MinecraftDataHandler session, PublicKey key) throws Exception {
        KeyPair serverKeyPair = EncryptionUtils.createKeyPair();
        byte[] token = EncryptionUtils.generateRandomToken();

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, token));
        session.sendPacket(packet);

        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), key, token);
        session.enableEncryption(encryptionKey);
    }

    public static int varintSize(int paramInt) {
        if ((paramInt & 0xFFFFFF80) == 0) {
            return 1;
        }
        if ((paramInt & 0xFFFFC000) == 0) {
            return 2;
        }
        if ((paramInt & 0xFFE00000) == 0) {
            return 3;
        }
        if ((paramInt & 0xF0000000) == 0) {
            return 4;
        }
        return 5;
    }
}
