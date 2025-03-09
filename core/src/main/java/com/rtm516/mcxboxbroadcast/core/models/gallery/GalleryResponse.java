package com.rtm516.mcxboxbroadcast.core.models.gallery;

public record GalleryResponse (
    Result result
) {
    public record Result (
        GalleryImage[] showcasedImages
    ) {}
}
