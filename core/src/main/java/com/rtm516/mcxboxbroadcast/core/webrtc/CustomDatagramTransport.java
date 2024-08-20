package com.rtm516.mcxboxbroadcast.core.webrtc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.bouncycastle.tls.DatagramTransport;
import org.ice4j.ice.Component;

public class CustomDatagramTransport implements DatagramTransport {
    private final int maxMessageSize = 262144; // default message size as provided in the ice attributes
    private DatagramSocket socket;

    public CustomDatagramTransport() {
    }

    public void init(Component component) {
        this.socket = component.getSocket();
    }

    @Override
    public int getReceiveLimit() {
        return maxMessageSize;
    }

    @Override
    public int getSendLimit() {
        return maxMessageSize;
    }

    @Override
    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        DatagramPacket packet = new DatagramPacket(buf, off, len);
        socket.receive(packet);
        return packet.getLength();
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        socket.send(new DatagramPacket(buf, off, len));
    }

    @Override
    public void close() {
        socket.close();
    }
}
