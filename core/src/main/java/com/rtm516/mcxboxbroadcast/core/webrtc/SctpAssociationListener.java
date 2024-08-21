package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712;
import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPStream;

public class SctpAssociationListener implements AssociationListener {
    private final SessionInfo sessionInfo;
    private final Runnable onDisconnect;
    private final Logger logger;

    public SctpAssociationListener(SessionInfo sessionInfo, Logger logger, Runnable onDisconnect) {
        this.sessionInfo = sessionInfo;
        this.logger = logger.prefixed("SctpAssociationListener");
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void onAssociated(Association association) {
//        System.out.println("Association associated: " + association.toString());
    }

    @Override
    public void onDisAssociated(Association association) {
//        System.out.println("Association disassociated: " + association.toString());
        onDisconnect.run();
    }

    @Override
    public void onDCEPStream(SCTPStream sctpStream, String label, int i) throws Exception {
        if (label == null) {
            return;
        }
        logger.debug("Received DCEP SCTP stream: " + sctpStream.toString());

        if ("ReliableDataChannel".equals(label)) {
            sctpStream.setSCTPStreamListener(new MinecraftDataHandler(sctpStream, Bedrock_v712.CODEC, sessionInfo, logger));
        }
    }

    @Override
    public void onRawStream(SCTPStream sctpStream) {
//        System.out.println("Received raw SCTP stream: " + sctpStream.toString());
    }
}