package com.rtm516.mcxboxbroadcast.core.webrtc;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import javax.crypto.SecretKey;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

// https://github.com/GeyserMC/GlobalLinkServer/blob/master/src/main/java/org/geysermc/globallinkserver/util/Utils.java
public class Utils {
    public static ChainValidationResult.IdentityData validateAndEncryptConnection(MinecraftDataHandler session, List<String> certChainData, String clientDataJwt) throws Exception {
        ChainValidationResult result = EncryptionUtils.validateChain(certChainData);
        if (!result.signed()) {
            throw new IllegalArgumentException("Chain is not signed");
        }
        PublicKey identityPublicKey = result.identityClaims().parsedIdentityPublicKey();

        byte[] clientDataPayload = EncryptionUtils.verifyClientData(clientDataJwt, identityPublicKey);
        if (clientDataPayload == null) {
            throw new IllegalStateException("Client data isn't signed by the given chain data");
        }

        startEncryptionHandshake(session, identityPublicKey);

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
}
