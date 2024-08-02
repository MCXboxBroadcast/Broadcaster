package com.rtm516.mcxboxbroadcast.core.models.auth;

import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;

public record XstsAuthData(StepXblSisuAuthentication.XblSisuTokens xstsToken, StepXblSisuAuthentication xstsAuth) {
}
