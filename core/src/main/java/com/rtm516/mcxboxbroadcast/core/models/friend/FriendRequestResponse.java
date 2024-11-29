package com.rtm516.mcxboxbroadcast.core.models.friend;

import com.rtm516.mcxboxbroadcast.core.models.session.FollowerResponse;

import java.util.List;

public class FriendRequestResponse {
    public Object accountLinkDetails;
    public Object friendFinderState;
    public Object friendRequestSummary;
    public Object recommendationSummary;
    public List<FollowerResponse.Person> people;
}
