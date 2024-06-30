package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

public class Constants {
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantConverter())
        .registerTypeAdapter(Date.class, new DateConverter())
        .create();

    public static final String SERVICE_CONFIG_ID = "4fc10100-5f7a-4470-899b-280835760c07"; // The service config ID for Minecraft
    public static final String CREATE_SESSION = "https://sessiondirectory.xboxlive.com/serviceconfigs/" + SERVICE_CONFIG_ID + "/sessionTemplates/MinecraftLobby/sessions/%s";
    public static final String JOIN_SESSION = "https://sessiondirectory.xboxlive.com/handles/%s/session";

    public static final URI RTA_WEBSOCKET = URI.create("wss://rta.xboxlive.com/connect");
    public static final URI CREATE_HANDLE = URI.create("https://sessiondirectory.xboxlive.com/handles");

    public static final String PEOPLE = "https://social.xboxlive.com/users/me/people/xuid(%s)";
    public static final String USER_PRESENCE = "https://userpresence.xboxlive.com/users/xuid(%s)/devices/current/titles/current";
    public static final URI FOLLOWERS = URI.create("https://peoplehub.xboxlive.com/users/me/people/followers");
    public static final URI SOCIAL = URI.create("https://peoplehub.xboxlive.com/users/me/people/social");
    public static final URI SOCIAL_SUMMARY = URI.create("https://social.xboxlive.com/users/me/summary");
    public static final URI BLOCK = URI.create("https://privacy.xboxlive.com/users/me/people/never");

    /**
     * From the ConnectionType enum in the game
     * pre 1.19.10 UPNP was 7
     * 1.19.10+ UPNP is 6 as a previous entry was removed
     */
    public static int ConnectionTypeUPNP = 6;
}
