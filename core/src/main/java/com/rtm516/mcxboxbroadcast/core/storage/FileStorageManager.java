package com.rtm516.mcxboxbroadcast.core.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class FileStorageManager implements StorageManager {
    private final String cacheFolder;

    public FileStorageManager(String cacheFolder) {
        this.cacheFolder = cacheFolder;

        try {
            Files.createDirectories(Paths.get(cacheFolder));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache folder", e);
        }
    }

    private String read(String file) throws IOException {
        return Files.readString(Paths.get(cacheFolder, file));
    }

    private void write(String file, String data) throws IOException {
        Path filePath = Paths.get(cacheFolder, file);
        // Cleanup the file if the data is empty
        if (data.isBlank()) {
            Files.deleteIfExists(filePath);
            return;
        }

        Files.writeString(filePath, data);
    }


    @Override
    public String cache() throws IOException {
        return read("cache.json");
    }

    @Override
    public void cache(String data) throws IOException {
        write("cache.json", data);
    }

    @Override
    public String subSessions() throws IOException {
        return read("sub_sessions.json");
    }

    @Override
    public void subSessions(String data) throws IOException {
        write("sub_sessions.json", data);
    }

    @Override
    public String lastSessionResponse() throws IOException {
        return read("lastSessionResponse.json");
    }

    @Override
    public void lastSessionResponse(String data) throws IOException {
        write("lastSessionResponse.json", data);
    }

    @Override
    public String currentSessionResponse() throws IOException {
        return read("currentSessionResponse.json");
    }

    @Override
    public void currentSessionResponse(String data) throws IOException {
        write("currentSessionResponse.json", data);
    }

    @Override
    public StorageManager subSession(String id) {
        return new FileStorageManager(Paths.get(cacheFolder, id).toString());
    }

    @Override
    public void cleanup() throws IOException {
        Path cache = Paths.get(cacheFolder);
        try (Stream<Path> files = Files.walk(cache)) {
            files.map(Path::toFile)
                .forEach(File::delete);
            cache.toFile().delete();
        }
    }

    @Override
    public String liveToken() throws IOException {
        return read("live_token.json");
    }

    @Override
    public void liveToken(String data) throws IOException {
        write("live_token.json", data);
    }

    @Override
    public void xboxToken(String data) throws IOException {
        write("xbox_token.json", data);
    }
}
