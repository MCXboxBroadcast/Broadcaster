package com.rtm516.mcxboxbroadcast.core.models.auth;

import com.google.gson.JsonObject;
import com.rtm516.mcxboxbroadcast.core.Constants;
import java.util.HashMap;

public final class PlayfabLoginBody {
    public static JsonObject create(String xboxToken) {
        return Constants.GSON_NULLS.toJsonTree(new HashMap<>() {{
            put("CreateAccount", true);
            put("EncryptedRequest", null);
            put("InfoRequestParameters", new HashMap<>() {{
                put("GetCharacterInventories", false);
                put("GetCharacterList", false);
                put("GetPlayerProfile", true);
                put("GetPlayerStatistics", false);
                put("GetTitleData", false);
                put("GetUserAccountInfo", true);
                put("GetUserData", false);
                put("GetUserInventory", false);
                put("GetUserReadOnlyData", false);
                put("GetUserVirtualCurrency", false);
                put("PlayerStatisticNames", null);
                put("ProfileConstraints", null);
                put("TitleDataKeys", null);
                put("UserDataKeys", null);
                put("UserReadOnlyDataKeys", null);
            }});
            put("PlayerSecret", null);
            put("TitleId", "20CA2");
            put("XboxToken", "XBL3.0 x=" + xboxToken);
        }}).getAsJsonObject();
    }
}
