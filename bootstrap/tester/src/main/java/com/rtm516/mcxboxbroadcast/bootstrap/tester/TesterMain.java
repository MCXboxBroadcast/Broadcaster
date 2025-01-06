package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.github.mizosoft.methanol.Methanol;
import com.rtm516.mcxboxbroadcast.core.AuthManager;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.models.auth.SessionStartBody;
import com.rtm516.mcxboxbroadcast.core.models.auth.SessionStartResponse;
import com.rtm516.mcxboxbroadcast.core.models.auth.XboxTokenInfo;
import com.rtm516.mcxboxbroadcast.core.models.session.Connection;
import com.rtm516.mcxboxbroadcast.core.models.session.FollowerResponse;
import com.rtm516.mcxboxbroadcast.core.models.ws.WsToMessage;
import com.rtm516.mcxboxbroadcast.core.storage.FileStorageManager;
import com.rtm516.mcxboxbroadcast.core.webrtc.IceLogger;
import com.rtm516.mcxboxbroadcast.core.webrtc.RtcWebsocketClient;
import io.jsonwebtoken.lang.Collections;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCryptoProvider;
import org.ice4j.ice.Agent;
import org.java_websocket.util.NamedThreadFactory;
import org.opentelecoms.javax.sdp.NistSdpFactory;
import org.slf4j.LoggerFactory;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TesterMain {
    private static TeserLoggerImpl logger;
    private static HttpClient httpClient;
    private static ScheduledExecutorService scheduledThreadPool;
    private static AuthManager authManager;
    private static XboxTokenInfo xboxToken;

    public static void main(String[] args) {
        logger = new TeserLoggerImpl(LoggerFactory.getLogger(TesterMain.class));
        logger.setDebug(true);

        httpClient = Methanol.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .requestTimeout(Duration.ofMillis(5000))
            .build();

        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5, new NamedThreadFactory("MCXboxBroadcast Thread"));

        String xboxFriendName = "invincible rt";

        logger.info("Starting test against '" + xboxFriendName + "'");

        setupAuth();

        FollowerResponse.Person foundFriend = getFriend(xboxFriendName);
        if (foundFriend == null) {
            return;
        }

        Connection nethernetConnection = findNethernetConnection(foundFriend.xuid);
        if (nethernetConnection == null) {
            return;
        }

        logger.info("Found ID: " + nethernetConnection.NetherNetId());

        logger.info("Setting up WebRTC connection...");
        ExpandedSessionInfo sessionInfo = new ExpandedSessionInfo("", "", new SessionInfo("", "", "", 0, 0, 0, "", 0));
        TesterRtcWebsocketClient rtcWebsocket = new TesterRtcWebsocketClient(getMCToken(), sessionInfo, logger, scheduledThreadPool, nethernetConnection.NetherNetId());
        rtcWebsocket.connect();


        // Harvest
        // Open tunnel
        // Send packets to get transfer back
    }

    private static Connection findNethernetConnection(String xboxFriendXuid) {
        logger.info("Getting sessions...");
        HttpRequest sessionHandlesRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://sessiondirectory.xboxlive.com/handles/query?include=relatedInfo,customProperties"))
            .header("Authorization", xboxToken.tokenHeader())
            .header("x-xbl-contract-version", "107")
            .header("accept-language", "en-GB")
            .POST(HttpRequest.BodyPublishers.ofString("{\"owners\":{\"people\":{\"moniker\":\"people\",\"monikerXuid\":\"" + xboxToken.userXUID() + "\"}},\"scid\":\"" + Constants.SERVICE_CONFIG_ID + "\",\"type\":\"activity\"}"))
            .build();

        SessionHandlesResponse.SessionHandleResponse foundSession = null;
        try {
            String rawResponse = httpClient.send(sessionHandlesRequest, HttpResponse.BodyHandlers.ofString()).body();

            SessionHandlesResponse sessionHandlesResponse = Constants.GSON.fromJson(rawResponse, SessionHandlesResponse.class);

            foundSession = sessionHandlesResponse.results().stream().filter(session -> session.ownerXuid().equalsIgnoreCase(xboxFriendXuid)).findFirst().orElse(null);

            if (foundSession == null) {
                logger.error("Session not found");
                return null;
            }

            logger.info("Session found");
        } catch (Exception e) {
            logger.error("Failed to get friends", e);
            return null;
        }

        Connection nethernetConnection = foundSession.customProperties().SupportedConnections().stream().filter(connection -> connection.ConnectionType() == Constants.ConnectionTypeWebRTC).findFirst().orElse(null);
        if (nethernetConnection == null) {
            logger.error("No nethernet connection found");
            return null;
        }

        return nethernetConnection;
    }

    private static FollowerResponse.Person getFriend(String xboxFriendName) {
        logger.info("Getting friends...");
        HttpRequest xboxFriendsRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://peoplehub.xboxlive.com/users/me/people/friends"))
            .header("Authorization", xboxToken.tokenHeader())
            .header("x-xbl-contract-version", "7")
            .header("accept-language", "en-GB")
            .GET()
            .build();

        FollowerResponse.Person foundFriend = null;
        try {
            String rawResponse = httpClient.send(xboxFriendsRequest, HttpResponse.BodyHandlers.ofString()).body();

            FollowerResponse xboxFriendsResponse = Constants.GSON.fromJson(rawResponse, FollowerResponse.class);

            // This might need to be changed to something other than gamertag
            foundFriend = xboxFriendsResponse.people.stream().filter(friend -> friend.gamertag.equalsIgnoreCase(xboxFriendName)).findFirst().orElse(null);
            if (foundFriend == null) {
                logger.error("Friend not found");
                // TODO Add them and wait for response
                return null;
            }

            logger.info("Friend found");
        } catch (Exception e) {
            logger.error("Failed to get friends", e);
            return null;
        }

        return foundFriend;
    }

    private static void setupAuth() {
        logger.info("Doing auth...");
        authManager = new AuthManager(null, new FileStorageManager("./cache"), logger);
        xboxToken = authManager.getXboxToken();
    }

    private static String getMCToken() {
        String playfabTicket = authManager.getPlayfabSessionTicket();

        HttpRequest request = HttpRequest.newBuilder(Constants.START_SESSION)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(SessionStartBody.create(UUID.randomUUID().toString(), playfabTicket)))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Unable to start session", e);
        }

        if (response.statusCode() != 200) {
            logger.debug(response.body());
            throw new IllegalStateException("Unable to start session!");
        }

        return Constants.GSON.fromJson(response.body(), SessionStartResponse.class).result().authorizationHeader();
    }
}