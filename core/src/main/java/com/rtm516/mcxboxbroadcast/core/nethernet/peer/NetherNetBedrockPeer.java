package com.rtm516.mcxboxbroadcast.core.nethernet.peer;

import com.rtm516.mcxboxbroadcast.core.nethernet.codec.NetherNetCompressionDecoder;
import com.rtm516.mcxboxbroadcast.core.nethernet.codec.NetherNetCompressionEncoder;
import com.rtm516.mcxboxbroadcast.core.nethernet.codec.NetherNetPacketDecoder;
import com.rtm516.mcxboxbroadcast.core.nethernet.codec.NetherNetPacketEncoder;
import com.rtm516.mcxboxbroadcast.core.nethernet.initializer.NetherNetBedrockChannelInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockSessionFactory;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.netty.codec.compression.CompressionStrategy;

import javax.crypto.SecretKey;

import java.util.Objects;

public class NetherNetBedrockPeer extends BedrockPeer {
    public NetherNetBedrockPeer(Channel channel, BedrockSessionFactory sessionFactory) {
        super(channel, sessionFactory);
    }

    @Override
    public void enableEncryption(SecretKey secretKey) {
        // No-op
    }

    @Override
    public void setCompression(PacketCompressionAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        this.setCompression(NetherNetBedrockChannelInitializer.getCompression());
    }

    @Override
    public void setCompression(CompressionStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy");

        boolean prefixed = this.getCodec().getProtocolVersion() >= 649;

        ChannelPipeline pipeline = this.channel.pipeline();

        if (pipeline.get(NetherNetCompressionDecoder.NAME) == null) {
            pipeline.addBefore(NetherNetPacketDecoder.NAME, NetherNetCompressionDecoder.NAME,
                    new NetherNetCompressionDecoder(strategy, prefixed));
        }
        if (pipeline.get(NetherNetCompressionEncoder.NAME) == null) {
            pipeline.addBefore(NetherNetPacketEncoder.NAME, NetherNetCompressionEncoder.NAME,
                    new NetherNetCompressionEncoder(strategy, prefixed, 1));
        }
    }
}
