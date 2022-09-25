package com.mlmo.rpsgame.model.dto;

import com.mlmo.rpsgame.model.enums.PlayerStates;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlayerDto {
    private String id;
    private String name;
    private PlayerStates playerState;
}
