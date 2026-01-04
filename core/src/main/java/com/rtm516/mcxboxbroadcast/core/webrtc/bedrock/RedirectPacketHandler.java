package com.rtm516.mcxboxbroadcast.core.webrtc.bedrock;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;

import java.io.IOException;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.data.ChatRestrictionLevel;
import org.cloudburstmc.protocol.bedrock.data.EduSharedUriResource;
import org.cloudburstmc.protocol.bedrock.data.GamePublishSetting;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.SpawnBiomeType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;

public class RedirectPacketHandler implements BedrockPacketHandler {

    private final BedrockServerSession session;
    private final SessionInfo sessionInfo;
    private final SessionManagerCore sessionManager;

    private ChainValidationResult.IdentityData identityData;
    private boolean networkSettingsRequested = false;
    private final Logger logger;

    public RedirectPacketHandler(BedrockServerSession session, SessionInfo sessionInfo, SessionManagerCore sessionManager, Logger logger) {
        this.session = session;
        this.sessionInfo = sessionInfo;
        this.sessionManager = sessionManager;
        this.logger = logger;
    }

    private void disconnect(String message) {
        if (message == null) {
            session.disconnect();
        } else {
            session.disconnect(message);
        }
    }

    private void disconnect() {
        disconnect(null);
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        BedrockPacketHandler.super.handlePacket(packet);
        return PacketSignal.HANDLED; 
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        int protocolVersion = packet.getProtocolVersion();
        BedrockCodec codec = BedrockCodecProvider.getCodec(protocolVersion);

        if (codec == null) {
            // we try the latest version available
            logger.warn("Unsupported Bedrock protocol version: " + protocolVersion + ". Falling back to latest supported version.");
            codec = BedrockCodecProvider.getLatestCodec();
        }

        session.setCodec(codec);

        NetworkSettingsPacket networkSettingsPacket = new NetworkSettingsPacket();
        networkSettingsPacket.setCompressionThreshold(0);
        networkSettingsPacket.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);

        session.sendPacketImmediately(networkSettingsPacket);
        session.setCompression(PacketCompressionAlgorithm.ZLIB);

        networkSettingsRequested = true;
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        if (!networkSettingsRequested) {
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
            session.sendPacket(statusPacket);

            disconnect();
            return PacketSignal.HANDLED;
        }

        PlayStatusPacket status = new PlayStatusPacket();
        status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.sendPacket(status);

        ResourcePacksInfoPacket info = new ResourcePacksInfoPacket();
        info.setWorldTemplateId(UUID.randomUUID());
        info.setWorldTemplateVersion("*");
        info.setVibrantVisualsForceDisabled(true);
        info.setForcedToAccept(false);
        session.sendPacket(info);

        try {
            ChainValidationResult result = EncryptionUtils.validatePayload(packet.getAuthPayload());
            if (!result.signed()) {
                throw new IllegalArgumentException("Chain is not signed");
            }
            PublicKey identityPublicKey = result.identityClaims().parsedIdentityPublicKey();

            byte[] clientDataPayload = EncryptionUtils.verifyClientData(packet.getClientJwt(), identityPublicKey);
            if (clientDataPayload == null) {
                throw new IllegalStateException("Client data isn't signed by the given chain data");
            }

            identityData = result.identityClaims().extraData;
        } catch (AssertionError | Exception error) {
            disconnect("disconnect.loginFailed");
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        switch (packet.getStatus()) {
            case COMPLETED:
                sendStartGame();
                break;
            case HAVE_ALL_PACKS:
                ResourcePackStackPacket stack = new ResourcePackStackPacket();
                stack.setExperimentsPreviouslyToggled(false);
                stack.setForcedToAccept(false);
                stack.setGameVersion("*");
                session.sendPacket(stack);
                break;
            default:
                disconnect("disconnectionScreen.resourcePack");
                break;
        }
        return PacketSignal.HANDLED;
    }

