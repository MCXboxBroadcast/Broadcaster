package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.models.gallery.GalleryImage;
import com.rtm516.mcxboxbroadcast.core.models.gallery.GalleryResponse;
import com.rtm516.mcxboxbroadcast.core.models.gallery.GalleryUploadResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.zip.CRC32;

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
        GalleryImage[] existingImages = getImages();
        if (existingImages == null) {
            logger.error("Unable to set showcase image, you are likely being rate limited");
            return false;
        }

        String newImageId = null;

        if (existingImages.length >= 1) {
            // Check if the image is already set as the showcase image by comparing the hash of the image data
            try {
                BufferedImage newImageData = ImageIO.read(imageFile);
                String newImageHash = getImageHash(newImageData);

                // Loop through existing images and compare hashes
                for (GalleryImage image : existingImages) {
                    BufferedImage imageData = getImage(image);
                    String imageHash = getImageHash(imageData);

                    if (newImageHash.equals(imageHash)) {
                        newImageId = image.id();
                        logger.info("Showcase image is already set, skipping upload");
                        break;
                    }

                }
            } catch (IOException e) {
                logger.error("Failed to read new showcase image file", e);
                return false;
            }
        }

        if (newImageId == null) {
            newImageId = uploadImage(imageFile, true);
        }

        if (newImageId == null) {
            return false;
        }

        for (GalleryImage image : existingImages) {
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
            logger.error("Failed to upload and set featured gallery image", e);
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
            httpClient.send(deleteImageRequest, HttpResponse.BodyHandlers.ofString());
        } catch (JsonParseException | InterruptedException | IOException e) {
            logger.error("Failed to delete gallery image", e);
        }
    }

    private GalleryImage[] getImages() {
        HttpRequest getImagesRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.GALLERY + "/xuid/" + sessionManager.getXuid()))
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
            logger.error("Failed to get gallery images", e);
        }

        return null;
    }

    private BufferedImage getImage(GalleryImage image) {
        HttpRequest getImageRequest = HttpRequest.newBuilder()
            .uri(URI.create(image.url()))
            .header("Authorization", sessionManager.getMCTokenHeader())
            .GET()
            .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(getImageRequest, HttpResponse.BodyHandlers.ofByteArray());
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } catch (InterruptedException | IOException ignored) {
        }

        return null;
    }

    private String getImageHash(BufferedImage image) {
        if (image == null) {
            return "";
        }

        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        CRC32 digest = new CRC32();
        digest.update(data, 0, data.length);

        return String.format("%08x", digest.getValue());
    }
}
