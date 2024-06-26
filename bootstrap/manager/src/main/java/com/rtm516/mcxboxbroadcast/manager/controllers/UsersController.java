package com.rtm516.mcxboxbroadcast.manager.controllers;

import com.rtm516.mcxboxbroadcast.manager.models.response.ErrorResponse;
import com.rtm516.mcxboxbroadcast.manager.database.model.User;
import com.rtm516.mcxboxbroadcast.manager.database.repository.UserCollection;
import com.rtm516.mcxboxbroadcast.manager.models.request.UserCreateRequest;
import com.rtm516.mcxboxbroadcast.manager.models.request.UserUpdateRequest;
import com.rtm516.mcxboxbroadcast.manager.models.response.UserInfoResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController()
@RequestMapping("/api/users")
public class UsersController {

    private final UserCollection userCollection;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UsersController(UserCollection userCollection, BCryptPasswordEncoder passwordEncoder) {
        this.userCollection = userCollection;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("")
    public List<UserInfoResponse> users(HttpServletResponse response) {
        response.setStatus(200);
        return userCollection.findAll().stream().map(User::toResponse).toList();
    }

    @PostMapping("/create")
    public ErrorResponse create(HttpServletResponse response, @RequestBody UserCreateRequest userCreateRequest) {
        if (userCreateRequest.username().isEmpty() || userCreateRequest.password().isEmpty()) {
            response.setStatus(400);
            return new ErrorResponse("Username and password must not be empty");
        }

        String username = userCreateRequest.username().toLowerCase();

        if (userCollection.findUserByUsername(username).isPresent()) {
            response.setStatus(409);
            return new ErrorResponse("User already exists");
        }

        userCollection.save(new User(username, passwordEncoder.encode(userCreateRequest.password())));

        response.setStatus(200);
        return null;
    }

    @PostMapping("/{userId:[a-z0-9]+}")
    public void password(HttpServletResponse response, @PathVariable ObjectId userId, @RequestBody UserUpdateRequest userUpdateRequest) {
        Optional<User> user = userCollection.findById(userId);
        if (user.isEmpty()) {
            response.setStatus(404);
            return;
        }

        if (userUpdateRequest.password().isEmpty()) {
            response.setStatus(400);
            return;
        }

        user.get().setPassword(passwordEncoder.encode(userUpdateRequest.password()));
        userCollection.save(user.get());

        response.setStatus(200);
    }

    @DeleteMapping("/{userId:[a-z0-9]+}")
    public void delete(HttpServletResponse response, @PathVariable ObjectId userId) {
        Optional<User> user = userCollection.findById(userId);
        if (user.isEmpty()) {
            response.setStatus(404);
            return;
        }

        // Don't let them delete the admin user or themselves
        String username = user.get().getUsername();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (username.equals("admin") || username.equals(auth.getName())) {
            response.setStatus(400);
            return;
        }

        userCollection.delete(user.get());

        response.setStatus(200);
    }
}
