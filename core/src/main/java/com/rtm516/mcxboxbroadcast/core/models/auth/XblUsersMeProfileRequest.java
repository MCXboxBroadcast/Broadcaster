package com.rtm516.mcxboxbroadcast.core.models.auth;

import net.lenni0451.commons.gson.elements.GsonElement;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;
import net.raphimc.minecraftauth.xbl.responsehandler.XblResponseHandler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XblUsersMeProfileRequest extends GetRequest implements XblResponseHandler<XblUsersMeProfileRequest.Response> {

    public XblUsersMeProfileRequest(final XblXstsToken xstsToken) throws MalformedURLException {
        super("https://profile.xboxlive.com/users/me/profile/settings?settings=Gamertag");

        this.setHeader(HttpHeaders.AUTHORIZATION, xstsToken.getAuthorizationHeader());
        this.setHeader("x-xbl-contract-version", "3");
    }

    @Override
    public Response handle(final HttpResponse response, final GsonObject json) {
        return Response.fromJson(json);
    }

    public record Response(
        List<ProfileUser> profileUsers) {
        public static Response fromJson(GsonObject json) {
            List<ProfileUser> profileUsers = new ArrayList<>();
            for (GsonElement userJson : json.reqArray("profileUsers")) {
                profileUsers.add(ProfileUser.fromJson(userJson.asObject()));
            }
            return new Response(profileUsers);
        }

        public record ProfileUser(
            String hostId,
            String id,
            boolean isSponsoredUser,
            Map<String, String> settings
        ) {

            public static ProfileUser fromJson(GsonObject json) {
                Map<String, String> settings = new HashMap<>();
                for (GsonElement settingJson : json.reqArray("settings")) {
                    GsonObject settingObj = settingJson.asObject();
                    settings.put(settingObj.getString("id"), settingObj.getString("value"));
                }

                return new ProfileUser(
                    json.reqString("hostId"),
                    json.reqString("id"),
                    json.reqBoolean("isSponsoredUser"),
                    settings
                );
            }
        }
    }
}

