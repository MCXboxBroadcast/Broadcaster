package com.rtm516.mcxboxbroadcast.core.models.auth;

import com.google.gson.JsonObject;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession.InitialXblSession;
import net.raphimc.minecraftauth.util.CryptUtil;

public final class SisuAuthorizeBody {
    private SisuAuthorizeBody() {}

    public static JsonObject sisuAuthorizedBody(InitialXblSession initialSession, String relyingParty) {
        var content = new JsonObject();
        content.addProperty("AccessToken", "t=" + initialSession.getMsaToken().getAccessToken());
        content.addProperty("DeviceToken", initialSession.getXblDeviceToken().getToken());
        content.addProperty("AppId", "00000000402b5328");
        content.add("ProofKey", CryptUtil.getProofKey(initialSession.getXblDeviceToken().getPublicKey()));
        content.addProperty("SiteName", "user.auth.xboxlive.com");
        content.addProperty("RelyingParty", relyingParty);
        content.addProperty("Sandbox", "RETAIL");
        content.addProperty("UseModernGamertag", true);
        return content;
    }
}
