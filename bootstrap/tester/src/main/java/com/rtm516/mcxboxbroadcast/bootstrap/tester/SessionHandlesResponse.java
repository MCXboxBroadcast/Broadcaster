package com.rtm516.mcxboxbroadcast.bootstrap.tester;

import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.SessionCustomProperties;
import com.rtm516.mcxboxbroadcast.core.models.session.SessionRef;

import java.util.List;

public record SessionHandlesResponse(List<SessionHandleResponse> results) {
    public record SessionHandleResponse(
        String createTime,
        SessionCustomProperties customProperties,
        Object gameTypes,
        String id,
        String inviteProtocol,
        String ownerXuid,
        Object relatedInfo,
        SessionRef sessionRef,
        String titleId,
        String type,
        int version
    ) {
    }
}