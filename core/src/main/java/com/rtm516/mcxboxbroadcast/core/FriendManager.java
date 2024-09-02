package com.rtm516.mcxboxbroadcast.core;

import com.google.gson.JsonParseException;
import com.rtm516.mcxboxbroadcast.core.configs.FriendSyncConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.friend.BlockRequest;
import com.rtm516.mcxboxbroadcast.core.models.friend.BlockedUsersResponse;
import com.rtm516.mcxboxbroadcast.core.models.friend.FriendModifyResponse;
import com.rtm516.mcxboxbroadcast.core.models.friend.FriendStatusResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.FollowerResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FriendManager {
    private final HttpClient httpClient;
    private final Logger logger;
    private final SessionManagerCore sessionManager;
    private final Map<String, String> toAdd;
    private final Map<String, String> toRemove;

    private List<FollowerResponse.Person> lastFriendCache;
    private Future<?> internalScheduledFuture;

    public FriendManager(HttpClient httpClient, Logger logger, SessionManagerCore sessionManager) {
        this.httpClient = httpClient;
        this.logger = logger;
        this.sessionManager = sessionManager;

        this.toAdd = new HashMap<>();
        this.toRemove = new HashMap<>();

        this.lastFriendCache = new ArrayList<>();
    }

    /**
     * Get a list of friends XUIDs
     *
     * @return A list of {@link FollowerResponse.Person} of your friends
     * @throws XboxFriendsException If there was an error getting friends from Xbox Live
     */
    public List<FollowerResponse.Person> get() throws XboxFriendsException {
        List<FollowerResponse.Person> people = new ArrayList<>();

        // Create the request for getting the people following us and friends
        HttpRequest xboxFollowersRequest = HttpRequest.newBuilder()
            .uri(Constants.FOLLOWERS)
            .header("Authorization", sessionManager.getTokenHeader())
            .header("x-xbl-contract-version", "5")
            .header("accept-language", "en-GB")
            .GET()
            .build();

        String lastResponse = "";
        try {
            // Get the list of friends from the api
            lastResponse = httpClient.send(xboxFollowersRequest, HttpResponse.BodyHandlers.ofString()).body();

            // We sometimes get an empty response so don't try and parse it
            if (!lastResponse.isEmpty()) {
                FollowerResponse xboxFollowerResponse = Constants.GSON.fromJson(lastResponse, FollowerResponse.class);

                if (xboxFollowerResponse.people != null) {
                    people.addAll(xboxFollowerResponse.people);
                }
            }
        } catch (JsonParseException | IOException | InterruptedException e) {
            logger.debug("Follower request response: " + lastResponse);
            throw new XboxFriendsException(e.getMessage());
        }

        // Create the request for getting the people we are following and friends
        HttpRequest xboxSocialRequest = HttpRequest.newBuilder()
            .uri(Constants.SOCIAL)
            .header("Authorization", sessionManager.getTokenHeader())
            .header("x-xbl-contract-version", "5")
            .header("accept-language", "en-GB")
            .GET()
            .build();

        try {
            // Get the list of people we are following from the api
            lastResponse = httpClient.send(xboxSocialRequest, HttpResponse.BodyHandlers.ofString()).body();

            // We sometimes get an empty response so don't try and parse it
            if (!lastResponse.isEmpty()) {
                FollowerResponse xboxSocialResponse = Constants.GSON.fromJson(lastResponse, FollowerResponse.class);

                if (xboxSocialResponse.people != null) {
                    people.addAll(xboxSocialResponse.people);
                }
            }
        } catch (JsonParseException | IOException | InterruptedException e) {
            logger.debug("Social request response: " + lastResponse);
            throw new XboxFriendsException(e.getMessage());
        }

        // Merge the 2 lists together
        Map<String, FollowerResponse.Person> outPeople = new HashMap<>();
        for (FollowerResponse.Person person : people) {
            if (outPeople.containsKey(person.xuid)) {
                outPeople.put(person.xuid, outPeople.get(person.xuid).merge(person));
            } else {
                outPeople.put(person.xuid, person);
            }
        }

        List<FollowerResponse.Person> outPeopleList = outPeople.values().stream().toList();
        lastFriendCache = outPeopleList;
        return outPeopleList;
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
        internalProcess();
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
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(xboxFriendStatus, HttpResponse.BodyHandlers.ofString());
            FriendStatusResponse modifyResponse = Constants.GSON.fromJson(response.body(), FriendStatusResponse.class);

            if (modifyResponse.isFollowingCaller() && modifyResponse.isFollowedByCaller()) {
                return false;
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
        // Remove the user from the add list (if they are on it)
        toAdd.remove(xuid);

        // Add the user to the remove list
        toRemove.put(xuid, gamertag);

        // Process the add/remove requests
        internalProcess();
    }

    /**
     * Set up a scheduled task to automatically follow/unfollow friends
     *
     * @param friendSyncConfig The config to use for the auto friend sync
     */
    public void initAutoFriend(FriendSyncConfig friendSyncConfig) {
        if (friendSyncConfig.autoFollow() || friendSyncConfig.autoUnfollow()) {
            sessionManager.scheduledThread().scheduleWithFixedDelay(() -> {
                // Cleanup any blocked users
                cleanupBlocked();

                try {
                    for (FollowerResponse.Person person : get()) {
                        // Make sure we are not targeting a subaccount (eg: split screen)
                        if (isSubAccount(person.xuid)) {
                            continue;
                        }

                        // Follow the person back
                        if (friendSyncConfig.autoFollow() && person.isFollowingCaller && !person.isFollowedByCaller) {
                            add(person.xuid, person.displayName);
                        }

                        // Unfollow the person
                        if (friendSyncConfig.autoUnfollow() && !person.isFollowingCaller && person.isFollowedByCaller) {
                            remove(person.xuid, person.displayName);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to sync friends", e);
                }
            }, friendSyncConfig.updateInterval(), friendSyncConfig.updateInterval(), TimeUnit.SECONDS);
        }
    }

    /**
     * Internal function to check if the XUID is a subaccount (used by split screen)
     *
     * @return True if the XUID is a sub account
     */
    private boolean isSubAccount(long xuid) {
        return xuid >> 52 == 1;
    }

    /**
     * @see #isSubAccount(long)
     */
    private boolean isSubAccount(String xuid) {
        try {
            return isSubAccount(Long.parseLong(xuid));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Internal function to process the add/remove requests
     * This will also handle retrying requests if they fail due to rate limits or other errors
     */
    private void internalProcess() {
        // If we are already running then don't run again
        if (internalScheduledFuture != null && !internalScheduledFuture.isDone()) {
            return;
        }

        internalScheduledFuture = sessionManager.scheduledThread().submit(() -> {
            int retryAfter = 0;

            // If we have friends to add then add them
            if (!toAdd.isEmpty()) {
                // Create a copy of the list to iterate over, so we don't get a concurrent modification exception
                Map<String, String> toProcess = new HashMap<>(toAdd);
                for (Map.Entry<String, String> entry : toProcess.entrySet()) {
                    // Create the request for adding the friend
                    HttpRequest xboxFriendRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Constants.PEOPLE.formatted(entry.getKey())))
                        .header("Authorization", sessionManager.getTokenHeader())
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();

                    try {
                        HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == 204) {
                            // The friend was added successfully so remove them from the list
                            toAdd.remove(entry.getKey());

                            // Let the user know we added a friend
                            logger.info("Added " + entry.getValue() + " (" + entry.getKey() + ") as a friend");

                            // Update the user in the cache
                            Optional<FollowerResponse.Person> friend = lastFriendCache.stream().filter(p -> p.xuid.equals(entry.getKey())).findFirst();
                            friend.ifPresent(person -> person.isFollowedByCaller = true);
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
                        } else if (response.statusCode() == 400) {
                            FriendModifyResponse modifyResponse = Constants.GSON.fromJson(response.body(), FriendModifyResponse.class);
                            if (modifyResponse.code() == 1028) {
                                logger.error("Friend list full, unable to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend");
                                break;
                            }

                            logger.warn("Failed to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());
                        } else {
                            FriendModifyResponse modifyResponse = Constants.GSON.fromJson(response.body(), FriendModifyResponse.class);

                            // 1011 - The requested friend operation was forbidden.
                            // 1015 - An invalid request was attempted.
                            // 1028 - The attempted People request was rejected because it would exceed the People list limit.
                            // 1039 - Request could not be completed due to another request taking precedence.
                            // 1049 - Target user privacy settings do not allow friend requests to be received.

                            if (modifyResponse.code() == 1028) {
                                logger.error("Friend list full, unable to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend");
                            } else if (modifyResponse.code() == 1011 || modifyResponse.code() == 1049) {
                                // The friend wasn't added successfully so remove them from the list
                                // This seems to happen in some cases, I assume from the user blocking us or having account restrictions
                                toAdd.remove(entry.getKey());

                                // Remove these people from following us (block and unblock)
                                forceUnfollow(entry.getKey());

                                logger.warn("Removed " + entry.getValue() + " (" + entry.getKey() + ") as a friend due to restrictions on their account");
                                sessionManager.slackNotificationManager().sendFriendRestrictionNotification(entry.getValue(), entry.getKey());
                            } else {
                                logger.warn("Failed to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());
                            }
                        }
                    } catch (IOException | InterruptedException e) {
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
                        .uri(URI.create(Constants.PEOPLE.formatted(entry.getKey())))
                        .header("Authorization", sessionManager.getTokenHeader())
                        .DELETE()
                        .build();

                    try {
                        HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == 204) {
                            // The friend was removed successfully so remove them from the list
                            toRemove.remove(entry.getKey());

                            // Let the user know we added a friend
                            logger.info("Removed " + entry.getValue() + " (" + entry.getKey() + ") as a friend");

                            // Update the user in the cache
                            Optional<FollowerResponse.Person> friend = lastFriendCache.stream().filter(p -> p.xuid.equals(entry.getKey())).findFirst();
                            friend.ifPresent(person -> person.isFollowedByCaller = false);
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
        });
    }

    /**
     * Force a user to unfollow us
     * This works by blocking and unblocking them
     *
     * @param xuid The XUID of the user to target
     */
    public void forceUnfollow(String xuid) {
        try {
            block(xuid);

            try {
                // Wait 2.5s else the unblock request will not get processed
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                logger.warn("Failed to wait to unblock user, this may cause issues (" + xuid + "): " + e.getMessage());
            }

            unblock(xuid);

            // Remove the user from the cache
            lastFriendCache.removeIf(person -> person.xuid.equals(xuid));
        } catch (Exception e) {
            logger.error("Failed to force unfollow user", e);
        }
    }

    private void block(String xuid) throws IOException, InterruptedException, RuntimeException {
        HttpRequest blockRequest = HttpRequest.newBuilder()
            .uri(Constants.BLOCK)
            .header("Authorization", sessionManager.getTokenHeader())
            .PUT(HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(new BlockRequest(xuid))))
            .build();

        HttpResponse<Void> blockResponse = httpClient.send(blockRequest, HttpResponse.BodyHandlers.discarding());
        if (blockResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to block user: " + blockResponse.statusCode());
        }
    }

    private void unblock(String xuid) throws IOException, InterruptedException, RuntimeException {
        HttpRequest unblockRequest = HttpRequest.newBuilder()
            .uri(Constants.BLOCK)
            .header("Authorization", sessionManager.getTokenHeader())
            .method("DELETE", HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(new BlockRequest(xuid))))
            .build();

        HttpResponse<Void> unblockResponse = httpClient.send(unblockRequest, HttpResponse.BodyHandlers.discarding());
        if (unblockResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to unblock user: " + unblockResponse.statusCode());
        }
    }

    /**
     * Cleanup any blocked users
     *
     * This is due to xbox taking time to process block requests and sometimes we are too early to unblock
     */
    private void cleanupBlocked() {
        try {
            HttpRequest blockedUsers = HttpRequest.newBuilder()
                .uri(Constants.BLOCK)
                .header("Authorization", sessionManager.getTokenHeader())
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(blockedUsers, HttpResponse.BodyHandlers.ofString());
            BlockedUsersResponse blockedUsersResponse = Constants.GSON.fromJson(response.body(), BlockedUsersResponse.class);

            for (BlockedUsersResponse.User user : blockedUsersResponse.users()) {
                try {
                    unblock(user.xuid());
                    logger.info("Unblocked " + user.xuid() + " as they were blocked previously");
                } catch (Exception e) {
                    // Silently continue
                }
            }
        } catch (IOException | InterruptedException e) {
            // Silently fail as it's not too important if this doesn't work
        }
    }

    /**
     * Get the last friend cache
     * This is the list of friends we got from the last get request
     *
     * @return The last friend cache
     */
    public List<FollowerResponse.Person> lastFriendCache() {
        return lastFriendCache;
    }
}
