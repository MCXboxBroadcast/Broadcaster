package com.rtm516.mcxboxbroadcast.core.nethernet;

import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.codec.v554.Bedrock_v554;
import org.cloudburstmc.protocol.bedrock.codec.v560.Bedrock_v560;
import org.cloudburstmc.protocol.bedrock.codec.v575.Bedrock_v575;
import org.cloudburstmc.protocol.bedrock.codec.v582.Bedrock_v582;
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589;
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594;
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685;
import org.cloudburstmc.protocol.bedrock.codec.v686.Bedrock_v686;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748;
import org.cloudburstmc.protocol.bedrock.codec.v766.Bedrock_v766;
import org.cloudburstmc.protocol.bedrock.codec.v776.Bedrock_v776;
import org.cloudburstmc.protocol.bedrock.codec.v786.Bedrock_v786;
import org.cloudburstmc.protocol.bedrock.codec.v800.Bedrock_v800;
import org.cloudburstmc.protocol.bedrock.codec.v818.Bedrock_v818;
import org.cloudburstmc.protocol.bedrock.codec.v827.Bedrock_v827;
import org.cloudburstmc.protocol.bedrock.codec.v844.Bedrock_v844;
import org.cloudburstmc.protocol.bedrock.codec.v859.Bedrock_v859;
import org.cloudburstmc.protocol.bedrock.codec.v860.Bedrock_v860;
import org.cloudburstmc.protocol.bedrock.codec.v898.Bedrock_v898;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
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

import java.util.HashMap;
import java.util.Map;

public class BedrockCodecProvider {

    private static final Map<Integer, BedrockCodec> MINIMAL_CODECS = new HashMap<>();
    private static final int LATEST_PROTOCOL_VERSION;

    static {
        // Register all supported versions >= 554 for RequestNetworkSettingsPacket support
        register(Bedrock_v554.CODEC);
        register(Bedrock_v560.CODEC);
        register(Bedrock_v575.CODEC);
        register(Bedrock_v582.CODEC);
        register(Bedrock_v589.CODEC);
        register(Bedrock_v594.CODEC);
        register(Bedrock_v618.CODEC);
        register(Bedrock_v622.CODEC);
        register(Bedrock_v630.CODEC);
        register(Bedrock_v649.CODEC);
        register(Bedrock_v662.CODEC);
        register(Bedrock_v671.CODEC);
        register(Bedrock_v685.CODEC);
        register(Bedrock_v686.CODEC);
        register(Bedrock_v712.CODEC);
        register(Bedrock_v729.CODEC);
        register(Bedrock_v748.CODEC);
        register(Bedrock_v766.CODEC);
        register(Bedrock_v776.CODEC);
        register(Bedrock_v786.CODEC);
        register(Bedrock_v800.CODEC);
        register(Bedrock_v818.CODEC);
        register(Bedrock_v827.CODEC);
        register(Bedrock_v844.CODEC);
        register(Bedrock_v859.CODEC);
        register(Bedrock_v860.CODEC);
        LATEST_PROTOCOL_VERSION = register(Bedrock_v898.CODEC);
    }

    private static int register(BedrockCodec original) {
        int version = original.getProtocolVersion();
        MINIMAL_CODECS.put(version, createMinimal(original));
        return version;
    }

    public static BedrockCodec getCodec(int protocolVersion) {
        return MINIMAL_CODECS.get(protocolVersion);
    }

    public static BedrockCodec getLatestCodec() {
        return MINIMAL_CODECS.get(LATEST_PROTOCOL_VERSION);
    }

    private static BedrockCodec createMinimal(BedrockCodec original) {
        BedrockCodec.Builder builder = BedrockCodec.builder()
                .protocolVersion(original.getProtocolVersion())
                .minecraftVersion(original.getMinecraftVersion())
                .helper(() -> original.createHelper());

        // Register only the specific packets needed by RedirectPacketHandler
        registerPacket(builder, original, RequestNetworkSettingsPacket.class);
        registerPacket(builder, original, NetworkSettingsPacket.class);
        registerPacket(builder, original, LoginPacket.class);
        registerPacket(builder, original, PlayStatusPacket.class);
        registerPacket(builder, original, ResourcePacksInfoPacket.class);
        registerPacket(builder, original, ClientCacheStatusPacket.class);
        registerPacket(builder, original, ResourcePackClientResponsePacket.class);
        registerPacket(builder, original, ResourcePackStackPacket.class);
        registerPacket(builder, original, StartGamePacket.class);
        registerPacket(builder, original, TransferPacket.class);

        return builder.build();
    }

    private static <T extends BedrockPacket> void registerPacket(BedrockCodec.Builder builder, BedrockCodec source, Class<T> packetClass) {
        BedrockPacketDefinition<T> definition = source.getPacketDefinition(packetClass);
        if (definition != null) {
            builder.registerPacket(
                definition.getFactory(), 
                definition.getSerializer(), 
                definition.getId(), 
                definition.getRecipient()
            );
        }
    }
}