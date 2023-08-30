package com.rtm516.mcxboxbroadcast.core.models.session;

import java.util.Date;
import java.util.List;

public class FollowerResponse {
    public List<Person> people;
    public Object recommendationSummary;
    public Object friendFinderState;
    public Object accountLinkDetails;

    public static class LinkedAccount {
        public String networkName;
        public String displayName;
        public boolean showOnProfile;
        public boolean isFamilyFriendly;
        public Object deeplink;
    }

    public static class Person {
        public String xuid;
        public boolean isFavorite;
        public boolean isFollowingCaller;
        public boolean isFollowedByCaller;
        public boolean isIdentityShared;
        public Date addedDateTimeUtc;
        public String displayName;
        public String realName;
        public String displayPicRaw;
        public String showUserAsAvatar;
        public String gamertag;
        public String gamerScore;
        public String modernGamertag;
        public String modernGamertagSuffix;
        public String uniqueModernGamertag;
        public String xboxOneRep;
        public String presenceState;
        public String presenceText;
        public Object presenceDevices;
        public boolean isBroadcasting;
        public Object isCloaked;
        public boolean isQuarantined;
        public boolean isXbox360Gamerpic;
        public Date lastSeenDateTimeUtc;
        public Object suggestion;
        public Object recommendation;
        public Object search;
        public Object titleHistory;
        public Object multiplayerSummary;
        public Object recentPlayer;
        public Object follower;
        public Object preferredColor;
        public Object presenceDetails;
        public Object titlePresence;
        public Object titleSummaries;
        public Object presenceTitleIds;
        public Object detail;
        public Object communityManagerTitles;
        public Object socialManager;
        public Object broadcast;
        public Object avatar;
        public List<LinkedAccount> linkedAccounts;
        public String colorTheme;
        public String preferredFlag;
        public List<String> preferredPlatforms;
    }
}
