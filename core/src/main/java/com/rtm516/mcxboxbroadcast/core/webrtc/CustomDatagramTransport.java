package com.rtm516.mcxboxbroadcast.core.webrtc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.bouncycastle.tls.DatagramTransport;
import org.ice4j.ice.Component;

public class CustomDatagramTransport implements DatagramTransport {
    private final int maxMessageSize = 262144; // vanilla
    private DatagramSocket socket;
    private Component component;

    public CustomDatagramTransport() {
    }

    public void init(Component component) {
        this.socket = component.getSocket();
        System.out.println("socket state: " + socket.isConnected());
        System.out.println("key state: " + component.getSelectedPair().getDatagramSocket().isConnected());
        this.component = component;
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
        System.out.println("receive! " + new String(buf, off, len));
        DatagramPacket packet = new DatagramPacket(buf, off, len);
        socket.receive(packet);
        return packet.getLength();
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        System.out.println("send! " + new String(buf, off, len));
        socket.send(new DatagramPacket(buf, off, len, component.getDefaultCandidate().getTransportAddress()));
    }

    @Override
    public void close() {
        socket.close();
    }
}
