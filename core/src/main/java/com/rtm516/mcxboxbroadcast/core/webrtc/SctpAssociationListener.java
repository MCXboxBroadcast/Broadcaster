package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729;
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
        logger.debug("SCTP session associated");
    }

    @Override
    public void onDisAssociated(Association association) {
        onDisconnect.run();
    }

    @Override
    public void onDCEPStream(SCTPStream sctpStream, String label, int i) {
        if (label == null) {
            return;
        }
        logger.debug("Received DCEP SCTP stream: " + sctpStream.toString());

        if ("ReliableDataChannel".equals(label)) {
            sctpStream.setSCTPStreamListener(new MinecraftDataHandler(sctpStream, Bedrock_v729.CODEC, sessionInfo, logger));
        }
    }

    @Override
    public void onRawStream(SCTPStream sctpStream) {
    }
}