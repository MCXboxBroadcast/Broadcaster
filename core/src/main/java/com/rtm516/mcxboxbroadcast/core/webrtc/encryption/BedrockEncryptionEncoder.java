package com.rtm516.mcxboxbroadcast.core.webrtc.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.FastThreadLocal;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

// https://github.com/CloudburstMC/Protocol/blob/3.0/bedrock-connection/src/main/java/org/cloudburstmc/protocol/bedrock/netty/codec/encryption/BedrockEncryptionEncoder.java
public class BedrockEncryptionEncoder {
    private static final FastThreadLocal<MessageDigest> DIGEST = new FastThreadLocal<>() {
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (Exception var2) {
                throw new AssertionError(var2);
            }
        }
    };
    private final AtomicLong packetCounter = new AtomicLong();
    private final SecretKey key;
    private final Cipher cipher;

    public ByteBuf encode(ByteBuf data) throws Exception {
        ByteBuf buf = Unpooled.buffer(data.readableBytes() + 8);

        try {
            ByteBuffer trailer = ByteBuffer.wrap(generateTrailer(data, this.key, this.packetCounter));
            ByteBuffer inBuffer = data.nioBuffer();
            ByteBuffer outBuffer = buf.nioBuffer(0, data.readableBytes() + 8);
            int index = this.cipher.update(inBuffer, outBuffer);
            index += this.cipher.update(trailer, outBuffer);
            buf.writerIndex(index);
            return buf.retain();
        } finally {
            buf.release();
        }

    }

    static byte[] generateTrailer(ByteBuf buf, SecretKey key, AtomicLong counter) {
        MessageDigest digest = DIGEST.get();
        ByteBuf counterBuf = ByteBufAllocator.DEFAULT.directBuffer(8);

        byte[] var7;
        try {
            counterBuf.writeLongLE(counter.getAndIncrement());
            ByteBuffer keyBuffer = ByteBuffer.wrap(key.getEncoded());
            digest.update(counterBuf.nioBuffer(0, 8));
            digest.update(buf.nioBuffer(buf.readerIndex(), buf.readableBytes()));
            digest.update(keyBuffer);
            byte[] hash = digest.digest();
            var7 = Arrays.copyOf(hash, 8);
        } finally {
            counterBuf.release();
            digest.reset();
        }

        return var7;
    }

    public BedrockEncryptionEncoder(SecretKey key, Cipher cipher) {
        this.key = key;
        this.cipher = cipher;
    }
}
