package com.mlmo.rpsgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mlmo.rpsgame.exception.EntityNotFoundException;
import com.mlmo.rpsgame.exception.InvalidPlayerException;
import com.mlmo.rpsgame.model.enums.GameModes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "rooms")
public class Room {
    private String id;
    private Player firstPlayer;
    private Player secondPlayer;

    private GameModes mode;
    private List<Game> games = new ArrayList<>();

    private LocalDateTime creationTime;
    private LocalDateTime invitationAcceptedTime;

    public Room(String id, Player firstPlayer, GameModes mode) {
        this.id = id;
        this.firstPlayer = firstPlayer;
        setMode(mode);
        setCreationTime(LocalDateTime.now(ZoneOffset.UTC));
    }

    public Room(String id, Player firstPlayer, Player secondPlayer, GameModes mode) {
        this(id, firstPlayer, mode);
        this.secondPlayer = secondPlayer;
    }

    @JsonIgnore
    public Player getPlayer(Predicate<Player> playerPredicate) {
        return Stream.of(firstPlayer, secondPlayer)
                .filter(playerPredicate)
                .findFirst()
                .orElseThrow(() -> new InvalidPlayerException(String.format("Player is NOT registered in room [%s]", this.id)));
    }

    @JsonIgnore
    public Game getGame(int gameNumber) {
        return this.games.stream()
                .filter(game -> game.getGameNumber() == gameNumber)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(String.format("Game [%s] not found in room [%s]",
                        gameNumber, this.getId())));
    }

    @JsonIgnore
    public Game getLastGame() {
        return games.stream().skip(games.size() - 1L).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(String.format("Not found last game in [%s]", this.id)));
    }

    @JsonIgnore
    public void addGame(Game game) {
        game.setGameNumber(this.games.size() + 1);
        this.games.add(game);
    }

}
