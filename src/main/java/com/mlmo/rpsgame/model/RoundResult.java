package com.mlmo.rpsgame.model;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoundResult {
    private Player winner;
    private boolean tie = true;
}
