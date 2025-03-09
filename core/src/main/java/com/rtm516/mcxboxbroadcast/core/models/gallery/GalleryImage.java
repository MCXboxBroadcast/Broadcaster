package com.rtm516.mcxboxbroadcast.core.models.gallery;

public record GalleryImage (
    String id,
    boolean isFeatured,
    String lastModified,
    String takenTime,
    String url
) {}
