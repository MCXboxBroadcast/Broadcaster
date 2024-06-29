package com.rtm516.mcxboxbroadcast.manager.models.response;

public record SuccessResponse(boolean success) implements CustomResponse {
    public SuccessResponse() {
        this(true);
    }
}
