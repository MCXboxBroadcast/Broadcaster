package com.rtm516.mcxboxbroadcast.core.webrtc;

import org.bouncycastle.tls.DatagramTransport;
import org.ice4j.ice.Component;
import org.ice4j.ice.ComponentSocket;

import java.io.IOException;
import java.net.DatagramPacket;

public class CustomDatagramTransport implements DatagramTransport {
    private final ComponentSocket socket;
    private final Component component;
    private final int receiveLimit = 1500; // Typically, a standard MTU size
    private final int sendLimit = 1500;    // Typically, a standard MTU size

    public CustomDatagramTransport(Component component) {
        this.socket = component.getComponentSocket();
        this.component = component;
    }

    @Override
    public int getReceiveLimit() {
        return receiveLimit;
    }

    @Override
    public int getSendLimit() {
        return sendLimit;
    }

    @Override
    public int receive(byte[] buf, int off, int len, int waitMillis) throws IOException {
        DatagramPacket packet = new DatagramPacket(buf, off, len);
        socket.receive(packet);
        return packet.getLength();
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        DatagramPacket packet = new DatagramPacket(buf, off, len, component.getDefaultCandidate().getTransportAddress());
        socket.send(packet);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
