package com.rtm516.mcxboxbroadcast.core.models;

import java.util.Date;
import java.util.List;

public class FollowerResponse {
    public Object accountLinkDetails;
    public Object friendFinderState;
    public List<Person> people;
    public Object recommendationSummary;

    public static class Follower {
        public Date followedDateTime;
        public String text;
    }

    public static class LinkedAccount {
        public Object deeplink;
        public String displayName;
        public boolean isFamilyFriendly;
        public String networkName;
        public boolean showOnProfile;
    }

    public static class Person {
        public Object addedDateTimeUtc;
        public Object avatar;
        public Object broadcast;
        public String colorTheme;
        public Object communityManagerTitles;
        public Object detail;
        public String displayName;
        public String displayPicRaw;
        public Follower follower;
        public String gamerScore;
        public String gamertag;
        public boolean isBroadcasting;
        public Object isCloaked;
        public boolean isFavorite;
        public boolean isFollowedByCaller;
        public boolean isFollowingCaller;
        public boolean isIdentityShared;
        public boolean isQuarantined;
        public boolean isXbox360Gamerpic;
        public Date lastSeenDateTimeUtc;
        public List<LinkedAccount> linkedAccounts;
        public String modernGamertag;
        public String modernGamertagSuffix;
        public Object multiplayerSummary;
        public Object preferredColor;
        public String preferredFlag;
        public List<String> preferredPlatforms;
        public Object presenceDetails;
        public Object presenceDevices;
        public String presenceState;
        public String presenceText;
        public Object presenceTitleIds;
        public String realName;
        public Object recentPlayer;
        public Object recommendation;
        public Object search;
        public String showUserAsAvatar;
        public Object socialManager;
        public Object suggestion;
        public Object titleHistory;
        public Object titlePresence;
        public Object titleSummaries;
        public String uniqueModernGamertag;
        public String xboxOneRep;
        public String xuid;
    }
}
