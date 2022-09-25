package com.mlmo.rpsgame.model;

import com.mlmo.rpsgame.model.enums.Choices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.mlmo.rpsgame.model.enums.GameStates.PLAYING;
import static com.mlmo.rpsgame.model.enums.PlayerStates.READY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomTest {

    @Test
    @DisplayName("Raised  EntityNotFoundException when game is not found")
    void raisedGameNotFoundException() {
        Room room = new Room();
        room.setId("Room1");
        room.setFirstPlayer(new Player("1", "Miguel", READY));
        room.setSecondPlayer(new Player("2", "John", READY));
        room.addGame(new Game(PLAYING));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            room.getGame(3);
        });

        String expectedMessage = String.format(String.format("Game [%s] not found in room [%s]", 3, room.getId()));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @DisplayName("Raised InvalidPlayer exception when player is not registered in room")
    void raisedInvalidPlayerException() {
        Room room = new Room();
        room.setId("Room1");
        room.setFirstPlayer(new Player("1", "Miguel", READY));
        room.setSecondPlayer(new Player("2", "John", READY));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            room.getPlayer(player -> player.getId().equals("3"));
        });

        String expectedMessage = String.format(String.format("Player is NOT registered in room [%s]", room.getId()));
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @DisplayName("Raised EntityNotFound exception obtaining last round")
    void raisedEntityNotFoundObtainingLastRound() {
        String roomId = "Room1";
        Room room = new Room();
        room.setId(roomId);
        Game game = new Game(PLAYING);
        room.addGame(game);

        Exception exception = assertThrows(RuntimeException.class, () -> game.getLastRoundOrThrow(roomId));

        String expectedMessage = String.format("Round not found in [game: %s] [room: %s]", 1, room.getId());
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }

    @Test
    @DisplayName("Raised InvalidChoice exception obtaining choice by value")
    void raisedInvalidChoiceException() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            Choices.findByValue(3);
        });

        String expectedMessage = "Invalid Choice. It should be ROCK, PAPER or SCISSORS";
        assertThat(exception.getMessage(), containsString(expectedMessage));
    }


}
