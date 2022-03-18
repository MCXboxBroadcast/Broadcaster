package com.rtm516.mcxboxbroadcast.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

public class Constants {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String AUTH_TITLE = "00000000441cc96b"; // Minecraft for Nintendo Switch
    public static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    public static final String RELAYING_PARTY = "http://xboxlive.com";

    public static final String SERVICE_CONFIG_ID = "4fc10100-5f7a-4470-899b-280835760c07"; // The service config ID for Minecraft
    public static final String CREATE_SESSION = "https://sessiondirectory.xboxlive.com/serviceconfigs/" + SERVICE_CONFIG_ID + "/sessionTemplates/MinecraftLobby/sessions/";

    public static final URI LIVE_DEVICE_CODE_REQUEST = URI.create("https://login.live.com/oauth20_connect.srf");
    public static final URI LIVE_TOKEN_REQUEST = URI.create("https://login.live.com/oauth20_token.srf");
    public static final URI USER_AUTHENTICATE_REQUEST = URI.create("https://user.auth.xboxlive.com/user/authenticate");
    public static final URI DEVICE_AUTHENTICATE_REQUEST = URI.create("https://device.auth.xboxlive.com/device/authenticate");
    public static final URI TITLE_AUTHENTICATE_REQUEST = URI.create("https://title.auth.xboxlive.com/title/authenticate");
    public static final URI XSTS_AUTHENTICATE_REQUEST = URI.create("https://xsts.auth.xboxlive.com/xsts/authorize");
    public static final URI RTA_WEBSOCKET = URI.create("wss://rta.xboxlive.com/connect");
    public static final URI CREATE_HANDLE = URI.create("https://sessiondirectory.xboxlive.com/handles");

    public static final URI PEOPLE = URI.create("https://social.xboxlive.com/users/me/people");
}
