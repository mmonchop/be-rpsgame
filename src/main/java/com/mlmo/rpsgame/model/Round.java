package com.mlmo.rpsgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mlmo.rpsgame.model.enums.RoundStates;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.mlmo.rpsgame.model.enums.RoundStates.PLAYING;
import static com.mlmo.rpsgame.service.RoundResultService.NUM_GAME_PLAYERS;

@Getter
@Setter
public class Round {
    private int roundNumber;
    private List<RoundTurn> roundTurns = new ArrayList<>();
    private RoundResult result;
    private RoundStates state = PLAYING;

    @JsonIgnore
    public void addTurn(RoundTurn roundTurn) {
        this.state = PLAYING;
        this.roundTurns.add(roundTurn);
    }

    @JsonIgnore
    public RoundTurn getLastRoundTurn() {
        return this.roundTurns.get(this.roundTurns.size() - 1);
    }

    @JsonIgnore
    public boolean playerIsWaiting() {
        return roundTurns.size() % NUM_GAME_PLAYERS != 0;
    }
}
