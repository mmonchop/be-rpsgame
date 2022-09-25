package com.mlmo.rpsgame.service;

import com.mlmo.rpsgame.exception.EntityNotFoundException;
import com.mlmo.rpsgame.model.*;
import com.mlmo.rpsgame.model.enums.Choices;
import com.mlmo.rpsgame.model.enums.GameStates;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mlmo.rpsgame.model.enums.Choices.*;
import static com.mlmo.rpsgame.model.enums.RoundStates.OVER;

@Log
@Service
public class RoundResultService {

    public static final int NUM_GAME_PLAYERS = 2;

    private static final int DRAW = 0;
    private static final int PLAYER1 = 1;
    private static final int PLAYER2 = 2;

    private final int gameNumRounds;
    private final MetricsService metricsService;
    private final Map<String, Integer> playerWinnerMatrix = new HashMap<>();

    @Autowired
    public RoundResultService(MetricsService metricsService,
                              @Value("${rpsgame.game-num-rounds}") int gameNumRounds) {
        this.gameNumRounds = gameNumRounds;
        this.metricsService = metricsService;
    }

    @PostConstruct
    public void init() {
        buildPlayerWinnerMatrix();
    }

    public Room calculateRoundResults(Room room, int gameNumber) {
        Game game = room.getGame(gameNumber);
        Round lastRound = game.getLastRoundOrThrow(room.getId());
        RoundResult roundResult = getRoundResult(room, game, lastRound);
        lastRound.setResult(roundResult);

        Optional.ofNullable(roundResult)
                .map(RoundResult::getWinner)
                .ifPresent(winner -> {
                    lastRound.setState(OVER);
                    if (winner.getId().equals(room.getFirstPlayer().getId())) {
                        game.getResult().setScoreFirstPlayer(game.getResult().getScoreFirstPlayer() + 1);
                    } else {
                        game.getResult().setScoreSecondPlayer(game.getResult().getScoreSecondPlayer() + 1);
                    }

                    metricsService.addRoundsOverMetrics(lastRound, room.getMode());
                    log.info(String.format("ROUND OVER score %s:%s - %s:%s [round: %s][game: %s][room: %s]",
                            room.getFirstPlayer().getName(), game.getResult().getScoreFirstPlayer(),
                            room.getSecondPlayer().getName(), game.getResult().getScoreSecondPlayer(),
                            lastRound.getRoundNumber(), game.getGameNumber(), room.getId()));
                });

        if (isGameOver(game)) {
            game.setState(GameStates.OVER);
            game.getResult().setWinner(game.getLastRoundOrThrow(room.getId()).getResult().getWinner());
            game.setFinishTime(LocalDateTime.now(ZoneOffset.UTC));
            metricsService.addGamesOverMetrics(game, room.getMode());

            log.info(String.format("GAME OVER winner %s (%s - %s) [game: %s][room: %s]",
                    game.getResult().getWinner().getName(), game.getResult().getScoreFirstPlayer(),game.getResult().getScoreSecondPlayer(),
                    game.getGameNumber(), room.getId()));
        }
        return room;
    }

    private RoundResult getRoundResult(Room room, Game game, Round round) {
        if (round.getRoundTurns().size() % NUM_GAME_PLAYERS != 0) {
            // Not available turns of both players, winner can't be decided
            return null;
        } else {
            RoundTurn firstPlayerTurn = getLastPlayerRoundTurn(room, game, round, room.getFirstPlayer());
            RoundTurn secondPlayerTurn = getLastPlayerRoundTurn(room, game, round, room.getSecondPlayer());
            return getRoundWinner(firstPlayerTurn, secondPlayerTurn);
        }
    }

    private RoundResult getRoundWinner(RoundTurn firstPlayerTurn, RoundTurn secondPlayerTurn) {
        int playerWinner = playerWinnerMatrix.get(getPlayerWinnerMatrixEntryKey(firstPlayerTurn.getChoice(), secondPlayerTurn.getChoice()));
        return switch (playerWinner) {
            case 1 -> new RoundResult(firstPlayerTurn.getPlayer(), false);
            case 2 -> new RoundResult(secondPlayerTurn.getPlayer(), false);
            default -> new RoundResult(null, true);
        };
    }

    private void buildPlayerWinnerMatrix() {
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(ROCK, ROCK), DRAW);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(ROCK, PAPER), PLAYER2);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(ROCK, SCISSORS), PLAYER1);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(PAPER, ROCK), PLAYER1);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(PAPER, PAPER), DRAW);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(PAPER, SCISSORS), PLAYER2);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(SCISSORS, ROCK), PLAYER2);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(SCISSORS, PAPER), PLAYER1);
        playerWinnerMatrix.put(getPlayerWinnerMatrixEntryKey(SCISSORS, SCISSORS), DRAW);
    }

    private String getPlayerWinnerMatrixEntryKey(Choices firstPlayerChoice, Choices secondPlayerChoice) {
        return firstPlayerChoice.name() + "_" + secondPlayerChoice.name();
    }

    private RoundTurn getLastPlayerRoundTurn(Room room, Game game, Round round, Player player) {
        List<RoundTurn> playerTurns = round.getRoundTurns().stream()
                .filter(roundTurn -> player.getId().equals(roundTurn.getPlayer().getId()))
                .toList();

        return playerTurns.stream().skip(playerTurns.size() - 1L).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(String.format("Not Found any RoundTurn for user [%s] in [room: %s][game: %s][round: %s]",
                        player.getId(), room.getId(), game.getGameNumber(), round.getRoundNumber())));
    }

    private boolean isGameOver(Game game) {
        return game.getResult().getScoreFirstPlayer() == gameNumRounds ||
                game.getResult().getScoreSecondPlayer() == gameNumRounds;
    }

}
