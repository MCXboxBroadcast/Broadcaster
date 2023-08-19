package com.rtm516.mcxboxbroadcast.core;

import com.rtm516.mcxboxbroadcast.core.configs.FriendSyncConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.XboxFriendsException;
import com.rtm516.mcxboxbroadcast.core.models.FollowerResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FriendManager {
    private final HttpClient httpClient;
    private final Logger logger;
    private final SessionManagerCore sessionManager;

    public FriendManager(HttpClient httpClient, Logger logger, SessionManagerCore sessionManager) {
        this.httpClient = httpClient;
        this.logger = logger;
        this.sessionManager = sessionManager;
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
                if ((person.isFollowedByCaller && person.isFollowingCaller)
                    || (includeFollowing && person.isFollowingCaller)) {
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
     * @return If the request was successful, this will be true even if the user is already your friend, false if something goes wrong
     */
    public boolean add(String xuid) {
        HttpRequest xboxFriendRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.PEOPLE.formatted(xuid)))
            .header("Authorization", sessionManager.getTokenHeader())
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

        try {
            HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            logger.debug("Failed to add friend: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove a friend from xbox live
     *
     * @param xuid The XUID of the friend to remove
     * @return If the request was successful, this will be true even if the user isn't your friend, false if something goes wrong
     */
    public boolean remove(String xuid) {
        HttpRequest xboxFriendRequest = HttpRequest.newBuilder()
            .uri(URI.create(Constants.PEOPLE.formatted(xuid)))
            .header("Authorization", sessionManager.getTokenHeader())
            .DELETE()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(xboxFriendRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            logger.debug("Failed to remove friend: " + e.getMessage());
            return false;
        }
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
                    for (FollowerResponse.Person person : get(friendSyncConfig.autoFollow(), friendSyncConfig.autoUnfollow())) {
                        // Follow the person back
                        if (friendSyncConfig.autoFollow() && person.isFollowingCaller && !person.isFollowedByCaller) {
                            if (add(person.xuid)) {
                                logger.info("Added " + person.displayName + " (" + person.xuid + ") as a friend");
                            } else {
                                logger.warning("Failed to add " + person.displayName + " (" + person.xuid + ") as a friend");
                            }
                        }

                        // Unfollow the person
                        if (friendSyncConfig.autoUnfollow() && !person.isFollowingCaller && person.isFollowedByCaller) {
                            if (remove(person.xuid)) {
                                logger.info("Removed " + person.displayName + " (" + person.xuid + ") as a friend");
                            } else {
                                logger.warning("Failed to remove " + person.displayName + " (" + person.xuid + ") as a friend");
                            }
                        }
                    }
                } catch (XboxFriendsException e) {
                    logger.error("Failed to sync friends", e);
                }
            }, friendSyncConfig.updateInterval(), friendSyncConfig.updateInterval(), TimeUnit.SECONDS);
        }
    }
}
