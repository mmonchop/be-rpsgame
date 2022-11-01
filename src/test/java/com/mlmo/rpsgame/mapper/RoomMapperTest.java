package com.mlmo.rpsgame.mapper;

import com.mlmo.rpsgame.model.Game;
import com.mlmo.rpsgame.model.GameResult;
import com.mlmo.rpsgame.model.Player;
import com.mlmo.rpsgame.model.RoundTurn;
import com.mlmo.rpsgame.model.dto.GameDto;
import com.mlmo.rpsgame.model.dto.RoomDto;
import com.mlmo.rpsgame.model.dto.RoundTurnDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static com.mlmo.rpsgame.model.enums.Choices.ROCK;
import static com.mlmo.rpsgame.model.enums.PlayerStates.READY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomMapperTest {

    private RoomMapper roomMapper ;

    @BeforeAll
    void init() {
        this.roomMapper = Mappers.getMapper(RoomMapper.class);
    }

    @Test
    @DisplayName("Should return null, mapping room")
    void shouldReturnNullMappingRoom() {
        // When
        RoomDto roomDto = roomMapper.roomToDto(null);

        // Then
        assertThat(roomDto, is(nullValue()));
    }

    @Test
    @DisplayName("Should return null, mapping game")
    void shouldReturnNullMappingGame() {
        // When
        GameDto game1Dto = roomMapper.gameToDto(null);
        GameDto game2Dto = roomMapper.gameToDto(new Game());

        Game game3 = new Game();
        game3.setResult(new GameResult());
        GameDto game3Dto = roomMapper.gameToDto(game3);

        Game game4 = new Game();
        GameResult game4Result = new GameResult();
        game4Result.setWinner(new Player("1", null, READY));
        game3.setResult(game4Result);
        GameDto game4Dto = roomMapper.gameToDto(game3);

        // Then
        assertThat(game1Dto, is(nullValue()));
        assertThat(game2Dto.getWinnerName(), is(nullValue()));
        assertThat(game2Dto.getScoreFirstPlayer(), is(0));
        assertThat(game2Dto.getScoreSecondPlayer(), is(0));
        assertThat(game3Dto.getWinnerName(), is(nullValue()));
        assertThat(game3Dto.getScoreFirstPlayer(), is(0));
        assertThat(game3Dto.getScoreSecondPlayer(), is(0));
        assertThat(game4Dto.getWinnerName(), is(nullValue()));
    }

    @Test
    @DisplayName("Should return null, mapping null roundTurnList")
    void shouldReturnNullMappingRoundTurnList() {
        // When
        List<RoundTurnDto> roundTurnDtos = roomMapper.roundTurnListToDtoList(null);

        // Then
        assertThat(roundTurnDtos, is(nullValue()));
    }

    @Test
    @DisplayName("Should return null, mapping roundTurn")
    void shouldReturnNullMappingRoundTurn() {
        // When
        RoundTurnDto roundTurn1Dto = roomMapper.roundTurnToDto(null);
        RoundTurnDto roundTurn2Dto = roomMapper.roundTurnToDto(new RoundTurn());
        RoundTurnDto roundTurn3Dto = roomMapper.roundTurnToDto(new RoundTurn(new Player(null, "Incorrect Player", READY), ROCK));

        // Then
        assertThat(roundTurn1Dto, is(nullValue()));
        assertThat(roundTurn2Dto.getPlayerId(), is(nullValue()));
        assertThat(roundTurn3Dto.getPlayerId(), is(nullValue()));
    }

}
