package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cloudburstmc.netty.channel.nethernet.signaling.ClientInfo;
import org.cloudburstmc.netty.channel.nethernet.signaling.HttpSignalingSettings;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v2225.Bedrock_v2225;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class Constants {
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantConverter())
        .registerTypeAdapter(Date.class, new DateConverter())
        .disableHtmlEscaping()
        .create();

    public static final String SERVICE_CONFIG_ID = "4fc10100-5f7a-4470-899b-280835760c07"; // The service config ID for Minecraft
    public static final String TEMPLATE_NAME = "MinecraftLobby";
    public static final String TITLE_ID = "896928775"; // The title ID for Minecraft Windows Edition
    public static final String CREATE_SESSION = "https://sessiondirectory.xboxlive.com/serviceconfigs/" + SERVICE_CONFIG_ID + "/sessionTemplates/" + TEMPLATE_NAME + "/sessions/%s";
    public static final String JOIN_SESSION = "https://sessiondirectory.xboxlive.com/handles/%s/session";

    public static final URI RTA_WEBSOCKET = URI.create("wss://rta.xboxlive.com/connect");
    public static final URI CREATE_HANDLE = URI.create("https://sessiondirectory.xboxlive.com/handles");

    public static final String PEOPLE = "https://social.xboxlive.com/users/me/people/xuid(%s)";
    public static final String USER_PRESENCE = "https://userpresence.xboxlive.com/users/xuid(%s)/devices/current/titles/current";
    public static final URI FRIENDS = URI.create("https://peoplehub.xboxlive.com/users/me/people/friends");
    public static final URI FRIEND_REQUESTS = URI.create("https://peoplehub.xboxlive.com/users/me/people/friendrequests(received)");
    public static final URI SOCIAL_SUMMARY = URI.create("https://social.xboxlive.com/users/me/summary");
    public static final String FRIEND = "https://social.xboxlive.com/users/me/people/friends/v2/xuid(%s)";

    public static final String GALLERY = "https://persona.franchise.minecraft-services.net/api/v1.0/gallery";

    public static final Duration WEBSOCKET_CONNECTION_TIMEOUT = Duration.ofSeconds(10);

    // The session template allows 30 members, rotate before that to leave room for joins in progress
    public static final int ROTATE_MEMBER_COUNT = 28;

    // How long to keep the old session open after rotating, so players that are mid-join can finish connecting
    public static final Duration ROTATE_GRACE_PERIOD = Duration.ofSeconds(30);

    /**
     * Gathered from scraped web requests, seems to use the below enum
     * https://github.com/LiteLDev/LeviLamina/blob/main/src/mc/network/ConnectionType.h
     */
    public static final int ConnectionTypeJsonRpc = 7;

    public static final int MAX_FRIENDS = 1000;

    /**
     * The game version and protocol we advertise in the session
     */
    public static final BedrockCodec BEDROCK_CODEC = Bedrock_v2225.CODEC;

    /**
     * How we query servers over NetherNet HTTP signaling
     */
    public static final HttpSignalingSettings HTTP_SIGNALING = HttpSignalingSettings.DEFAULT
        .withClientInfo(new ClientInfo(BEDROCK_CODEC.getMinecraftVersion(), BEDROCK_CODEC.getProtocolVersion(), -1));

    /**
     * Config version for upgrade purposes
     */
    public static final int CONFIG_VERSION = 9;
}
