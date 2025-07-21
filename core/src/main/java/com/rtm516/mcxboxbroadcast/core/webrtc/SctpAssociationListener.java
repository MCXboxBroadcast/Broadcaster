package com.rtm516.mcxboxbroadcast.core.webrtc;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;
import pe.pi.sctp4j.sctp.Association;
import pe.pi.sctp4j.sctp.AssociationListener;
import pe.pi.sctp4j.sctp.SCTPStream;

public class SctpAssociationListener implements AssociationListener {
    private final SessionInfo sessionInfo;
    private final Runnable onDisconnect;
    private final Logger logger;
    private final SessionManagerCore sessionManager;

    public SctpAssociationListener(SessionInfo sessionInfo, Logger logger, Runnable onDisconnect, SessionManagerCore sessionManager) {
        this.sessionInfo = sessionInfo;
        this.logger = logger.prefixed("SctpAssociationListener");
        this.onDisconnect = onDisconnect;
        this.sessionManager = sessionManager;
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
            sctpStream.setSCTPStreamListener(new MinecraftDataHandler(sctpStream, Constants.BEDROCK_CODEC, sessionInfo, logger, sessionManager));
        }
    }

    @Override
    public void onRawStream(SCTPStream sctpStream) {
    }
}