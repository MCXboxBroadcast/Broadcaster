package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v897.Bedrock_v897;

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

    public static final Gson GSON_NULLS = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantConverter())
        .registerTypeAdapter(Date.class, new DateConverter())
        .serializeNulls()
        .create();

    public static final String SERVICE_CONFIG_ID = "4fc10100-5f7a-4470-899b-280835760c07"; // The service config ID for Minecraft
    public static final String TEMPLATE_NAME = "MinecraftLobby";
    public static final String TITLE_ID = "896928775"; // The title ID for Minecraft Windows Edition
    public static final String CREATE_SESSION = "https://sessiondirectory.xboxlive.com/serviceconfigs/" + SERVICE_CONFIG_ID + "/sessionTemplates/" + TEMPLATE_NAME + "/sessions/%s";
    public static final String JOIN_SESSION = "https://sessiondirectory.xboxlive.com/handles/%s/session";

    public static final String PLAYFAB_LOGIN = "https://20ca2.playfabapi.com/Client/LoginWithXbox";
    public static final URI START_SESSION = URI.create("https://authorization.franchise.minecraft-services.net/api/v1.0/session/start");
    public static final String RTC_WEBSOCKET_FORMAT = "wss://signal.franchise.minecraft-services.net/ws/v1.0/signaling/%s";

    public static final URI RTA_WEBSOCKET = URI.create("wss://rta.xboxlive.com/connect");
    public static final URI CREATE_HANDLE = URI.create("https://sessiondirectory.xboxlive.com/handles");

    public static final String PEOPLE = "https://social.xboxlive.com/users/me/people/xuid(%s)";
    public static final String USER_PRESENCE = "https://userpresence.xboxlive.com/users/xuid(%s)/devices/current/titles/current";
    public static final URI FOLLOWERS = URI.create("https://peoplehub.xboxlive.com/users/me/people/followers");
    public static final URI SOCIAL = URI.create("https://peoplehub.xboxlive.com/users/me/people/social");
    public static final URI SOCIAL_SUMMARY = URI.create("https://social.xboxlive.com/users/me/summary");
    public static final String FOLLOWER = "https://social.xboxlive.com/users/me/people/follower/xuid(%s)";
    public static final String PROFILE_SETTINGS = "https://profile.xboxlive.com/users/xuid(%s)/profile/settings?settings=Gamertag";

    public static final String GALLERY = "https://persona.franchise.minecraft-services.net/api/v1.0/gallery";

    public static final Duration WEBSOCKET_CONNECTION_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Gathered from scraped web requests
     */
    public static final int ConnectionTypeWebRTC = 3;

    /**
     * Used to be 1000, but the limit was increased in Aug 2024
     */
    public static final int MAX_FRIENDS = 2000;

    /**
     * Used for the micro nethernet server that trasnfers the client to the real server
     */
    public static final BedrockCodec BEDROCK_CODEC = Bedrock_v897.CODEC.toBuilder().protocolVersion(898).build();
}
