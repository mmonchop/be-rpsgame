package com.mlmo.rpsgame.repository;

import com.mlmo.rpsgame.model.Room;
import com.mlmo.rpsgame.model.enums.GameModes;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByModeAndSecondPlayerIsNullAndCreationTimeAfterOrderByCreationTimeAsc(GameModes gameMode, LocalDateTime creationTime);
}
