package com.mlmo.rpsgame.model.dto;

import com.mlmo.rpsgame.model.enums.RoundStates;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RoundDto {
    private int roundNumber;
    private RoundStates state;

    private List<RoundTurnDto> roundTurns = new ArrayList<>();

    private boolean playerIsWaiting;
    private String winnerName;
    private boolean tie = true;

}
