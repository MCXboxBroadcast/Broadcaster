package com.rtm516.mcxboxbroadcast.manager.database.repository;

import com.rtm516.mcxboxbroadcast.manager.database.model.Server;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerCollection extends MongoRepository<Server, ObjectId> {
}
