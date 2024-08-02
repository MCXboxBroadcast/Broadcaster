package com.rtm516.mcxboxbroadcast.manager.services;

import com.rtm516.mcxboxbroadcast.manager.database.model.User;
import com.rtm516.mcxboxbroadcast.manager.database.repository.UserCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MongoAuthUserDetailService implements UserDetailsService {
    private final UserCollection userCollection;

    @Autowired
    public MongoAuthUserDetailService(UserCollection userCollection) {
        this.userCollection = userCollection;
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        Optional<User> user = userCollection.findUserByUsername(userName);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        return user.get();
    }
}
