package com.rtm516.mcxboxbroadcast.manager.database.model;

import com.rtm516.mcxboxbroadcast.manager.models.response.UserInfoResponse;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Document("users")
public final class User implements UserDetails {
    @Id
    private ObjectId _id;
    private final String username;
    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    public UserInfoResponse toResponse() {
        return new UserInfoResponse(_id, username);
    }
}
