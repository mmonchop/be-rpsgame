package com.mlmo.rpsgame.model.dto;

import com.mlmo.rpsgame.model.enums.GameModes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoomDto {
    private String id;
    private PlayerDto firstPlayer;
    private PlayerDto secondPlayer;
    private GameModes mode;

    GameDto currentGame;
}
