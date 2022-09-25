package com.mlmo.rpsgame.mapper;

import com.mlmo.rpsgame.model.*;
import com.mlmo.rpsgame.model.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(source = "lastGame", target = "currentGame")
    RoomDto roomToDto(Room room);

    PlayerDto playerToDto(Player player);

    @Mapping(source = "result.winner.name", target = "winnerName")
    @Mapping(source = "result.scoreFirstPlayer", target = "scoreFirstPlayer")
    @Mapping(source = "result.scoreSecondPlayer", target = "scoreSecondPlayer")
    @Mapping(source = "lastRoundOrNull", target = "lastRound")
    GameDto gameToDto(Game game);


    @Mapping(source = "result.tie", target = "tie")
    @Mapping(source = "result.winner.name", target = "winnerName")
    RoundDto roundToDto(Round round);

    @Mapping(source = "player.id", target = "playerId")
    RoundTurnDto roundTurnToDto(RoundTurn roundTurn);

    List<RoundTurnDto> roundTurnListToDtoList(List<RoundTurn> roundTurn);
}
