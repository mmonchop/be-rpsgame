package com.mlmo.rpsgame.controller;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.internal.DefaultsImpl;
import com.mlmo.rpsgame.model.Game;
import com.mlmo.rpsgame.model.Room;
import com.mlmo.rpsgame.model.Round;
import com.mlmo.rpsgame.model.enums.Choices;
import com.mlmo.rpsgame.model.enums.GameModes;
import com.mlmo.rpsgame.model.enums.GameStates;
import com.mlmo.rpsgame.model.enums.RoundStates;
import com.mlmo.rpsgame.service.GamePlayService;
import com.mlmo.rpsgame.service.RoomService;
import com.mlmo.rpsgame.service.notification.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.java.Log;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static com.mlmo.rpsgame.model.enums.Choices.*;
import static com.mlmo.rpsgame.model.enums.GameModes.*;
import static com.mlmo.rpsgame.model.enums.PlayerStates.READY;
import static com.mlmo.rpsgame.model.enums.PlayerStates.WAITING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan(basePackages = {
        "com.mlmo.rpsgame.controller",
        "com.mlmo.rpsgame.mapper",
        "com.mlmo.rpsgame.service",
        "com.mlmo.rpsgame.repository",
})
class GamePlayControllerIT {

    private static final String PARAM_PLAY_ROOM_ID = "roomId";
    private static final String PARAM_PLAY_GAME_NUMBER = "gameNumber";
    private static final String PARAM_PLAY_PLAYER_ID = "playerId";
    private static final String PARAM_PLAY_CHOICE = "choice";

    @Autowired
    GamePlayController gamePlayController;

    @Autowired
    ControllerExceptionHandler controllerExceptionHandler;

    @Autowired
    RoomService roomService;

    @Autowired
    GamePlayService gamePlayService;

    @Autowired
    MongoOperations mongoOperations;

    @SpyBean
    NotificationService notificationService;

    @MockBean
    MeterRegistry meterRegistry;

    @MockBean
    private Search search;

    private MockMvc mockMvc;

