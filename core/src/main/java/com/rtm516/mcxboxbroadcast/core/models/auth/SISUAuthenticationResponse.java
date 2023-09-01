package com.rtm516.mcxboxbroadcast.core.models.auth;

public record SISUAuthenticationResponse(
    GenericAuthenticationResponse AuthorizationToken,
    String DeviceToken,
    String Sandbox,
    GenericAuthenticationResponse TitleToken,
    boolean UseModernGamertag,
    GenericAuthenticationResponse UserToken,
    String WebPage
) {
}
