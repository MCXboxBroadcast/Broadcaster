package com.rtm516.mcxboxbroadcast.core.webrtc.bedrock;

import com.rtm516.mcxboxbroadcast.core.webrtc.MinecraftDataHandler;
import com.rtm516.mcxboxbroadcast.core.webrtc.Utils;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.BedrockDisconnectReasons;
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
import org.cloudburstmc.protocol.bedrock.packet.BiomeDefinitionListPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;

import java.util.UUID;

public class RedirectPacketHandler implements BedrockPacketHandler {
    private final MinecraftDataHandler dataHandler;

    /**
     * In Protocol V554 and above, RequestNetworkSettingsPacket is sent before LoginPacket.
     */
    private boolean networkSettingsRequested = false;

    public RedirectPacketHandler(MinecraftDataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Override
    public void onDisconnect(String reason) {
        // TODO
    }

    private void disconnect(String message) {
        DisconnectPacket disconnectPacket = new DisconnectPacket();
        if (message == null) {
            disconnectPacket.setMessageSkipped(true);
            message = BedrockDisconnectReasons.DISCONNECTED;
        }
        disconnectPacket.setKickMessage(message);
        dataHandler.sendPacket(disconnectPacket);
    }

    private void disconnect() {
        disconnect(null);
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        BedrockPacketHandler.super.handlePacket(packet);
        return PacketSignal.HANDLED; // Avoids warning spam about all the packets we ignore and don't handle
    }

    private boolean setCorrectCodec(int protocolVersion) {
        // TODO: Implement this?
//        BedrockCodec packetCodec = BedrockVersionUtils.bedrockCodec(protocolVersion);
//        if (packetCodec == null) {
//            // Protocol version is not supported
//            PlayStatusPacket status = new PlayStatusPacket();
//            if (protocolVersion > BedrockVersionUtils.latestProtocolVersion()) {
//                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
//            } else {
//                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
//            }
//
//            session.sendPacketImmediately(status);
//            session.disconnect();
//            return false;
//        }
//
//        session.setCodec(packetCodec);
        return true;
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        if (!setCorrectCodec(packet.getProtocolVersion())) {
            return PacketSignal.HANDLED; // Unsupported version, client has been disconnected
        }

        // New since 1.19.30 - sent before login packet
        PacketCompressionAlgorithm algorithm = PacketCompressionAlgorithm.ZLIB;

        NetworkSettingsPacket responsePacket = new NetworkSettingsPacket();
        responsePacket.setCompressionAlgorithm(algorithm);
        responsePacket.setCompressionThreshold(0);
        dataHandler.sendPacket(responsePacket);

//        session.setCompression(algorithm);
        networkSettingsRequested = true;
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        if (!networkSettingsRequested) {
            // This is expected for pre-1.19.30
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
            dataHandler.sendPacket(statusPacket);

            disconnect();
            return PacketSignal.HANDLED;
        }

        try {
            // TODO
//            ChainValidationResult.IdentityData extraData =
//                Utils.validateAndEncryptConnection(session, packet.getChain(), packet.getExtra());

            Utils.validateAndEncryptConnection(dataHandler, packet.getChain(), packet.getExtra());

            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            dataHandler.sendPacket(status);

            ResourcePacksInfoPacket info = new ResourcePacksInfoPacket();
            dataHandler.sendPacket(info);

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
                // TODO Test if we can send a transfer packet here
                sendStartGame();
                break;
            case HAVE_ALL_PACKS:
                ResourcePackStackPacket stack = new ResourcePackStackPacket();
                stack.setExperimentsPreviouslyToggled(false);
                stack.setForcedToAccept(false);
                stack.setGameVersion("*");
                dataHandler.sendPacket(stack);
                break;
            default:
                disconnect("disconnectionScreen.resourcePack");
                break;
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        TransferPacket transferPacket = new TransferPacket();
        transferPacket.setAddress("test.geysermc.org");
        transferPacket.setPort(19132);
        dataHandler.sendPacket(transferPacket);
        return PacketSignal.HANDLED;
    }

    public void sendStartGame() {
        // A lot of this likely doesn't need to be changed
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
        startGamePacket.setLevelName("GlobalLinkServer");
        startGamePacket.setPremiumWorldTemplateId("");
        startGamePacket.setWorldTemplateId(new UUID(0, 0));
        startGamePacket.setCurrentTick(0);
        startGamePacket.setEnchantmentSeed(0);
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setVanillaVersion("*");

        startGamePacket.setAuthoritativeMovementMode(AuthoritativeMovementMode.CLIENT);
        startGamePacket.setRewindHistorySize(0);
        startGamePacket.setServerAuthoritativeBlockBreaking(false);

        startGamePacket.setServerId("");
        startGamePacket.setWorldId("");
        startGamePacket.setScenarioId("");

        dataHandler.sendPacket(startGamePacket);

        // Send an empty chunk
        LevelChunkPacket data = new LevelChunkPacket();
        data.setChunkX(0);
        data.setChunkZ(0);
        data.setSubChunksLength(0);
        data.setData(Unpooled.wrappedBuffer(PaletteUtils.EMPTY_LEVEL_CHUNK_DATA));
        data.setCachingEnabled(false);
        dataHandler.sendPacket(data);

        // Send the biomes
        BiomeDefinitionListPacket biomeDefinitionListPacket = new BiomeDefinitionListPacket();
        biomeDefinitionListPacket.setDefinitions(PaletteUtils.BIOMES_PALETTE);
        dataHandler.sendPacket(biomeDefinitionListPacket);

        // Let the client know the player can spawn
        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
        dataHandler.sendPacket(playStatusPacket);

        // Freeze the player
        SetEntityMotionPacket setEntityMotionPacket = new SetEntityMotionPacket();
        setEntityMotionPacket.setRuntimeEntityId(1);
        setEntityMotionPacket.setMotion(Vector3f.ZERO);
        dataHandler.sendPacket(setEntityMotionPacket);
    }
}
