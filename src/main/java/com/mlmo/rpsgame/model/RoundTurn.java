package com.mlmo.rpsgame.model;

import com.mlmo.rpsgame.model.enums.Choices;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoundTurn {
    private Player player;
    private Choices choice;
}
