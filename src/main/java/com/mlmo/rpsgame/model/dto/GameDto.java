package com.mlmo.rpsgame.model.dto;

import com.mlmo.rpsgame.model.enums.GameStates;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameDto {
    private int gameNumber;
    private GameStates state;

    private RoundDto lastRound;

    private String winnerName;
    private int scoreFirstPlayer;
    private int scoreSecondPlayer;
}
