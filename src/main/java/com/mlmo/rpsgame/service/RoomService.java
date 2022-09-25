package com.mlmo.rpsgame.service;

import com.mlmo.rpsgame.exception.EntityNotFoundException;
import com.mlmo.rpsgame.mapper.RoomMapper;
import com.mlmo.rpsgame.model.Game;
import com.mlmo.rpsgame.model.Player;
import com.mlmo.rpsgame.model.Room;
import com.mlmo.rpsgame.model.dto.RoomDto;
import com.mlmo.rpsgame.model.enums.GameModes;
import com.mlmo.rpsgame.model.enums.GameStates;
import com.mlmo.rpsgame.model.enums.NotificationEventType;
import com.mlmo.rpsgame.repository.RoomRepository;
import com.mlmo.rpsgame.service.notification.NotificationService;
import lombok.extern.java.Log;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.mlmo.rpsgame.model.enums.GameStates.*;
import static com.mlmo.rpsgame.model.enums.NotificationEventType.INVITATION_ACCEPTED;
import static com.mlmo.rpsgame.model.enums.PlayerStates.READY;

@Log
@Service
public class RoomService {

    private final String machinePlayerName;
    private final int maxWaitRandomPlayerMinutes;
    private final String roomNotificationsTopicPattern;

    private final RoomRepository roomRepository;
    private final MetricsService metricsService;
    private final NotificationService notificationService;
    private final RoomMapper roomMapper;

    @Autowired
    public RoomService(RoomRepository roomRepository,
                       MetricsService metricsService,
                       NotificationService notificationService,
                       RoomMapper roomMapper,
                       @Value("${rpsgame.max-wait-random-player-minutes}") int maxWaitRandomPlayerMinutes,
                       @Value("${rpsgame.machine-player-name}") String machinePlayerName,
                       @Value("${rpsgame.notifications.stomp.rooms-topic-pattern}") String roomNotificationsTopicPattern) {
        this.roomMapper = roomMapper;
        this.roomRepository = roomRepository;
        this.metricsService = metricsService;
        this.notificationService = notificationService;
        this.machinePlayerName = machinePlayerName;
        this.maxWaitRandomPlayerMinutes = maxWaitRandomPlayerMinutes;
        this.roomNotificationsTopicPattern = roomNotificationsTopicPattern;
    }

    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Room [%s] NOT FOUND", roomId)));
    }

    @Transactional
    public Room createRoom(String firstPlayerName, GameModes gameMode) {
        Player firstPlayer = new Player(ObjectId.get().toString(), firstPlayerName, READY);

        return switch (gameMode) {
            case VS_FRIEND -> createRoom(firstPlayer, null, gameMode, WAITING);

            case VS_MACHINE -> {
                Player secondPlayer = new Player(ObjectId.get().toString(), machinePlayerName, READY);
                Room room = createRoom(firstPlayer, secondPlayer, gameMode, ACCEPTED);
                yield room;
            }

            case VS_RANDOM_PLAYER -> joinAvailableRoom(firstPlayer, gameMode);

            default -> null;
        };
    }

    @Transactional
    public Room acceptRoomInvitation(String roomId, String secondPlayerName) {
        Player secondPlayer = new Player(ObjectId.get().toString(), secondPlayerName, READY);

        Room room = getRoom(roomId);
        room.getGame(1).setState(ACCEPTED);
        room.setSecondPlayer(secondPlayer);
        room.setInvitationAcceptedTime(LocalDateTime.now(ZoneOffset.UTC));

        metricsService.addRoomInvitationsAcceptedMetrics(room);

        log.info(String.format("Player %s ACCEPTED invitation and joined room [%s][mode: %s]",
                secondPlayer.getName(), room.getId(), room.getMode()));
        return updateRoomAndSendNotification(room, secondPlayer.getId(), INVITATION_ACCEPTED);
    }

    public Room updateRoomAndSendNotification(Room room, String playerId, NotificationEventType notificationEventType) {
        sendRoomNotificationEvent(room, playerId, notificationEventType);
        return roomRepository.save(room);
    }

    public void sendRoomNotificationEvent(Room room, String playerId, NotificationEventType type) {
        Map<String, Object> data = new HashMap<>();
        data.put(RoomDto.class.getSimpleName(), roomMapper.roomToDto(room));
        data.put("PlayerId", playerId);

        String destination = String.format(roomNotificationsTopicPattern, room.getId());
        notificationService.sendNotification(destination, type, room.getId(), data);
    }

    private Room joinAvailableRoom(Player firstPlayer, GameModes gameMode) {
        LocalDateTime timeAgoLimit = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(maxWaitRandomPlayerMinutes);
        List<Room> availableRooms = roomRepository.findByModeAndSecondPlayerIsNullAndCreationTimeAfterOrderByCreationTimeAsc(gameMode, timeAgoLimit);

        return availableRooms.stream()
                .findFirst()
                .map(availableRoom -> {
                    Player secondPlayer = new Player(ObjectId.get().toString(), firstPlayer.getName(), READY);
                    availableRoom.setSecondPlayer(secondPlayer);
                    availableRoom.setInvitationAcceptedTime(LocalDateTime.now(ZoneOffset.UTC));
                    availableRoom.getGame(1).setState(ACCEPTED);
                    sendRoomNotificationEvent(availableRoom, firstPlayer.getId(), INVITATION_ACCEPTED);

                    log.info(String.format("Player %s JOINED successfully room [%s][mode: %s] created by %s [id: %s]", secondPlayer.getName(),
                            availableRoom.getId(), gameMode, availableRoom.getFirstPlayer().getName(), availableRoom.getFirstPlayer().getId()));

                    return roomRepository.save(availableRoom);
                })
                .orElseGet(() -> this.createRoom(firstPlayer, null, gameMode, WAITING));
    }

    private Room createRoom(Player firstPlayer, Player secondPlayer, GameModes gameMode, GameStates gameState) {
        String roomId = UUID.randomUUID().toString();
        Room room = new Room(roomId, firstPlayer, secondPlayer, gameMode);
        room.addGame(new Game(gameState));

        Optional.ofNullable(secondPlayer)
                .ifPresent(player -> room.setInvitationAcceptedTime(LocalDateTime.now(ZoneOffset.UTC)));

        metricsService.addRoomsCreatedMetrics(room);
        metricsService.addGamesCreatedMetrics(gameMode);
        Room roomCreated = roomRepository.save(room);

        log.info(String.format("CREATED room [%s][mode: %s] by player %s [id: %s]", room.getId(),
                gameMode, firstPlayer.getName(), firstPlayer.getId()));
        return roomCreated;
    }

}
