package com.mlmo.rpsgame.service;

import com.mlmo.rpsgame.exception.GameOverException;
import com.mlmo.rpsgame.exception.IncorrectPlayerRoundTurnException;
import com.mlmo.rpsgame.exception.InvalidPlayerException;
import com.mlmo.rpsgame.model.*;
import com.mlmo.rpsgame.model.enums.Choices;
import com.mlmo.rpsgame.model.enums.GameStates;
import com.mlmo.rpsgame.model.enums.NotificationEventType;
import com.mlmo.rpsgame.model.enums.PlayerStates;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Stream;

import static com.mlmo.rpsgame.model.enums.GameModes.VS_MACHINE;
import static com.mlmo.rpsgame.model.enums.GameStates.PLAYING;
import static com.mlmo.rpsgame.model.enums.GameStates.WAITING;
import static com.mlmo.rpsgame.model.enums.NotificationEventType.NEW_GAME_CREATED;
import static com.mlmo.rpsgame.model.enums.NotificationEventType.ROUND_TURN_PLAY;
import static com.mlmo.rpsgame.model.enums.RoundStates.OVER;
import static com.mlmo.rpsgame.service.RoundResultService.NUM_GAME_PLAYERS;

@Log
@Service
public class GamePlayService {

    private static final int CHOICE_LOW_RANDOM_VALUE = 0;
    private static final int CHOICE_HIGH_RANDOM_VALUE = 3;

    private final String machinePlayerName;

    private final RoomService roomService;
    private final RoundResultService roundResultService;
    private final MetricsService metricsService;
    private Random random;

    @Autowired
    public GamePlayService(RoomService roomService,
                           MetricsService metricsService,
                           RoundResultService roundResultService,
                           @Value("${rpsgame.machine-player-name}") String machinePlayerName) {
        this.roomService = roomService;
        this.metricsService = metricsService;
        this.roundResultService = roundResultService;
        this.machinePlayerName = machinePlayerName;
        try {
            this.random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            this.random = new Random();
        }
    }

    public Room play(String roomId, int gameNumber, String playerId, Choices choice) {
        Room room = roomService.getRoom(roomId);
        Game game = room.getGame(gameNumber);

        // Validations
        checkPlayerIsValid(room, playerId);
        checkGameOver(roomId, game);
        checkPlayerRoundTurn(room, game, playerId);

        // Create new round (if applies)
        game.setState(PLAYING);
        if (needToCreateANewRound(roomId, game)) {
            game.addRound(new Round());
            metricsService.addRoundsPlayedMetrics(room.getMode());
        }

        // Add Player turn
        addPlayerTurnToLastRound(room, game, playerId, choice);
        if (VS_MACHINE.equals(room.getMode())) {
            addMachineTurnToLastRound(room, game);
        }

        // Obtain results and update states
        room = roundResultService.calculateRoundResults(room, gameNumber);
        setPlayersState(room, game, playerId);

        // Persist Room & send notification to players
        return roomService.updateRoomAndSendNotification(room, playerId, ROUND_TURN_PLAY);
    }

    public Room createNewGame(String roomId, String playerId) {
        Room room = roomService.getRoom(roomId);

        checkPlayerIsValid(room, playerId);
        checkLastGameIsOver(roomId, room.getLastGame());

        Game game = new Game(WAITING);
        room.addGame(game);
        metricsService.addGamesCreatedMetrics(room.getMode());
        return roomService.updateRoomAndSendNotification(room, playerId, NEW_GAME_CREATED);
    }


    private void addPlayerTurnToLastRound(Room room, Game game, String playerId, Choices choice) {
        Player player = room.getPlayer(roomPlayer -> playerId.equals(roomPlayer.getId()));

        RoundTurn roundTurn = new RoundTurn(player, choice);
        Round lastRound = game.getLastRoundOrThrow(room.getId());
        lastRound.addTurn(roundTurn);

        metricsService.addTurnsPlayedMetrics(roundTurn, room.getMode(), false);
        log.fine(String.format("Player %s played %s in [turn: %s][round: %s][game: %s][room: %s]",
                player.getName(), choice, lastRound.getRoundTurns().size(), lastRound.getRoundNumber(),
                game.getGameNumber(), room.getId()));
    }

    private void addMachineTurnToLastRound(Room room, Game game) {
        Player machinePlayer = room.getPlayer(roomPlayer -> machinePlayerName.equals(roomPlayer.getName()));

        int choiceValue = random.nextInt(CHOICE_HIGH_RANDOM_VALUE - CHOICE_LOW_RANDOM_VALUE) + CHOICE_LOW_RANDOM_VALUE;
        Choices choice = Choices.findByValue(choiceValue);

        RoundTurn roundTurn = new RoundTurn(machinePlayer, choice);
        Round lastRound = game.getLastRoundOrThrow(room.getId());
        lastRound.addTurn(roundTurn);

        metricsService.addTurnsPlayedMetrics(roundTurn, room.getMode(), true);
        log.fine(String.format("Machine %s played %s in [turn: %s][round: %s][game: %s][room: %s]",
                machinePlayer.getName(), choice, lastRound.getRoundTurns().size(), lastRound.getRoundNumber(),
                game.getGameNumber(), room.getId()));
    }

    private void checkPlayerIsValid(Room room, String playerId) {
        boolean validPlayer = Stream.of(room.getFirstPlayer(), room.getSecondPlayer())
                .anyMatch(player -> playerId.equals(player.getId()));

        if (!validPlayer) {
            throw new InvalidPlayerException(String.format("Player [%s] is NOT registered in room [%s]", playerId, room.getId()));
        }
    }

    private void checkGameOver(String roomId, Game game) {
        if (game.getState().equals(GameStates.OVER)) {
            throw new GameOverException(String.format("Game [%s] in room [%s] is OVER", game.getGameNumber(), roomId));
        }
    }

    private void checkLastGameIsOver(String roomId, Game game) {
        if (!game.getState().equals(GameStates.OVER)) {
            throw new GameOverException(String.format("Last Game [%s] in room [%s] is NOT OVER. Can't create a new game in this room",
                    game.getGameNumber(), roomId));
        }
    }

    private void checkPlayerRoundTurn(Room room, Game game, String playerId) {
        game.getLastRound()
                .ifPresent(lastRound -> {
                    RoundTurn lastRoundTurn = lastRound.getLastRoundTurn();

                    if (playerId.equals(lastRoundTurn.getPlayer().getId()) &&
                            lastRound.getRoundTurns().size() % NUM_GAME_PLAYERS != 0) {
                        throw new IncorrectPlayerRoundTurnException(String.format("Player [%s] is playing 2 consecutive turns [game: %s] [room: %s]",
                                room.getPlayer(player -> playerId.equals(player.getId())).getName(), game.getGameNumber(), room.getId()));
                    }
                });
    }

    private Room setPlayersState(Room room, Game game, String playerId) {
        if (game.getLastRoundOrThrow(room.getId()).playerIsWaiting()) {
            room.getPlayer(player -> playerId.equals(player.getId())).setPlayerState(PlayerStates.WAITING);
        } else {
            room.getFirstPlayer().setPlayerState(PlayerStates.READY);
            room.getSecondPlayer().setPlayerState(PlayerStates.READY);
        }
        return room;
    }

    private boolean needToCreateANewRound(String roomId, Game game) {
        return !game.getLastRound().isPresent() ||
                OVER.equals(game.getLastRoundOrThrow(roomId).getState());
    }
}