    @SuppressWarnings("deprecation")
    public void sendStartGame() {
        StartGamePacket startGamePacket = new StartGamePacket();
        startGamePacket.setUniqueEntityId(1);
        startGamePacket.setRuntimeEntityId(1);
        startGamePacket.setPlayerGameType(GameType.CREATIVE);
        startGamePacket.setPlayerPosition(Vector3f.from(0, 64 + 2, 0));
        startGamePacket.setRotation(Vector2f.ONE);
        startGamePacket.setPlayerPropertyData(NbtMap.EMPTY);

        startGamePacket.setSeed(0L);
        startGamePacket.setDimensionId(2);
        startGamePacket.setGeneratorId(1);
        startGamePacket.setSpawnBiomeType(SpawnBiomeType.DEFAULT);
        startGamePacket.setCustomBiomeName("");
        startGamePacket.setForceExperimentalGameplay(OptionalBoolean.empty());
        startGamePacket.setLevelGameType(GameType.CREATIVE);
        startGamePacket.setDifficulty(0);
        startGamePacket.setDefaultSpawn(Vector3i.ZERO);
        startGamePacket.setAchievementsDisabled(true);
        startGamePacket.setCurrentTick(-1);
        startGamePacket.setEduEditionOffers(0);
        startGamePacket.setEduFeaturesEnabled(false);
        startGamePacket.setEducationProductionId("");
        startGamePacket.setEduSharedUriResource(EduSharedUriResource.EMPTY);
        startGamePacket.setRainLevel(0);
        startGamePacket.setLightningLevel(0);
        startGamePacket.setMultiplayerGame(true);
        startGamePacket.setBroadcastingToLan(true);
        startGamePacket.getGamerules().add(new GameRuleData<>("showcoordinates", false));
        startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setCommandsEnabled(true);
        startGamePacket.setChatRestrictionLevel(ChatRestrictionLevel.NONE);
        startGamePacket.setTexturePacksRequired(false);
        startGamePacket.setBonusChestEnabled(false);
        startGamePacket.setStartingWithMap(false);
        startGamePacket.setTrustingPlayers(true);
        startGamePacket.setDefaultPlayerPermission(PlayerPermission.VISITOR);
        startGamePacket.setServerChunkTickRange(4);
        startGamePacket.setBehaviorPackLocked(false);
        startGamePacket.setResourcePackLocked(false);
        startGamePacket.setFromLockedWorldTemplate(false);
        startGamePacket.setUsingMsaGamertagsOnly(false);
        startGamePacket.setFromWorldTemplate(false);
        startGamePacket.setWorldTemplateOptionLocked(false);

        startGamePacket.setServerEngine("");
        startGamePacket.setLevelId("");
        startGamePacket.setLevelName("MCXboxBroadcast");
        startGamePacket.setPremiumWorldTemplateId("");
        startGamePacket.setWorldTemplateId(new UUID(0, 0));
        startGamePacket.setCurrentTick(0);
        startGamePacket.setEnchantmentSeed(0);
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setVanillaVersion("*");

        startGamePacket.setAuthoritativeMovementMode(AuthoritativeMovementMode.SERVER);
        startGamePacket.setRewindHistorySize(0);
        startGamePacket.setServerAuthoritativeBlockBreaking(false);

        startGamePacket.setServerId("");
        startGamePacket.setWorldId("");
        startGamePacket.setScenarioId("");
        startGamePacket.setOwnerId("");

        session.sendPacket(startGamePacket);

        TransferPacket transferPacket = new TransferPacket();
        transferPacket.setAddress(sessionInfo.getIp());
        transferPacket.setPort(sessionInfo.getPort());
        session.sendPacket(transferPacket);

        try {
            if (identityData != null) {
                sessionManager.logger().info("Transferred bedrock client " + identityData.displayName + " (" + identityData.xuid + ") to target server.");
                sessionManager.storageManager().playerHistory().lastSeen(identityData.xuid, Instant.now());
            }
        } catch (IOException ignored) { }
    }
}
