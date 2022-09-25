package com.mlmo.rpsgame.service;

import com.mlmo.rpsgame.model.Game;
import com.mlmo.rpsgame.model.Room;
import com.mlmo.rpsgame.model.Round;
import com.mlmo.rpsgame.model.RoundTurn;
import com.mlmo.rpsgame.model.enums.GameModes;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private static final String METRIC_COUNT_ROOMS_CREATED = "rpsgame-rooms-created-count";
    private static final String METRIC_COUNT_INVITE_ACCEPTED = "rpsgame-invites-accepted-count";
    private static final String METRIC_TIME_INVITE_ACCEPTED = "rpsgame-invites-accepted-time";
    private static final String METRIC_COUNT_GAMES_CREATED = "rpsgame-games-created-count";
    private static final String METRIC_COUNT_GAMES_OVER = "rpsgame-games-over-count";
    private static final String METRIC_TIME_GAMES_OVER = "rpsgame-games-over-time";
    private static final String METRIC_COUNT_ROUNDS_PLAYED = "rpsgame-rounds-played-count";
    private static final String METRIC_COUNT_ROUNDS_OVER = "rpsgame-rounds-over-count";
    private static final String METRIC_COUNT_TURNS_PLAYED = "rpsgame-turns-played-count";

    private static final String TAG_ROOM_GAME_MODE = "room-game-mode";
    private static final String TAG_GAME_NUM_GAME = "game-num-game";
    private static final String TAG_ROUND_NUM_ROUND = "round-num-round";
    private static final String TAG_ROUND_NUM_TURNS = "round-num-turns";
    private static final String TAG_TURN_CHOICE = "round-turn-choice";
    private static final String TAG_TURN_MACHINE = "round-turn-machine";

    public void addRoomsCreatedMetrics(Room room) {
        String[] roomCreationTags = getTags(TAG_ROOM_GAME_MODE, room.getMode().name());
        counter(METRIC_COUNT_ROOMS_CREATED, roomCreationTags);
    }

    public void addRoomInvitationsAcceptedMetrics(Room room) {
        long timeDifference = room.getInvitationAcceptedTime().toEpochSecond(ZoneOffset.UTC) -
                room.getCreationTime().toEpochSecond(ZoneOffset.UTC);

        String[] invitationsAcceptedTags = getTags(TAG_ROOM_GAME_MODE, room.getMode().name());
        counter(METRIC_COUNT_INVITE_ACCEPTED, invitationsAcceptedTags);
        timer(METRIC_TIME_INVITE_ACCEPTED, timeDifference, invitationsAcceptedTags);
    }

    public void addGamesCreatedMetrics(GameModes gameMode) {
        String[] gamesCreatedTags = getTags(TAG_ROOM_GAME_MODE, gameMode.name());
        counter(METRIC_COUNT_GAMES_CREATED, gamesCreatedTags);
    }

    public void addGamesOverMetrics(Game game, GameModes gameMode) {
        long timeDifference = game.getStartTime().toEpochSecond(ZoneOffset.UTC) -
                game.getFinishTime().toEpochSecond(ZoneOffset.UTC);

        String[] gamesOverTags = getTags(
                TAG_GAME_NUM_GAME, String.valueOf(game.getGameNumber()),
                TAG_ROOM_GAME_MODE, gameMode.name());

        counter(METRIC_COUNT_GAMES_OVER, gamesOverTags);
        timer(METRIC_TIME_GAMES_OVER, timeDifference, gamesOverTags);
    }

    public void addRoundsPlayedMetrics(GameModes gameMode) {
        String[] roundsPlayedTags = getTags(TAG_ROOM_GAME_MODE, gameMode.name());
        counter(METRIC_COUNT_ROUNDS_PLAYED, roundsPlayedTags);
    }

    public void addRoundsOverMetrics(Round round, GameModes gameMode) {
        String[] roundsOverTags = getTags(
                TAG_ROUND_NUM_ROUND, String.valueOf(round.getRoundNumber()),
                TAG_ROUND_NUM_TURNS, String.valueOf(round.getRoundTurns().size()),
                TAG_ROOM_GAME_MODE, gameMode.name());

        counter(METRIC_COUNT_ROUNDS_OVER, roundsOverTags);
    }

    public void addTurnsPlayedMetrics(RoundTurn roundTurn, GameModes gameMode, boolean isMachine) {
        String[] turnsPlayedTags = getTags(
                TAG_TURN_MACHINE, String.valueOf(isMachine),
                TAG_TURN_CHOICE, roundTurn.getChoice().name(),
                TAG_ROOM_GAME_MODE, gameMode.name());

        counter(METRIC_COUNT_TURNS_PLAYED, turnsPlayedTags);
    }

    private String[] getTags(String... tags) {
        return Stream.of(tags).toArray(String[]::new);
    }

    private void counter(String name, String... tags) {
        Search search = meterRegistry.find(name);
        Optional.ofNullable(search)
                .ifPresent(s -> {
                    Counter counter = s.tags(tags).counter();
                    if (counter == null) {
                        counter = meterRegistry.counter(name, tags);
                    }
                    counter.increment();
                });
    }

    private void timer(String name, long time, String... tags) {
        Search search = meterRegistry.find(name);
        Optional.ofNullable(search)
                .ifPresent(s -> {
                    Timer timer = s.tags(tags).timer();
                    if (timer == null) {
                        timer = meterRegistry.timer(name, tags);
                    }
                    timer.record(time, TimeUnit.MILLISECONDS);
                });
    }

}

