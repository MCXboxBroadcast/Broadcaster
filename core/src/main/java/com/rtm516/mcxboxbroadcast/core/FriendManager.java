package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.configs.CoreConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.friend.FriendAddResponse;
import com.rtm516.mcxboxbroadcast.core.models.friend.FriendModifyResponse;
import com.rtm516.mcxboxbroadcast.core.models.friend.FriendStatusResponse;
import com.rtm516.mcxboxbroadcast.core.models.friend.PeopleResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.CreateHandleRequest;
import com.rtm516.mcxboxbroadcast.core.models.session.SessionRef;
import com.rtm516.mcxboxbroadcast.core.storage.StorageManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FriendManager {
    private final HttpClient httpClient;
    private final Logger logger;
    private final SessionManagerCore sessionManager;
    private final Map<String, String> toAdd;
    private final Map<String, String> toRemove;

    private List<PeopleResponse.Person> lastFriendCache;
    private Future<?> internalScheduledFuture;
    private boolean initialInvite;
    private boolean autoFriend = true;

    public FriendManager(HttpClient httpClient, Logger logger, SessionManagerCore sessionManager) {
        this.httpClient = httpClient;
        this.logger = logger;
        this.sessionManager = sessionManager;

        this.toAdd = new ConcurrentHashMap<>();
        this.toRemove = new ConcurrentHashMap<>();
    }

    /**
     * Get the list of friends
     *
     * @return A list of {@link PeopleResponse.Person} of your friends
     * @throws XboxFriendsException If there was an error getting friends from Xbox Live
     */
    public List<PeopleResponse.Person> get() throws XboxFriendsException {
        List<PeopleResponse.Person> people = new ArrayList<>();

        // Create the request for getting our friends
        HttpRequest xboxFriendsRequest = HttpRequest.newBuilder()
            .uri(Constants.FRIENDS)
            .header("Authorization", sessionManager.getTokenHeader())
            .header("x-xbl-contract-version", "7")
            .header("accept-language", "en-GB")
            .GET()
            .build();

        String lastResponse = "";
        try {
            // Get the list of friends from the api
            lastResponse = httpClient.send(xboxFriendsRequest, HttpResponse.BodyHandlers.ofString()).body();

            // We sometimes get an empty response so don't try and parse it
            if (!lastResponse.isEmpty()) {
                PeopleResponse xboxFriendsResponse = Constants.GSON.fromJson(lastResponse, PeopleResponse.class);

                if (xboxFriendsResponse.people != null) {
                    people.addAll(xboxFriendsResponse.people);
                }
            }
        } catch (JsonParseException | IOException | InterruptedException e) {
            logger.debug("Friends request response: " + lastResponse);
            throw new XboxFriendsException(e.getMessage());
        }

        lastFriendCache = people;
        return people;
    }

    /**
     * Add a friend from xbox live
     *
     * @param xuid The XUID of the friend to add
     * @param gamertag The gamertag of the friend to add
     */
    public void add(String xuid, String gamertag) {
        // Remove the user from the remove list (if they are on it)
        toRemove.remove(xuid);

        // Add the user to the add list
        toAdd.put(xuid, gamertag);

        // Process the add/remove requests
        callInternalProcess();
    }

    /**
     * Add a friend from xbox live if they aren't already a friend
     *
     * @param xuid The XUID of the friend to add
     * @param gamertag The gamertag of the friend to add
     * @return True if the friend was added, false if they are already a friend
     */
    public boolean addIfRequired(String xuid, String gamertag) {
        // Check if they are already in the list to be added
        if (toAdd.containsKey(xuid)) {
            return false;
        }

        // Check if we are already friends
        HttpRequest xboxFriendStatus = HttpRequest.newBuilder()
            .uri(URI.create(Constants.PEOPLE.formatted(xuid)))
            .header("Authorization", sessionManager.getTokenHeader())
            .header("x-xbl-contract-version", "3")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(xboxFriendStatus, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                FriendStatusResponse statusResponse = Constants.GSON.fromJson(response.body(), FriendStatusResponse.class);

                if (statusResponse.isFriend()) {
                    return false;
                }
            }
        } catch (JsonParseException | InterruptedException | IOException e) {
            // Debug log it failed and assume we aren't friends
            logger.debug("Failed to check if " + gamertag + " (" + xuid + ") is a friend: " + e.getMessage());
        }

        add(xuid, gamertag);
        return true;
    }

    /**
     * Remove a friend from xbox live
     *
     * @param xuid The XUID of the friend to remove
     * @param gamertag The gamertag of the friend to remove
     */
    public void remove(String xuid, String gamertag) {
        // Try and get the gamertag from the cache if it wasn't provided
        if (gamertag == null) {
            Optional<PeopleResponse.Person> foundFriend = lastFriendCache().stream().filter(person -> person.xuid.equals(xuid)).findFirst();
            if (foundFriend.isPresent()) {
                gamertag = foundFriend.get().gamertag;
            } else {
                gamertag = "Unknown";
            }
        }

        // Remove the user from the add list (if they are on it)
        toAdd.remove(xuid);

        // Add the user to the remove list
        toRemove.put(xuid, gamertag);

        // Process the add/remove requests
        callInternalProcess();
    }

    public void init(CoreConfig.FriendSyncConfig friendSyncConfig) {
        autoFriend = friendSyncConfig.autoFriend();

        // Initialize the auto friend sync if enabled
        initAutoFriend(friendSyncConfig);

        // Accept any pending friend requests if enabled incase we got any while offline
        acceptPendingFriendRequests();

        if (!friendSyncConfig.expiry().enabled()) return;

        StorageManager.PlayerHistoryStorage playerHistory = sessionManager.storageManager().playerHistory();
        if (playerHistory.isFirstRun()) {
            logger.info("Player history is being initialized for the first time, this may take a few seconds");
            try {
                for (PeopleResponse.Person friend : get()) {
                    playerHistory.lastSeen(friend.xuid, Instant.now());
                }
            } catch (Exception e) {
                logger.error("Failed to initialize player history", e);
            }
        } else {
            try {
                Set<String> friendXuids = lastFriendCache().stream().map(person -> person.xuid).collect(Collectors.toUnmodifiableSet());
                Set<String> historyXuids = playerHistory.all().keySet();

                // Remove any players from history that are no longer friends
                for (String xuid : historyXuids) {
                    if (!friendXuids.contains(xuid)) {
                        playerHistory.clear(xuid);
                    }
                }

                // Add any friends that are missing from history
                for (String xuid : friendXuids) {
                    if (!historyXuids.contains(xuid)) {
                        playerHistory.lastSeen(xuid, Instant.now());
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to clean up player history", e);
            }
        }

        sessionManager.scheduledThread().scheduleWithFixedDelay(() -> {
            try {
                for (Map.Entry<String, Instant> entry : playerHistory.all().entrySet()) {
                    String xuid = entry.getKey();
                    Instant lastSeen = entry.getValue();

                    if (lastSeen.isBefore(Instant.now().minusSeconds(TimeUnit.DAYS.toSeconds(friendSyncConfig.expiry().days())))) {
                        try {
                            logger.info("Removing player " + xuid + " from friends due to inactivity");
                            remove(xuid, null);
                        } catch (Exception e) {
                            if (e.getMessage().startsWith("429: ")) {
                                logger.warn("Rate limited while trying to remove player " + xuid + " from friends for inactivity, will try again later");
                                return;
                            }
                            logger.error("Failed to remove player " + xuid + " from friends for inactivity", e);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to clean up friends list", e);
            }
        }, 10, friendSyncConfig.expiry().check(), TimeUnit.SECONDS);
    }

    /**
     * Set up a scheduled task to accept friend requests
     *
     * @param friendSyncConfig The config to use for the auto friend sync
     */
    private void initAutoFriend(CoreConfig.FriendSyncConfig friendSyncConfig) {
        this.initialInvite = friendSyncConfig.initialInvite();
        if (friendSyncConfig.autoFriend()) {
            sessionManager.scheduledThread().scheduleWithFixedDelay(this::acceptPendingFriendRequests, friendSyncConfig.updateInterval(), friendSyncConfig.updateInterval(), TimeUnit.SECONDS);
        }
    }

    /**
     * Internal function to check if the XUID is a guest account (used by split screen)
     *
     * @return True if the XUID is a guest account
     */
    private boolean isGuestAccount(long xuid) {
        return xuid >> 52 == 1;
    }

    /**
     * @see #isGuestAccount(long)
     */
    private boolean isGuestAccount(String xuid) {
        try {
            return isGuestAccount(Long.parseLong(xuid));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Calls the internal process to handle adding/removing friends if it isn't already running
     */
    private void callInternalProcess() {
        // If we are already running then don't run again
        if (internalScheduledFuture != null && !internalScheduledFuture.isDone()) {
            return;
        }

        internalScheduledFuture = sessionManager.scheduledThread().submit(this::internalProcess);
    }

    /**
     * Internal function to process the add/remove requests
     * This will also handle retrying requests if they fail due to rate limits or other errors
     */
    private void internalProcess() {
        int retryAfter = 0;

        // Initialize the cache if it is null
        if (lastFriendCache == null) lastFriendCache = new ArrayList<>();

        // If we have friends to add then add them
        if (!toAdd.isEmpty()) {
            // Create a copy of the list to iterate over, so we don't get a concurrent modification exception
            Map<String, String> toProcess = new HashMap<>(toAdd);
            for (Map.Entry<String, String> entry : toProcess.entrySet()) {
                // Create the request for adding the friend
                HttpRequest xboxFriendRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Constants.FRIEND.formatted(entry.getKey())))
                        .header("Authorization", sessionManager.getTokenHeader())
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();

                try {
                    HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        // The request was successful so remove them from the list
                        toAdd.remove(entry.getKey());

                        FriendAddResponse addResponse = Constants.GSON.fromJson(response.body(), FriendAddResponse.class);
                        if (addResponse.isFriend) {
                            // Let the user know we added a friend
                            logger.info("Added " + entry.getValue() + " (" + entry.getKey() + ") as a friend");
                            sendInvite(entry.getKey());

                            // Add the user to the cache
                            if (lastFriendCache.stream().noneMatch(p -> p.xuid.equals(entry.getKey()))) {
                                PeopleResponse.Person friend = new PeopleResponse.Person();
                                friend.xuid = entry.getKey();
                                friend.gamertag = entry.getValue();
                                friend.isFriend = true;
                                lastFriendCache.add(friend);
                            }
                        } else {
                            // They hadn't sent us a request so we sent them one
                            logger.info("Sent a friend request to " + entry.getValue() + " (" + entry.getKey() + ")");
                        }
                    } else if (response.statusCode() == 429) {
                        // The friend wasn't added successfully so get the retry after header
                        Optional<String> header = response.headers().firstValue("Retry-After");
                        if (header.isPresent()) {
                            retryAfter = Integer.parseInt(header.get());
                        }

                        // Log the error
                        logger.debug("Failed to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());

                        // Break out of the loop, so we don't try to add more friends
                        break;
                    } else {
                        FriendModifyResponse modifyResponse = Constants.GSON.fromJson(response.body(), FriendModifyResponse.class);

                        // 1011 - The requested friend operation was forbidden.
                        // 1015 - An invalid request was attempted.
                        // 1028 - The attempted People request was rejected because it would exceed the People list limit.
                        // 1039 - Request could not be completed due to another request taking precedence.
                        // 1049 - Target user privacy settings do not allow friend requests to be received.

                        if (modifyResponse != null && modifyResponse.code() == 1028) {
                            logger.error("Friend list full, unable to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend");

                            // Nothing else can be added so clear the list
                            toAdd.clear();
                            break;
                        } else if (modifyResponse != null && (modifyResponse.code() == 1011 || modifyResponse.code() == 1049)) {
                            // The friend wasn't added successfully so remove them from the list
                            // This seems to happen in some cases, I assume from the user blocking us or having account restrictions
                            toAdd.remove(entry.getKey());

                            // Decline their friend request so we don't keep trying to accept it
                            try {
                                declineFriendRequest(entry.getKey());
                            } catch (Exception e) {
                                logger.debug("Failed to decline friend request from " + entry.getValue() + " (" + entry.getKey() + "): " + e.getMessage());
                            }

                            logger.warn("Unable to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend due to restrictions on their account");
                            sessionManager.notificationManager().sendFriendRestrictionNotification(entry.getValue(), entry.getKey());
                        } else {
                            logger.warn("Failed to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());
                        }
                    }
                } catch (JsonParseException | IOException | InterruptedException e) {
                    logger.error("Failed to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend: " + e.getMessage());
                    break;
                }
            }
        }

        // If we have friends to remove then remove them
        // Note: This can be run even if add hits the rate limit as it seems to be separate
        if (!toRemove.isEmpty()) {
            // Create a copy of the list to iterate over, so we don't get a concurrent modification exception
            Map<String, String> toProcess = new HashMap<>(toRemove);
            for (Map.Entry<String, String> entry : toProcess.entrySet()) {
                // Create the request for removing the friend
                HttpRequest xboxFriendRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Constants.FRIEND.formatted(entry.getKey())))
                        .header("Authorization", sessionManager.getTokenHeader())
                        .DELETE()
                        .build();

                try {
                    HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        // The friend was removed successfully so remove them from the list
                        toRemove.remove(entry.getKey());

                        // Let the user know we removed a friend
                        logger.info("Removed " + entry.getValue() + " (" + entry.getKey() + ") as a friend");

                        sessionManager.storageManager().playerHistory().clear(entry.getKey());

                        // Remove the user from the cache
                        lastFriendCache.removeIf(p -> p.xuid.equals(entry.getKey()));
                    } else if (response.statusCode() == 429) {
                        // The friend wasn't removed successfully so get the retry after header
                        Optional<String> header = response.headers().firstValue("Retry-After");
                        if (header.isPresent()) {
                            retryAfter = Integer.parseInt(header.get());
                        }

                        // Log the error
                        logger.debug("Failed to remove " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());

                        // Break out of the loop, so we don't try to remove more friends
                        break;
                    } else {
                        logger.warn("Failed to remove " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());
                    }
                } catch (IOException | InterruptedException e) {
                    logger.error("Failed to remove " + entry.getValue() + " (" + entry.getKey() + ") as a friend: " + e.getMessage());
                    break;
                }
            }
        }

        // If we still have friends to add or remove then schedule another run after the retry after time
        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
            internalScheduledFuture = sessionManager.scheduledThread().schedule(this::internalProcess, retryAfter, TimeUnit.SECONDS);
        }
    }

    /**
     * Decline a friend request from a user
     *
     * @param xuid The XUID of the user to target
     */
    private void declineFriendRequest(String xuid) throws Exception {
        HttpRequest declineRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.FRIEND.formatted(xuid)))
            .header("Authorization", sessionManager.getTokenHeader())
            .DELETE()
            .build();

        HttpResponse<String> response = httpClient.send(declineRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new RuntimeException(response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Get the last friend cache
     * This is the list of friends we got from the last get request
     *
     * @return The last friend cache
     */
    public List<PeopleResponse.Person> lastFriendCache() {
        if (lastFriendCache == null) {
            try {
                // If the cache is empty then get the current friends from Xbox Live
                lastFriendCache = get();
            } catch (XboxFriendsException e) {
                logger.error("Failed to get friends from Xbox Live", e);
                lastFriendCache = new ArrayList<>();
            }
        }

        return lastFriendCache;
    }

    /**
     * Accept any pending friend requests if auto friend is enabled
     */
    public void acceptPendingFriendRequests() {
        if (!autoFriend) {
            return;
        }

        try {
            // Get the pending friend requests
            HttpRequest friendRequests = HttpRequest.newBuilder()
                .uri(Constants.FRIEND_REQUESTS)
                .header("Authorization", sessionManager.getTokenHeader())
                .header("x-xbl-contract-version", "7")
                .header("accept-language", "en-GB")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(friendRequests, HttpResponse.BodyHandlers.ofString());
            PeopleResponse friendRequestResponse = Constants.GSON.fromJson(response.body(), PeopleResponse.class);

            // We got no pending friend requests returned
            if (friendRequestResponse == null || friendRequestResponse.people == null) {
                return;
            }

            // Add them through the normal process to handle rate limits
            for (PeopleResponse.Person person : friendRequestResponse.people) {
                // Make sure we are not targeting a subaccount (eg: split screen)
                if (isGuestAccount(person.xuid)) {
                    continue;
                }

                if (!toAdd.containsKey(person.xuid)) {
                    add(person.xuid, person.gamertag);
                }
            }
        } catch (JsonParseException | IOException | InterruptedException e) {
            logger.error("Failed to accept friend requests", e);
        }
    }

    /**
     * Send an invite to a given xuid for the current game session
     *
     * @param xuid The XUID of the user to invite
     */
    public void sendInvite(String xuid) {
        // Only invite if enabled
        if (!initialInvite) {
            return;
        }

        try {
            CreateHandleRequest createHandleContent = new CreateHandleRequest(
                1,
                "invite",
                new SessionRef(
                    Constants.SERVICE_CONFIG_ID,
                    Constants.TEMPLATE_NAME,
                    sessionManager.getSessionId()
                ),
                xuid,
                Map.of("titleId", Constants.TITLE_ID) // Minecraft Windows title Id
            );

            HttpRequest sendInvite = HttpRequest.newBuilder()
                .uri(Constants.CREATE_HANDLE)
                .header("Authorization", sessionManager.getTokenHeader())
                .header("x-xbl-contract-version", "107")
                .POST(HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(createHandleContent)))
                .build();

            HttpResponse<String> inviteResponse = httpClient.send(sendInvite, HttpResponse.BodyHandlers.ofString());
            logger.debug(inviteResponse.body());
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to send invite to " + xuid + ": " + e.getMessage());
        }
    }
}
