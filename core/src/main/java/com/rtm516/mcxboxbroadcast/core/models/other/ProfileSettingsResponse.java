package com.rtm516.mcxboxbroadcast.core.models.other;

import java.util.List;

public record ProfileSettingsResponse(List<ProfileUser> profileUsers) {
    public record ProfileUser(
        String hostId,
        String id,
        boolean isSponsoredUser,
        List<ProfileSetting> settings
    ) { }

    public record ProfileSetting(
        String id,
        String value
    ) { }
}
