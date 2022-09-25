package com.mlmo.rpsgame.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameResult {
    private Player winner;
    private int scoreFirstPlayer;
    private int scoreSecondPlayer;
}
