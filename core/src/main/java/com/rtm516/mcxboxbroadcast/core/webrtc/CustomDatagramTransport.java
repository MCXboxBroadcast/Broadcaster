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
//        System.out.println("receive! " + bytesToHex(buf));

        DatagramPacket packet = new DatagramPacket(buf, off, len);
        socket.receive(packet);
        return packet.getLength();
    }

    @Override
    public void send(byte[] buf, int off, int len) throws IOException {
        System.out.println("send! " + new String(buf, off, len));
//        System.out.println("send! " + bytesToHex(buf));
        socket.send(new DatagramPacket(buf, off, len, component.getDefaultCandidate().getTransportAddress()));
    }

    @Override
    public void close() {
        socket.close();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
