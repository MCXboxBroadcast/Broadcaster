package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.common.PacketSignal;

public class TesterPacketHandler implements BedrockPacketHandler {
    private final TesterMinecraftDataHandler dataHandler;
    private final Logger logger;

    public TesterPacketHandler(TesterMinecraftDataHandler dataHandler, Logger logger) {
        this.dataHandler = dataHandler;
        this.logger = logger.prefixed("TesterPacketHandler");
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        BedrockPacketHandler.super.handlePacket(packet);
        return PacketSignal.HANDLED; // Avoids warning spam about all the packets we ignore and don't handle
    }

    @Override
    public PacketSignal handle(NetworkSettingsPacket packet) {
        dataHandler.enableCompression(packet.getCompressionAlgorithm(), packet.getCompressionThreshold());

        sendLoginPacket();

        return PacketSignal.HANDLED;
    }

    private void sendLoginPacket() {
        LoginPacket loginPacket = new LoginPacket();
        loginPacket.setProtocolVersion(Constants.BEDROCK_CODEC.getProtocolVersion());
        loginPacket.setAuthPayload(new TokenPayload("", AuthType.SELF_SIGNED));
        loginPacket.setClientJwt("");

        dataHandler.sendPacket(loginPacket);
    }
}
