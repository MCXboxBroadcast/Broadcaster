package com.rtm516.mcxboxbroadcast.core.models.auth;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.ExpandedSessionInfo;
import java.util.HashMap;

public final class SessionStartBody {
    private SessionStartBody() {}

    public static String sessionStart(ExpandedSessionInfo info, String playfabSessionTicket) {
        return Constants.GSON_NULLS.toJson(new HashMap<>() {{
            put("device", new HashMap<>() {{
                put("applicationType", "MinecraftPE");
                put("capabilities", new String[0]); // it's RayTracing for me
                put("gameVersion", info.getVersion());
                put("id", info.getDeviceId());
                put("memory", "8589934592"); // exactly 8GiB
                put("platform", "Windows10"); // idk if it matters but the auth was with Android
                put("playFabTitleId", "20CA2");
                put("storePlatform", "uwp.store");
                put("treatmentOverrides", null);
                put("type", "Windows10");
            }});;
            put("user", new HashMap<>() {{
                put("language", "en");
                put("languageCode", "en-US");
                put("regionCode", "US");
                put("token", playfabSessionTicket);
                put("tokenType", "PlayFab");
            }});
        }});
    }
}
