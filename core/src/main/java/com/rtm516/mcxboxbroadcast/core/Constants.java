package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;

public class Constants {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule());

    public static final String AUTH_TITLE = "0000000048183522"; // 00000000441cc96b Nintendo Switch, 0000000048183522 Android
    public static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    public static final String RELAYING_PARTY = "http://xboxlive.com";

    public static final String SERVICE_CONFIG_ID = "4fc10100-5f7a-4470-899b-280835760c07"; // The service config ID for Minecraft
    public static final String CREATE_SESSION = "https://sessiondirectory.xboxlive.com/serviceconfigs/" + SERVICE_CONFIG_ID + "/sessionTemplates/MinecraftLobby/sessions/%s";
    public static final String JOIN_SESSION = "https://sessiondirectory.xboxlive.com/handles/%s/session";

    public static final URI LIVE_DEVICE_CODE_REQUEST = URI.create("https://login.live.com/oauth20_connect.srf");
    public static final URI LIVE_TOKEN_REQUEST = URI.create("https://login.live.com/oauth20_token.srf");
    public static final URI DEVICE_AUTHENTICATE_REQUEST = URI.create("https://device.auth.xboxlive.com/device/authenticate");
    public static final URI XSTS_AUTHENTICATE_REQUEST = URI.create("https://xsts.auth.xboxlive.com/xsts/authorize");
    public static final URI RTA_WEBSOCKET = URI.create("wss://rta.xboxlive.com/connect");
    public static final URI CREATE_HANDLE = URI.create("https://sessiondirectory.xboxlive.com/handles");

    public static final String PEOPLE = "https://social.xboxlive.com/users/me/people/xuid(%s)";
    public static final String USER_PRESENCE = "https://userpresence.xboxlive.com/users/xuid(%s)/devices/current/titles/current";
    public static final URI FOLLOWERS = URI.create("https://peoplehub.xboxlive.com/users/me/people/followers");
    public static final URI SOCIAL = URI.create("https://peoplehub.xboxlive.com/users/me/people/social");
    public static final URI SOCIAL_SUMMARY = URI.create("https://social.xboxlive.com/users/me/summary");

    /**
     * From the ConnectionType enum in the game
     * pre 1.19.10 UPNP was 7
     * 1.19.10+ UPNP is 6 as a previous entry was removed
     */
    public static int ConnectionTypeUPNP = 6;
}
