package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.configs.FriendSyncConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.FriendModifyResponse;
import com.rtm516.mcxboxbroadcast.core.models.session.FollowerResponse;
import com.rtm516.mcxboxbroadcast.core.player.Player;

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
import java.util.stream.Collectors;

public class FriendManager {
    private final HttpClient httpClient;
    private final Logger logger;
    private final SessionManagerCore sessionManager;
    private final Map<String, String> toAdd;
    private final Map<String, String> toRemove;

    private Future internalScheduledFuture;

    public FriendManager(HttpClient httpClient, Logger logger, SessionManagerCore sessionManager) {
        this.httpClient = httpClient;
        this.logger = logger;
        this.sessionManager = sessionManager;

        this.toAdd = new HashMap<>();
        this.toRemove = new HashMap<>();
    }

    /**
     * Get a list of friends XUIDs
     *
     * @param includeFollowing  Include users that are following us and not full friends
     * @param includeFollowedBy Include users that we are following and not full friends
     * @return A list of {@link FollowerResponse.Person} of your friends
     * @throws XboxFriendsException If there was an error getting friends from Xbox Live
     */
    public List<FollowerResponse.Person> get(boolean includeFollowing, boolean includeFollowedBy) throws XboxFriendsException {
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
            FollowerResponse xboxFollowerResponse = Constants.OBJECT_MAPPER.readValue(lastResponse, FollowerResponse.class);

            // Parse through the returned list to make sure we are friends and
            // add them to the list to return
            for (FollowerResponse.Person person : xboxFollowerResponse.people) {
                // Make sure they are full friends
                if ((person.isFollowedByCaller && person.isFollowingCaller) || (includeFollowing && person.isFollowingCaller)) {
                    people.add(person);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.debug("Follower request response: " + lastResponse);
            throw new XboxFriendsException(e.getMessage());
        }

        if (includeFollowedBy) {
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
                FollowerResponse xboxSocialResponse = Constants.OBJECT_MAPPER.readValue(httpClient.send(xboxSocialRequest, HttpResponse.BodyHandlers.ofString()).body(), FollowerResponse.class);

                // Parse through the returned list to make sure we are following them and
                // add them to the list to return
                for (FollowerResponse.Person person : xboxSocialResponse.people) {
                    // Make sure we are following them
                    if (person.isFollowedByCaller) {
                        people.add(person);
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.debug("Social request response: " + lastResponse);
                throw new XboxFriendsException(e.getMessage());
            }
        }

        return people;
    }

    /**
     * @see #get(boolean, boolean)
     */
    public List<FollowerResponse.Person> get() throws XboxFriendsException {
        return get(false, false);
    }

    /**
     * Add a friend from xbox live
     *
     * @param xuid The XUID of the friend to add
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
     * Remove a friend from xbox live
     *
     * @param xuid The XUID of the friend to remove
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
                // Auto Friend Checker
                try {
                    List<FollowerResponse.Person> friends = get(friendSyncConfig.autoFollow(), friendSyncConfig.autoUnfollow());
                    long amount = sessionManager.socialSummary().targetFollowingCount();
                    logger.info("FRIENDS LIST: " + friends.size() + " | JUST FRIENDS: " + amount);
                    for (FollowerResponse.Person person : friends) {
                        Player player = sessionManager.getPlayer(person.xuid);
                        long lastLogOff = player.getLastLogOff();
                        long difference = /*lastLogOff <= 0 || player.getJoinTimes() >= 3 ? -1 : */System.currentTimeMillis() - (lastLogOff <= 0 ? 0 : lastLogOff);
                        if (amount >= 1000) {
                            if (difference != -1 && friendSyncConfig.unfollowTimeInDays() != -1 && difference > friendSyncConfig.unfollowTimeInDays() * 24L * 60L * 60L * 1000L) {
                                remove(person.xuid, person.displayName);
                            }
                            continue;
                        }
                        // Follow the person back
                        if (friendSyncConfig.autoFollow() && person.isFollowingCaller && !person.isFollowedByCaller) {
                            if (!(difference != -1 && friendSyncConfig.unfollowTimeInDays() != -1 && difference > friendSyncConfig.unfollowTimeInDays() * 24L * 60L * 60L * 1000L)) {
                               add(person.xuid, person.displayName);
                            }
                        }
                        // Unfollow the person
                        if (friendSyncConfig.autoUnfollow() && !person.isFollowingCaller && person.isFollowedByCaller) remove(person.xuid, person.displayName);
                    }
                } catch (XboxFriendsException e) {
                    logger.error("Failed to sync friends", e);
                }
            }, friendSyncConfig.updateInterval(), friendSyncConfig.updateInterval(), TimeUnit.SECONDS);
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
                            FriendModifyResponse modifyResponse = Constants.OBJECT_MAPPER.readValue(response.body(), FriendModifyResponse.class);
                            if (modifyResponse.code() == 1028) {
                                logger.error("Friend list full, unable to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend");
                                break;
                            }

                            logger.warning("Failed to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());
                        } else {
                            try {
                                FriendModifyResponse modifyResponse = Constants.OBJECT_MAPPER.readValue(response.body(), FriendModifyResponse.class);

                                // 1011 - The requested friend operation was forbidden.
                                // 1015 - An invalid request was attempted.
                                // 1028 - The attempted People request was rejected because it would exceed the People list limit.
                                // 1039 - Request could not be completed due to another request taking precedence.

                                if (modifyResponse.code() == 1028) {
                                    logger.error("Friend list full, unable to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend");
                                    break;
                                } else if (modifyResponse.code() == 1011) {
                                    // The friend wasn't added successfully so remove them from the list
                                    // This seems to happen in some cases, I assume from the user blocking us or having account restrictions
                                    toAdd.remove(entry.getKey());
                                    // TODO Remove these people from following us (block and unblock)
                                }
                            } catch (IOException e) {
                                // Ignore this error as it is just a fallback
                            }

                            logger.warning("Failed to add " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());
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
                            logger.warning("Failed to remove " + entry.getValue() + " (" + entry.getKey() + ") as a friend: (" + response.statusCode() + ") " + response.body());
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
}
