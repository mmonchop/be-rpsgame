package com.mlmo.rpsgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mlmo.rpsgame.exception.EntityNotFoundException;
import com.mlmo.rpsgame.model.enums.GameStates;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class Game {
    private int gameNumber;
    private GameStates state;
    private GameResult result;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    private List<Round> rounds = new ArrayList<>();

    public Game(GameStates state) {
        this.state = state;
        this.result = new GameResult(null, 0, 0);
        setState(state);
        setStartTime(LocalDateTime.now(ZoneOffset.UTC));
    }

    @JsonIgnore
    public Optional<Round> getLastRound() {
        return this.rounds.isEmpty() ? Optional.empty() : Optional.of(this.rounds.get(this.rounds.size() - 1));
    }

    @JsonIgnore
    public Round getLastRoundOrNull() {
        return getLastRound().orElse(null);
    }

    @JsonIgnore
    public Round getLastRoundOrThrow(String roomId) {
        return getLastRound()
                .orElseThrow(() -> new EntityNotFoundException(String.format("Round not found in [game: %s] [room: %s]", this.gameNumber, roomId)));
    }

    @JsonIgnore
    public void addRound(Round round) {
        round.setRoundNumber(this.rounds.size() + 1);
        this.rounds.add(round);
    }

}
