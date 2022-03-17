package com.rtm516.mcxboxbroadcast.core.models;

import java.util.List;

public class PeopleResponse {
    public int totalCount;
    public List<Person> people;

    public static class Person {
        public String xuid;
        public String addedDateTimeUtc;
        public boolean isFavorite;
        public boolean isKnown;
        public List<String> socialNetworks;
        public boolean isFollowedByCaller;
        public boolean isFollowingCaller;
        public boolean isIdentityShared;
        public boolean isSquadMateWith;
        public boolean isUnfollowingFeed;
    }
}
