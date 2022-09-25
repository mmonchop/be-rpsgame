package com.mlmo.rpsgame.model;

import com.mlmo.rpsgame.model.enums.PlayerStates;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String id;
    private String name;
    private PlayerStates playerState;
}
