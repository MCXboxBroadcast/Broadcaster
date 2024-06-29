package com.rtm516.mcxboxbroadcast.manager.controllers;

import com.rtm516.mcxboxbroadcast.core.AuthManager;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.models.auth.XstsAuthData;
import com.rtm516.mcxboxbroadcast.manager.BackendManager;
import com.rtm516.mcxboxbroadcast.manager.BotManager;
import com.rtm516.mcxboxbroadcast.manager.models.BotContainer;
import com.rtm516.mcxboxbroadcast.manager.models.response.ErrorResponse;
import com.rtm516.mcxboxbroadcast.manager.models.response.ImportResponse;
import jakarta.servlet.http.HttpServletResponse;
import net.raphimc.minecraftauth.MinecraftAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController()
@RequestMapping("/api/bots/import")
public class BotsImportController {
    private final BotManager botManager;
    private final BackendManager backendManager;

    @Autowired
    public BotsImportController(BotManager botManager, BackendManager backendManager) {
        this.botManager = botManager;
        this.backendManager = backendManager;
    }

    @PostMapping("/legacy")
    public Object importLegacy(HttpServletResponse response, @RequestParam("file") MultipartFile file) {
        // Check file format
        if (!file.getOriginalFilename().endsWith(".zip")) {
            response.setStatus(400);
            return new ErrorResponse("Invalid file format");
        }

        // Load zip
        Map<String, String> cacheFiles = new HashMap<>();
        List<String> subSessions = new ArrayList<>();
        try {
            ZipInputStream zis = new ZipInputStream(file.getInputStream());

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Adjust root if it contains the cache folder and isn't inside
                String cleanName = entry.getName();
                if (cleanName.startsWith("cache/")) {
                    cleanName = cleanName.substring(6);
                }

                if (cleanName.endsWith("cache.json")) {
                    // Read the cache.json file and store the string contents
                    cacheFiles.put(cleanName, getEntryString(zis));
                } else if (cleanName.equals("sub_sessions.json")) {
                    subSessions.addAll(List.of(Constants.GSON.fromJson(getEntryString(zis), String[].class)));
                }
            }
        } catch (IOException e) {
            response.setStatus(400);
            return new ErrorResponse("Error reading zip file");
        }

        if (!cacheFiles.containsKey("cache.json")) {
            response.setStatus(400);
            return new ErrorResponse("Missing main session cache file");
        }

        List<BotContainer> importedBots = new ArrayList<>();

        // Create a bot for the main session
        BotContainer mainBot = botManager.addBot();
        mainBot.cache(cacheFiles.get("cache.json"));
        importedBots.add(mainBot);

        // For each sub session check for the cache file and create a bot
        for (String subSession : subSessions) {
            if (cacheFiles.containsKey(subSession + "/cache.json")) {
                BotContainer subBot = botManager.addBot();
                subBot.cache(cacheFiles.get(subSession + "/cache.json"));
                importedBots.add(subBot);
            }
        }

        // Start all the imported bots
        backendManager.scheduledThreadPool().execute(() -> importedBots.forEach(BotContainer::start));

        response.setStatus(200);
        return new ImportResponse(importedBots.size(), Math.max(cacheFiles.size(), subSessions.size()));
    }

    @PostMapping("/credentials")
    public Object importCredentials(HttpServletResponse response, @RequestBody String credentialsData) {
        if (credentialsData.isBlank()) {
            response.setStatus(400);
            return new ErrorResponse("No credentials provided");
        }

        List<BotContainer> importedBots = new ArrayList<>();

        // Parse the credentials data email:password
        String[] lines = credentialsData.split("\n");
        for (String credential : lines) {
            String[] parts = credential.split(":");
            if (parts.length != 2) {
                continue;
            }

            // Process the credentials
            String cacheData = "";
            try {
                // TODO Use a different logger
                XstsAuthData xstsAuthData = AuthManager.fromCredentials(parts[0], parts[1], MinecraftAuth.LOGGER);
                cacheData = Constants.GSON.toJson(xstsAuthData.xstsAuth().toJson(xstsAuthData.xstsToken()));
            } catch (Exception e) {
                // TODO Catch the exception and let the user know?
            }

            // Skip if the cache data is blank
            if (cacheData.isBlank()) {
                continue;
            }

            // Create a bot for each credential
            BotContainer bot = botManager.addBot();
            bot.cache(cacheData);
            importedBots.add(bot);
        }

        // Start all the imported bots
        backendManager.scheduledThreadPool().execute(() -> importedBots.forEach(BotContainer::start));

        response.setStatus(200);
        return new ImportResponse(importedBots.size(), lines.length);
    }

    /**
     * Get the string contents of a zip entry
     *
     * @param zis The zip input stream
     * @return The string contents of the zip entry
     * @throws IOException If an error occurs while reading the zip entry
     */
    private static String getEntryString(ZipInputStream zis) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = zis.read(buffer)) > 0) {
            sb.append(new String(buffer, 0, read));
        }
        return sb.toString();
    }
}