    @BeforeAll
    public void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(gamePlayController, controllerExceptionHandler).build();
    }

    @AfterAll
    public void after() {
        Configuration.setDefaults(DefaultsImpl.INSTANCE);
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        log.info("----------------------------------------------------------------------------------");
        log.info(testInfo.getDisplayName());
    }

    @AfterEach
    public void afterAll() {
        mongoOperations.dropCollection("rooms");
    }

    @Test
    @DisplayName("Game 1 - Round 1 - VS_FRIEND")
    /**
     * Game 1 (VS_FRIEND)
     * Round 1 (wins Paul 0-1)
     * ROCK     ROCK
     * ROCK     PAPER*
     */
    void playGame1Round1() throws Exception {
        // Given
        GameModes gameMode = VS_FRIEND;
        String firstPlayerName = "Miguel";
        String secondPlayerName = "Paul";
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);
        Room roomCreated = roomService.createRoom(firstPlayerName, gameMode);
        Room room = roomService.acceptRoomInvitation(roomCreated.getId(), secondPlayerName);

        // When
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActionsRound1_Turn1 = doPlayPost(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        Room roomRound1_Turn1 = roomService.getRoom(room.getId());

        ResultActions resultActionsRound1_Turn2 = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        Room roomRound1_Turn2 = roomService.getRoom(room.getId());;

        ResultActions resultActionsRound1_Turn3 = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), PAPER);
        Room roomRound1_Turn3 = roomService.getRoom(room.getId());;

        ResultActions resultActionsRound1_Turn4 = doPlayPost(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        Room roomRound1_Turn4 = roomService.getRoom(room.getId());;

        // Then
        resultActionsRound1_Turn1.andExpect(status().isOk());
        resultActionsRound1_Turn2.andExpect(status().isOk());
        resultActionsRound1_Turn3.andExpect(status().isOk());
        resultActionsRound1_Turn4.andExpect(status().isOk());

        // Turn 1 (Player1)
        assertGame1Round1Turn1(gameMode, room, firstPlayerName, roomRound1_Turn1);

        // Turn 2 (Player1)
        assertGame1Round1Turn2(room, secondPlayerName, roomRound1_Turn2);

        // Turn 3 (Player2)
        assertGame1Round1Turn3(room, secondPlayerName, roomRound1_Turn3);

        // Turn 4 (Player1)
        assertGame1Round1Turn4(room, firstPlayerName, secondPlayerName, roomRound1_Turn4);

        verify(counter, times(6)).increment();
        verify(timer, times(0)).record(anyLong(), any());
        verify(notificationService, times(5)).sendNotification(any(), any(), any(), any());

    }

    void assertGame1Round1Turn1(GameModes gameMode, Room room, String firstPlayerName, Room roomRound1_Turn1) {
        assertThat(roomRound1_Turn1.getId(), is(room.getId()));
        assertThat(roomRound1_Turn1.getMode(), is(gameMode));
        assertThat(roomRound1_Turn1.getGames().size(), is(1));
        assertThat(roomRound1_Turn1.getFirstPlayer().getPlayerState(), is(WAITING));
        assertThat(roomRound1_Turn1.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound1_Turn1 = roomRound1_Turn1.getGame(1);
        assertThat(gameRound1_Turn1.getGameNumber(), is(1));
        assertThat(gameRound1_Turn1.getState(), is(GameStates.PLAYING));
        assertThat(gameRound1_Turn1.getResult().getWinner(), is(nullValue()));
        assertThat(gameRound1_Turn1.getStartTime(), is(notNullValue()));
        assertThat(gameRound1_Turn1.getFinishTime(), is(nullValue()));
        assertThat(gameRound1_Turn1.getRounds().size(), is(1));

        Round round1_Turn1 = gameRound1_Turn1.getLastRoundOrThrow(roomRound1_Turn1.getId());
        assertThat(round1_Turn1.getRoundNumber(), is(1));
        assertThat(round1_Turn1.getState(), is(RoundStates.PLAYING));
        assertThat(round1_Turn1.getResult(), is(nullValue()));
        assertThat(round1_Turn1.getRoundTurns().size(), is(1));
        assertThat(round1_Turn1.getRoundTurns().get(0).getPlayer().getName(), is(firstPlayerName));
        assertThat(round1_Turn1.getRoundTurns().get(0).getChoice(), is(ROCK));
    }

    void assertGame1Round1Turn2(Room room, String secondPlayerName, Room roomRound1_Turn2) {
        assertThat(roomRound1_Turn2.getId(), is(room.getId()));
        assertThat(roomRound1_Turn2.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound1_Turn2.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound1_Turn2 = roomRound1_Turn2.getGame(1);
        Round round1_Turn2 = gameRound1_Turn2.getLastRoundOrThrow(roomRound1_Turn2.getId());
        assertThat(round1_Turn2.getRoundNumber(), is(1));
        assertThat(round1_Turn2.getState(), is(RoundStates.PLAYING));
        assertThat(round1_Turn2.getResult().isTie(), is(true));
        assertThat(round1_Turn2.getResult().getWinner(), is(nullValue()));
        assertThat(round1_Turn2.getRoundTurns().size(), is(2));
        assertThat(round1_Turn2.getRoundTurns().get(1).getPlayer().getName(), is(secondPlayerName));
        assertThat(round1_Turn2.getRoundTurns().get(1).getChoice(), is(ROCK));
    }

    void assertGame1Round1Turn3(Room room, String secondPlayerName, Room roomRound1_Turn3) {
        assertThat(roomRound1_Turn3.getId(), is(room.getId()));
        assertThat(roomRound1_Turn3.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound1_Turn3.getSecondPlayer().getPlayerState(), is(WAITING));

        Game gameRound1_Turn3 = roomRound1_Turn3.getGame(1);
        Round round1_Turn3 = gameRound1_Turn3.getLastRoundOrThrow(roomRound1_Turn3.getId());
        assertThat(round1_Turn3.getRoundNumber(), is(1));
        assertThat(round1_Turn3.getState(), is(RoundStates.PLAYING));
        assertThat(round1_Turn3.getResult(), is(nullValue()));
        assertThat(round1_Turn3.getRoundTurns().size(), is(3));
        assertThat(round1_Turn3.getRoundTurns().get(2).getPlayer().getName(), is(secondPlayerName));
        assertThat(round1_Turn3.getRoundTurns().get(2).getChoice(), is(PAPER));
    }

    void assertGame1Round1Turn4(Room room, String firstPlayerName, String secondPlayerName, Room roomRound1_Turn4) {
        assertThat(roomRound1_Turn4.getId(), is(room.getId()));
        assertThat(roomRound1_Turn4.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound1_Turn4.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound1_Turn4 = roomRound1_Turn4.getGame(1);
        Round round1_Turn4 = gameRound1_Turn4.getLastRoundOrThrow(roomRound1_Turn4.getId());
        assertThat(round1_Turn4.getRoundNumber(), is(1));
        assertThat(round1_Turn4.getState(), is(RoundStates.OVER));
        assertThat(round1_Turn4.getResult().getWinner().getName(), is(secondPlayerName));
        assertThat(round1_Turn4.getRoundTurns().size(), is(4));
        assertThat(round1_Turn4.getRoundTurns().get(3).getPlayer().getName(), is(firstPlayerName));
        assertThat(round1_Turn4.getRoundTurns().get(3).getChoice(), is(ROCK));

        assertThat(gameRound1_Turn4.getResult().getWinner(), is(nullValue()));
        assertThat(gameRound1_Turn4.getResult().getScoreFirstPlayer(), is(0));
        assertThat(gameRound1_Turn4.getResult().getScoreSecondPlayer(), is(1));
    }

    @Test
    @DisplayName("Game 1 - Round 2 - VS_FRIEND")
    /**
     * Game 1 (VS_FRIEND)-
     * Round 2 (wins Miguel 1-1)
     * ROCK*    SCISSORS
     */
    void playGame1Round2() throws Exception {
        // Given
        GameModes gameMode = VS_FRIEND;
        String firstPlayerName = "Miguel";
        String secondPlayerName = "Paul";
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        Room roomCreated = roomService.createRoom(firstPlayerName, gameMode);
        Room room = roomService.acceptRoomInvitation(roomCreated.getId(), secondPlayerName);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), PAPER);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);

        // When
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActionsRound2_Turn1 = doPlayPost(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        Room roomRound2_Turn1 = roomService.getRoom(room.getId());

        ResultActions resultActionsRound2_Turn2 = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);
        Room roomRound2_Turn2 = roomService.getRoom(room.getId());

        // Then
        resultActionsRound2_Turn1.andExpect(status().isOk());
        resultActionsRound2_Turn2.andExpect(status().isOk());

        // Turn 1 (Player1)
        assertGame1Round2Turn1(gameMode, room, firstPlayerName, roomRound2_Turn1);

        // Turn 2 (Player2)
        assertGame1Round2Turn2(room, firstPlayerName, secondPlayerName, roomRound2_Turn2);

        verify(counter, times(4)).increment();
        verify(timer, times(0)).record(anyLong(), any());
        verify(notificationService, times(7)).sendNotification(any(), any(), any(), any());
    }

    void assertGame1Round2Turn1(GameModes gameMode, Room room, String firstPlayerName, Room roomRound2_Turn1) {
        assertThat(roomRound2_Turn1.getId(), is(room.getId()));
        assertThat(roomRound2_Turn1.getMode(), is(gameMode));
        assertThat(roomRound2_Turn1.getGames().size(), is(1));
        assertThat(roomRound2_Turn1.getFirstPlayer().getPlayerState(), is(WAITING));
        assertThat(roomRound2_Turn1.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound2_Turn1 = roomRound2_Turn1.getGame(1);
        assertThat(gameRound2_Turn1.getGameNumber(), is(1));
        assertThat(gameRound2_Turn1.getState(), is(GameStates.PLAYING));
        assertThat(gameRound2_Turn1.getStartTime(), is(notNullValue()));
        assertThat(gameRound2_Turn1.getFinishTime(), is(nullValue()));
        assertThat(gameRound2_Turn1.getRounds().size(), is(2));

        Round round2_Turn1 = gameRound2_Turn1.getLastRoundOrThrow(roomRound2_Turn1.getId());
        assertThat(round2_Turn1.getRoundNumber(), is(2));
        assertThat(round2_Turn1.getState(), is(RoundStates.PLAYING));
        assertThat(round2_Turn1.getResult(), is(nullValue()));
        assertThat(round2_Turn1.getRoundTurns().size(), is(1));
        assertThat(round2_Turn1.getRoundTurns().get(0).getPlayer().getName(), is(firstPlayerName));
        assertThat(round2_Turn1.getRoundTurns().get(0).getChoice(), is(ROCK));
    }

    void assertGame1Round2Turn2(Room room, String firstPlayerName, String secondPlayerName, Room roomRound2_Turn2) {
        assertThat(roomRound2_Turn2.getId(), is(room.getId()));
        assertThat(roomRound2_Turn2.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound2_Turn2.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound1_Turn2 = roomRound2_Turn2.getGame(1);
        Round round2_Turn2 = gameRound1_Turn2.getLastRoundOrThrow(roomRound2_Turn2.getId());
        assertThat(round2_Turn2.getRoundNumber(), is(2));
        assertThat(round2_Turn2.getState(), is(RoundStates.OVER));
        assertThat(round2_Turn2.getResult().getWinner().getName(), is(firstPlayerName));
        assertThat(round2_Turn2.getRoundTurns().size(), is(2));
        assertThat(round2_Turn2.getRoundTurns().get(1).getPlayer().getName(), is(secondPlayerName));
        assertThat(round2_Turn2.getRoundTurns().get(1).getChoice(), is(SCISSORS));

        assertThat(gameRound1_Turn2.getResult().getWinner(), is(nullValue()));
        assertThat(gameRound1_Turn2.getResult().getScoreFirstPlayer(), is(1));
        assertThat(gameRound1_Turn2.getResult().getScoreSecondPlayer(), is(1));
    }

    @Test
    @DisplayName("Game 1 - Round 3 - VS_FRIEND")
    /**
     * Game 1 (VS_FRIEND)-
     * Round 3 (wins Miguel 2-1)
     * PAPER*    ROCK
     */
    void playGame1Round3() throws Exception {
        // Given
        GameModes gameMode = VS_FRIEND;
        String firstPlayerName = "Miguel";
        String secondPlayerName = "Paul";
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        Room roomCreated = roomService.createRoom(firstPlayerName, gameMode);
        Room room = roomService.acceptRoomInvitation(roomCreated.getId(), secondPlayerName);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), PAPER);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        // When
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActionsRound3_Turn1 = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        Room roomRound3_Turn1 = roomService.getRoom(room.getId());

        ResultActions resultActionsRound3_Turn2 = doPlayPost(room.getId(), 1, room.getFirstPlayer().getId(), PAPER);
        Room roomRound3_Turn2 = roomService.getRoom(room.getId());

        // Then
        resultActionsRound3_Turn1.andExpect(status().isOk());
        resultActionsRound3_Turn2.andExpect(status().isOk());

        // Turn 1 (Player2)
        assertGame1Round3Turn1(gameMode, room, secondPlayerName, roomRound3_Turn1);

        // Turn 2 (Player1)
        assertGame1Round3Turn2(room, firstPlayerName, roomRound3_Turn2);

        verify(counter, times(4)).increment();
        verify(timer, times(0)).record(anyLong(), any());
        verify(notificationService, times(9)).sendNotification(any(), any(), any(), any());
    }

    void assertGame1Round3Turn1(GameModes gameMode, Room room, String secondPlayerName, Room roomRound3_Turn1) {
        assertThat(roomRound3_Turn1.getId(), is(room.getId()));
        assertThat(roomRound3_Turn1.getMode(), is(gameMode));
        assertThat(roomRound3_Turn1.getGames().size(), is(1));
        assertThat(roomRound3_Turn1.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound3_Turn1.getSecondPlayer().getPlayerState(), is(WAITING));

        Game gameRound3_Turn1 = roomRound3_Turn1.getGame(1);
        assertThat(gameRound3_Turn1.getGameNumber(), is(1));
        assertThat(gameRound3_Turn1.getState(), is(GameStates.PLAYING));
        assertThat(gameRound3_Turn1.getStartTime(), is(notNullValue()));
        assertThat(gameRound3_Turn1.getFinishTime(), is(nullValue()));
        assertThat(gameRound3_Turn1.getRounds().size(), is(3));

        Round round3_Turn1 = gameRound3_Turn1.getLastRoundOrThrow(roomRound3_Turn1.getId());
        assertThat(round3_Turn1.getRoundNumber(), is(3));
        assertThat(round3_Turn1.getState(), is(RoundStates.PLAYING));
        assertThat(round3_Turn1.getResult(), is(nullValue()));
        assertThat(round3_Turn1.getRoundTurns().size(), is(1));
        assertThat(round3_Turn1.getRoundTurns().get(0).getPlayer().getName(), is(secondPlayerName));
        assertThat(round3_Turn1.getRoundTurns().get(0).getChoice(), is(ROCK));
    }

    void assertGame1Round3Turn2(Room room, String firstPlayerName, Room roomRound3_Turn2) {
        assertThat(roomRound3_Turn2.getId(), is(room.getId()));
        assertThat(roomRound3_Turn2.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound3_Turn2.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound3_Turn2 = roomRound3_Turn2.getGame(1);
        Round round3_Turn2 = gameRound3_Turn2.getLastRoundOrThrow(roomRound3_Turn2.getId());
        assertThat(round3_Turn2.getRoundNumber(), is(3));
        assertThat(round3_Turn2.getState(), is(RoundStates.OVER));
        assertThat(round3_Turn2.getResult().getWinner().getName(), is(firstPlayerName));
        assertThat(round3_Turn2.getRoundTurns().size(), is(2));
        assertThat(round3_Turn2.getRoundTurns().get(1).getPlayer().getName(), is(firstPlayerName));
        assertThat(round3_Turn2.getRoundTurns().get(1).getChoice(), is(PAPER));

        assertThat(gameRound3_Turn2.getResult().getWinner(), is(nullValue()));
        assertThat(gameRound3_Turn2.getResult().getScoreFirstPlayer(), is(2));
        assertThat(gameRound3_Turn2.getResult().getScoreSecondPlayer(), is(1));
    }

    @Test
    @DisplayName("Game 1 - Round 4 - VS_FRIEND")
    /**
     * Game 1 (VS_FRIEND)
     * Round 4 (wins Paul 2-2)
     * PAPER     PAPER
     * PAPER     SCISSORS*
     */
    void playGame1Round4() throws Exception {
        // Given
        GameModes gameMode = VS_FRIEND;
        String firstPlayerName = "Miguel";
        String secondPlayerName = "Paul";
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        Room roomCreated = roomService.createRoom(firstPlayerName, gameMode);
        Room room = roomService.acceptRoomInvitation(roomCreated.getId(), secondPlayerName);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), PAPER);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), PAPER);

        // When
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActionsRound4_Turn1 = doPlayPost(room.getId(), 1, room.getFirstPlayer().getId(), PAPER);
        Room roomRound4_Turn1 = roomService.getRoom(room.getId());

        ResultActions resultActionsRound4_Turn2 = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), PAPER);
        Room roomRound4_Turn2 = roomService.getRoom(room.getId());

        ResultActions resultActionsRound4_Turn3 = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);
        Room roomRound4_Turn3 = roomService.getRoom(room.getId());

        ResultActions resultActionsRound4_Turn4 = doPlayPost(room.getId(), 1, room.getFirstPlayer().getId(), PAPER);
        Room roomRound4_Turn4 = roomService.getRoom(room.getId());

        // Then
        resultActionsRound4_Turn1.andExpect(status().isOk());
        resultActionsRound4_Turn2.andExpect(status().isOk());
        resultActionsRound4_Turn3.andExpect(status().isOk());
        resultActionsRound4_Turn4.andExpect(status().isOk());

        // Turn 1 (Player1)
        assertGame1Round4Turn1(gameMode, room, firstPlayerName, roomRound4_Turn1);

        // Turn 2 (Player2)
        assertGame1Round4Turn2(room, secondPlayerName, roomRound4_Turn2);

        // Turn 3 (Player2)
        assertGame1Round4Turn3(room, secondPlayerName, roomRound4_Turn3);

        // Turn 4 (Player1)
        assertGame1Round4Turn4(room, firstPlayerName, secondPlayerName, roomRound4_Turn4);

        verify(counter, times(6)).increment();
        verify(timer, times(0)).record(anyLong(), any());
        verify(notificationService, times(13)).sendNotification(any(), any(), any(), any());
    }

    void assertGame1Round4Turn1(GameModes gameMode, Room room, String firstPlayerName, Room roomRound4_Turn1) {
        assertThat(roomRound4_Turn1.getId(), is(room.getId()));
        assertThat(roomRound4_Turn1.getMode(), is(gameMode));
        assertThat(roomRound4_Turn1.getGames().size(), is(1));
        assertThat(roomRound4_Turn1.getFirstPlayer().getPlayerState(), is(WAITING));
        assertThat(roomRound4_Turn1.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound4_Turn1 = roomRound4_Turn1.getGame(1);
        assertThat(gameRound4_Turn1.getGameNumber(), is(1));
        assertThat(gameRound4_Turn1.getState(), is(GameStates.PLAYING));
        assertThat(gameRound4_Turn1.getResult().getWinner(), is(nullValue()));
        assertThat(gameRound4_Turn1.getStartTime(), is(notNullValue()));
        assertThat(gameRound4_Turn1.getFinishTime(), is(nullValue()));
        assertThat(gameRound4_Turn1.getRounds().size(), is(4));

        Round round4_Turn1 = gameRound4_Turn1.getLastRoundOrThrow(roomRound4_Turn1.getId());
        assertThat(round4_Turn1.getRoundNumber(), is(4));
        assertThat(round4_Turn1.getState(), is(RoundStates.PLAYING));
        assertThat(round4_Turn1.getResult(), is(nullValue()));
        assertThat(round4_Turn1.getRoundTurns().size(), is(1));
        assertThat(round4_Turn1.getRoundTurns().get(0).getPlayer().getName(), is(firstPlayerName));
        assertThat(round4_Turn1.getRoundTurns().get(0).getChoice(), is(PAPER));
    }

    void assertGame1Round4Turn2(Room room, String secondPlayerName, Room roomRound4_Turn2) {
        assertThat(roomRound4_Turn2.getId(), is(room.getId()));
        assertThat(roomRound4_Turn2.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound4_Turn2.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound4Turn2 = roomRound4_Turn2.getGame(1);
        Round round4_Turn2 = gameRound4Turn2.getLastRoundOrThrow(roomRound4_Turn2.getId());
        assertThat(round4_Turn2.getRoundNumber(), is(4));
        assertThat(round4_Turn2.getState(), is(RoundStates.PLAYING));
        assertThat(round4_Turn2.getResult().isTie(), is(true));
        assertThat(round4_Turn2.getResult().getWinner(), is(nullValue()));
        assertThat(round4_Turn2.getRoundTurns().size(), is(2));
        assertThat(round4_Turn2.getRoundTurns().get(1).getPlayer().getName(), is(secondPlayerName));
        assertThat(round4_Turn2.getRoundTurns().get(1).getChoice(), is(PAPER));
    }

    void assertGame1Round4Turn3(Room room, String secondPlayerName, Room roomRound4_Turn3) {
        assertThat(roomRound4_Turn3.getId(), is(room.getId()));
        assertThat(roomRound4_Turn3.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound4_Turn3.getSecondPlayer().getPlayerState(), is(WAITING));

        Game gameRound1_Turn3 = roomRound4_Turn3.getGame(1);
        Round round1_Turn3 = gameRound1_Turn3.getLastRoundOrThrow(roomRound4_Turn3.getId());
        assertThat(round1_Turn3.getRoundNumber(), is(4));
        assertThat(round1_Turn3.getState(), is(RoundStates.PLAYING));
        assertThat(round1_Turn3.getResult(), is(nullValue()));
        assertThat(round1_Turn3.getRoundTurns().size(), is(3));
        assertThat(round1_Turn3.getRoundTurns().get(2).getPlayer().getName(), is(secondPlayerName));
        assertThat(round1_Turn3.getRoundTurns().get(2).getChoice(), is(SCISSORS));

    }

    void assertGame1Round4Turn4(Room room, String firstPlayerName, String secondPlayerName, Room roomRound4_Turn4) {
        assertThat(roomRound4_Turn4.getId(), is(room.getId()));
        assertThat(roomRound4_Turn4.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound4_Turn4.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound4_Turn4 = roomRound4_Turn4.getGame(1);
        Round round4_Turn4 = gameRound4_Turn4.getLastRoundOrThrow(roomRound4_Turn4.getId());
        assertThat(round4_Turn4.getRoundNumber(), is(4));
        assertThat(round4_Turn4.getState(), is(RoundStates.OVER));
        assertThat(round4_Turn4.getResult().getWinner().getName(), is(secondPlayerName));
        assertThat(round4_Turn4.getRoundTurns().size(), is(4));
        assertThat(round4_Turn4.getRoundTurns().get(3).getPlayer().getName(), is(firstPlayerName));
        assertThat(round4_Turn4.getRoundTurns().get(3).getChoice(), is(PAPER));

        assertThat(gameRound4_Turn4.getResult().getWinner(), is(nullValue()));
        assertThat(gameRound4_Turn4.getResult().getScoreFirstPlayer(), is(2));
        assertThat(gameRound4_Turn4.getResult().getScoreSecondPlayer(), is(2));
    }

    @Test
    @DisplayName("Game 1 - Round 5 - VS_FRIEND")
    /**
     * Game 1 (VS_FRIEND)-
     * Round 5 (wins Paul 2-3)
     * SCISSORS    ROCK*
     */
    void playGame1Round5() throws Exception {
        // Given
        GameModes gameMode = VS_FRIEND;
        String firstPlayerName = "Miguel";
        String secondPlayerName = "Paul";
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        Room roomCreated = roomService.createRoom(firstPlayerName, gameMode);
        Room room = roomService.acceptRoomInvitation(roomCreated.getId(), secondPlayerName);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), PAPER);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);

        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), PAPER);

        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), PAPER);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), PAPER);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), PAPER);


        // When
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActionsRound5_Turn1 = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);
        Room roomRound5_Turn1 = roomService.getRoom(room.getId());

        ResultActions resultActionsRound5_Turn2 = doPlayPost(room.getId(), 1, room.getFirstPlayer().getId(), SCISSORS);
        Room roomRound5_Turn2 = roomService.getRoom(room.getId());

        // Then
        resultActionsRound5_Turn1.andExpect(status().isOk());
        resultActionsRound5_Turn2.andExpect(status().isOk());

        // Turn 1 (Player2)
        assertGame1Round5Turn1(gameMode, room, secondPlayerName, roomRound5_Turn1);

        // Turn 2 (Player1)
        assertGame1Round5Turn2(room, firstPlayerName, secondPlayerName, roomRound5_Turn2);

        verify(counter, times(5)).increment();
        verify(timer, times(1)).record(anyLong(), any());
        verify(notificationService, times(15)).sendNotification(any(), any(), any(), any());
    }

    void assertGame1Round5Turn1(GameModes gameMode, Room room, String secondPlayerName, Room roomRound5_Turn1) {
        assertThat(roomRound5_Turn1.getId(), is(room.getId()));
        assertThat(roomRound5_Turn1.getMode(), is(gameMode));
        assertThat(roomRound5_Turn1.getGames().size(), is(1));
        assertThat(roomRound5_Turn1.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound5_Turn1.getSecondPlayer().getPlayerState(), is(WAITING));

        Game gameRound5_Turn1 = roomRound5_Turn1.getGame(1);
        assertThat(gameRound5_Turn1.getGameNumber(), is(1));
        assertThat(gameRound5_Turn1.getState(), is(GameStates.PLAYING));
        assertThat(gameRound5_Turn1.getStartTime(), is(notNullValue()));
        assertThat(gameRound5_Turn1.getFinishTime(), is(nullValue()));
        assertThat(gameRound5_Turn1.getRounds().size(), is(5));

        Round round5_Turn1 = gameRound5_Turn1.getLastRoundOrThrow(roomRound5_Turn1.getId());
        assertThat(round5_Turn1.getRoundNumber(), is(5));
        assertThat(round5_Turn1.getState(), is(RoundStates.PLAYING));
        assertThat(round5_Turn1.getResult(), is(nullValue()));
        assertThat(round5_Turn1.getRoundTurns().size(), is(1));
        assertThat(round5_Turn1.getRoundTurns().get(0).getPlayer().getName(), is(secondPlayerName));
        assertThat(round5_Turn1.getRoundTurns().get(0).getChoice(), is(ROCK));
    }

    void assertGame1Round5Turn2(Room room, String firstPlayerName, String secondPlayerName, Room roomRound5_Turn2) {
        assertThat(roomRound5_Turn2.getId(), is(room.getId()));
        assertThat(roomRound5_Turn2.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(roomRound5_Turn2.getSecondPlayer().getPlayerState(), is(READY));

        Game gameRound5_Turn2 = roomRound5_Turn2.getGame(1);
        Round round5_Turn2 = gameRound5_Turn2.getLastRoundOrThrow(roomRound5_Turn2.getId());
        assertThat(round5_Turn2.getRoundNumber(), is(5));
        assertThat(round5_Turn2.getState(), is(RoundStates.OVER));
        assertThat(round5_Turn2.getResult().getWinner().getName(), is(secondPlayerName));
        assertThat(round5_Turn2.getRoundTurns().size(), is(2));
        assertThat(round5_Turn2.getRoundTurns().get(1).getPlayer().getName(), is(firstPlayerName));
        assertThat(round5_Turn2.getRoundTurns().get(1).getChoice(), is(SCISSORS));

        assertThat(gameRound5_Turn2.getFinishTime(), is(notNullValue()));
        assertThat(gameRound5_Turn2.getResult().getWinner().getName(), is(secondPlayerName));
        assertThat(gameRound5_Turn2.getResult().getScoreFirstPlayer(), is(2));
        assertThat(gameRound5_Turn2.getResult().getScoreSecondPlayer(), is(3));
    }

    @Test
    @DisplayName("Generate automatic turn in mode VS_MACHINE")
    void addMachineTurnToLastRound() throws Exception {
        // Given
        String firstPlayerName = "Miguel";
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);

        Room createdRoom = roomService.createRoom(firstPlayerName, VS_MACHINE);

        // When
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActions = doPlayPost(createdRoom.getId(), 1, createdRoom.getFirstPlayer().getId(), ROCK);
        Room room = roomService.getRoom(createdRoom.getId());

        // Then
        resultActions.andExpect(status().isOk());

        // Turn 1 (Player2)
        assertThat(room.getId(), is(room.getId()));
        assertThat(room.getMode(), is(VS_MACHINE));
        assertThat(room.getGames().size(), is(1));
        assertThat(room.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(room.getSecondPlayer().getPlayerState(), is(READY));

        Game game = room.getGame(1);
        assertThat(game.getGameNumber(), is(1));
        assertThat(game.getState(), is(GameStates.PLAYING));
        assertThat(game.getRounds().size(), is(1));

        Round lastRound = game.getLastRoundOrThrow(room.getId());
        assertThat(lastRound.getRoundNumber(), is(1));
        assertThat(lastRound.getRoundTurns().size(), is(2));
        assertThat(lastRound.getRoundTurns().get(0).getPlayer().getName(), is(firstPlayerName));
        assertThat(lastRound.getRoundTurns().get(0).getChoice(), is(ROCK));
        assertThat(lastRound.getRoundTurns().get(1).getPlayer().getName(), is("HAL 9000"));
        assertThat(lastRound.getRoundTurns().get(1).getChoice(), anyOf(is(ROCK), is(PAPER), is(SCISSORS)));

        verify(counter, atLeast(3)).increment();
        verify(timer, times(0)).record(anyLong(), any());
        verify(notificationService, times(1)).sendNotification(any(), any(), any(), any());
    }


    @Test
    @DisplayName("Raised GAME OVER exception")
    void raisedGameOverException() throws Exception {
        // Given
        roomService.createRoom("Miguel", VS_RANDOM_PLAYER);
        Room room = roomService.createRoom("John", VS_RANDOM_PLAYER);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        // When
        ResultActions resultActions = doPlayPost(room.getId(), 1, room.getSecondPlayer().getId(), ROCK);

        //Then
        String error = resultActions
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException().getMessage();

        String expectedError = String.format("Game [1] in room [%s] is OVER", room.getId());
        assertThat(error, containsString(expectedError));
    }

    @Test
    @DisplayName("Raised exception due to player is not registered in given room")
    void raisedInvalidPlayerException() throws Exception {
        // Given
        Room room = roomService.createRoom("Miguel", VS_MACHINE);

        // When
        String invalidPlayerId = "INVALID_PLAYER_ID";
        ResultActions resultActions = doPlayPost(room.getId(), 1, invalidPlayerId, ROCK);

        //Then
        String error = resultActions
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException().getMessage();

        String expectedError = String.format("Player [%s] is NOT registered in room [%s]", invalidPlayerId, room.getId());
        assertThat(error, containsString(expectedError));
    }

    @Test
    @DisplayName("Raised exception as same player is doing 2 consecutive movements")
    void raisedIIncorrectPlayerTurnException() throws Exception {
        // Given
        Room roomCreated = roomService.createRoom("Miguel", VS_FRIEND);
        Room room = roomService.acceptRoomInvitation(roomCreated.getId(), "Diego");
        String playerId = room.getFirstPlayer().getId();

        gamePlayService.play(room.getId(), 1, playerId, ROCK);

        // When
        ResultActions resultActions = doPlayPost(room.getId(), 1, playerId, PAPER);

        //Then
        String error = resultActions
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException().getMessage();

        String expectedError = String.format("Player [%s] is playing 2 consecutive turns [game: %s] [room: %s]",
                room.getFirstPlayer().getName(), 1, room.getId());
        assertThat(error, containsString(expectedError));
    }

    @Test
    @DisplayName("Create a new game in room")
    void createNewGameInRoom() throws Exception {
        // Given
        Counter counter = mock(Counter.class);
        roomService.createRoom("Miguel", VS_RANDOM_PLAYER);
        Room room = roomService.createRoom("John", VS_RANDOM_PLAYER);
        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        gamePlayService.play(room.getId(), 1, room.getFirstPlayer().getId(), ROCK);
        gamePlayService.play(room.getId(), 1, room.getSecondPlayer().getId(), SCISSORS);

        // When
        addMetricsMocksInteractions(counter, null);
        doCreateNewGamePost(room.getId(), room.getSecondPlayer().getId())
                .andExpect(status().isOk());

        Room roomGame2 = roomService.getRoom(room.getId());

        // Then
        Game lastGame = roomGame2.getLastGame();
        assertThat(roomGame2.getGames().size(), is(2));
        assertThat(lastGame.getGameNumber(), is(2));
        assertThat(lastGame.getState(), is(GameStates.WAITING));
        assertThat(lastGame.getStartTime(), is(notNullValue()));
        assertThat(lastGame.getFinishTime(), is(nullValue()));
        assertThat(lastGame.getResult().getWinner(), is(nullValue()));
        assertThat(lastGame.getResult().getScoreFirstPlayer(), is(0));
        assertThat(lastGame.getResult().getScoreSecondPlayer(), is(0));
        assertThat(lastGame.getRounds().size(), is(0));

        verify(counter, times(1)).increment();
        verify(notificationService, times(8)).sendNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Raised exception due to player is not registered in given room, creating new game")
    void raisedCreatingNewGameInvalidPlayerException() throws Exception {
        // Given
        Room room = roomService.createRoom("Miguel", VS_MACHINE);

        // When
        String invalidPlayerId = "INVALID_PLAYER_ID";
        ResultActions resultActions = doCreateNewGamePost(room.getId(), invalidPlayerId);

        //Then
        String error = resultActions
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException().getMessage();

        String expectedError = String.format("Player [%s] is NOT registered in room [%s]", invalidPlayerId, room.getId());
        assertThat(error, containsString(expectedError));
    }

    @Test
    @DisplayName("Raised GAME is not OVER exception")
    void raisedGameIsNotOverException() throws Exception {
        // Given
        Room room = roomService.createRoom("Miguel", VS_MACHINE);

        // When
        ResultActions resultActions = doCreateNewGamePost(room.getId(), room.getFirstPlayer().getId());

        //Then
        String error = resultActions
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException().getMessage();

        String expectedError = String.format("Last Game [1] in room [%s] is NOT OVER", room.getId());
        assertThat(error, containsString(expectedError));
    }

    private void addMetricsMocksInteractions(Counter counter, Timer timer) {
        when(meterRegistry.find(any())).thenReturn(search);
        when(search.tags((String[]) any())).thenReturn(search);
        when(search.counter()).thenReturn(counter);

        Optional.ofNullable(timer)
                .ifPresent(t -> when(search.timer()).thenReturn(t));
    }

    private ResultActions doPlayPost(String roomId, int gameNumber, String playerId, Choices choice) throws Exception {
        return mockMvc.perform(
                post("/api/1.0/rooms/" + roomId + "/games/" + gameNumber + "/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param(PARAM_PLAY_ROOM_ID, roomId)
                        .param(PARAM_PLAY_GAME_NUMBER, String.valueOf(gameNumber))
                        .param(PARAM_PLAY_PLAYER_ID, playerId)
                        .param(PARAM_PLAY_CHOICE, choice.name()));
    }

    private ResultActions doCreateNewGamePost(String roomId, String playerId) throws Exception {
        return mockMvc.perform(
                post("/api/1.0/rooms/" + roomId + "/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param(PARAM_PLAY_ROOM_ID, roomId)
                        .param(PARAM_PLAY_PLAYER_ID, playerId));
    }
}