package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.models.gallery.GalleryImage;
import com.rtm516.mcxboxbroadcast.core.models.gallery.GalleryResponse;
import com.rtm516.mcxboxbroadcast.core.models.gallery.GalleryUploadResponse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

public class GalleryManager {
    private final HttpClient httpClient;
    private final Logger logger;
    private final SessionManagerCore sessionManager;

    public GalleryManager(HttpClient httpClient, Logger logger, SessionManagerCore sessionManager) {
        this.httpClient = httpClient;
        this.logger = logger;
        this.sessionManager = sessionManager;
    }

    public boolean setShowcase(File imageFile) {
        String newImageId = uploadImage(imageFile, true);

        if (newImageId == null) {
            return false;
        }

        for (GalleryImage image : getImages()) {
            if (image.id().equals(newImageId)) {
                continue;
            }
            deleteImage(image.id());
        }

        return true;
    }

    private String uploadImage(File imageFile, boolean isFeatured) {
        try {
            HttpRequest uploadImageRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.GALLERY))
                .header("Authorization", sessionManager.getMCTokenHeader())
                .header("content-type", "application/octet-stream")
                .header("x-ms-showcased-featured", String.valueOf(isFeatured))
                .header("x-ms-showcased-timetaken", Instant.ofEpochMilli(imageFile.lastModified()).toString())
                .POST(HttpRequest.BodyPublishers.ofFile(imageFile.toPath()))
                .build();

            HttpResponse<String> response = httpClient.send(uploadImageRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 202) {
                logger.error("Failed to upload and set featured gallery image: " + response.body());
                return null;
            }

            GalleryImage image = Constants.GSON.fromJson(response.body(), GalleryUploadResponse.class).result();

            return image.id();
        } catch (JsonParseException | InterruptedException | IOException e) {
            logger.error("Failed to upload and set featured gallery image: " + e.getMessage());
        }

        return null;
    }

    private void deleteImage(String imageId) {
        HttpRequest deleteImageRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.GALLERY + "/" + imageId))
            .header("Authorization", sessionManager.getMCTokenHeader())
            .DELETE()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(deleteImageRequest, HttpResponse.BodyHandlers.ofString());
        } catch (JsonParseException | InterruptedException | IOException e) {
            logger.error("Failed to delete gallery image: " + e.getMessage());
        }
    }

    private GalleryImage[] getImages() {
        HttpRequest getImagesRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.GALLERY + "/xuid/" + sessionManager.userXUID()))
            .header("Authorization", sessionManager.getMCTokenHeader())
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(getImagesRequest, HttpResponse.BodyHandlers.ofString());
            GalleryResponse.Result result = Constants.GSON.fromJson(response.body(), GalleryResponse.class).result();
            if (result == null) {
                throw new RuntimeException("Gallery response result is null");
            }
            return result.showcasedImages();
        } catch (InterruptedException | IOException | RuntimeException e) {
            logger.error("Failed to get gallery images: " + e.getMessage());
        }

        return new GalleryImage[0];
    }
}
