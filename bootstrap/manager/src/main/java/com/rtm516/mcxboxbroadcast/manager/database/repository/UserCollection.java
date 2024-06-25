package com.rtm516.mcxboxbroadcast.manager.database.repository;

import com.rtm516.mcxboxbroadcast.manager.database.model.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCollection extends MongoRepository<User, ObjectId> {
    Optional<User> findUserByUsername(String username);
}
