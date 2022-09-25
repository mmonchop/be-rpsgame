package com.mlmo.rpsgame.model.dto;

import com.mlmo.rpsgame.model.enums.Choices;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoundTurnDto {
    private String playerId;
    private Choices choice;
}
