package com.mlmo.rpsgame.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.internal.DefaultsImpl;
import com.mlmo.rpsgame.model.Game;
import com.mlmo.rpsgame.model.Player;
import com.mlmo.rpsgame.model.Room;
import com.mlmo.rpsgame.model.dto.RoomDto;
import com.mlmo.rpsgame.model.enums.GameModes;
import com.mlmo.rpsgame.model.enums.GameStates;
import com.mlmo.rpsgame.repository.RoomRepository;
import com.mlmo.rpsgame.service.RoomService;
import com.mlmo.rpsgame.service.notification.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.java.Log;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.mlmo.rpsgame.model.enums.GameModes.*;
import static com.mlmo.rpsgame.model.enums.GameStates.WAITING;
import static com.mlmo.rpsgame.model.enums.PlayerStates.READY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class RoomControllerIT {

    private static final String PARAM_CREATE_ROOM_FIRST_PLAYER_NAME = "firstPlayerName";
    private static final String PARAM_CREATE_ROOM_GAME_MODE = "gameMode";

    private static final String PARAM_ACCEPT_ROOM_INVITATION_ROOM_ID = "roomId";
    private static final String PARAM_ACCEPT_ROOM_INVITATION_SECOND_PLAYER_NAME = "secondPlayerName";

    @Value("${rpsgame.max-wait-random-player-minutes}")
    int maxWaitRandomPlayerMinutes;

    @Autowired
    private RoomController roomController;

    @Autowired
    private ControllerExceptionHandler controllerExceptionHandler;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @SpyBean
    private NotificationService notificationService;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private Search search;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeAll
    public void init() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(roomController, controllerExceptionHandler).build();
        search = mock(Search.class);
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
    public void afterEach() {
        mongoOperations.dropCollection("rooms");
    }

    @Test
    @DisplayName("Get room by id")
    void getRoomById() throws Exception {
        // Given
        Room roomCreated = roomService.createRoom("Miguel", VS_RANDOM_PLAYER);

        // When
        ResultActions resultActions = doRoomGet(roomCreated.getId());

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
        String roomId = getRoomIdFromResultActions(resultActions);

        assertThat(roomId, is(roomCreated.getId()));
    }

    @Test
    @DisplayName("Raised room NOT FOUND exception while trying to obtain room by id")
    void roomNotFound() throws Exception {
        // Given
        roomService.createRoom("Miguel", VS_RANDOM_PLAYER);

        // When
        String invalidRoomId = "INVALID_ID";
        ResultActions resultActions = doRoomGet(invalidRoomId);

        //Then
        String error = resultActions
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();

        String expectedError = String.format("Room [%s] NOT FOUND", invalidRoomId);
        assertThat(error, containsString(expectedError));
    }

    @Test
    @DisplayName("Create new room in mode VS_FRIEND")
    void createNewRoomVsFriend() throws Exception {
        // Given
        GameModes gameMode = VS_FRIEND;
        String firstPlayerName = "Miguel";

        // When
        Counter counter = mock(Counter.class);
        Timer timer = mock(Timer.class);
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActions = doCreateRoomPost(firstPlayerName, gameMode.name());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        String roomId = getRoomIdFromResultActions(resultActions);
        Room room = roomService.getRoom(roomId);

        assertThat(room.getId(), is(notNullValue()));
        assertThat(room.getMode(), is(gameMode));
        assertThat(room.getFirstPlayer().getId(), is(notNullValue()));
        assertThat(room.getFirstPlayer().getName(), is(firstPlayerName));
        assertThat(room.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(room.getSecondPlayer(), is(nullValue()));
        assertThat(room.getCreationTime(), is(notNullValue()));
        assertThat(room.getInvitationAcceptedTime(), is(nullValue()));

        assertThat(room.getGames().size(), is(1));
        Game game = room.getGame(1);
        assertThat(game.getGameNumber(), is(1));
        assertThat(game.getState(), is(WAITING));
        assertThat(game.getRounds().size(), is(0));
        assertThat(game.getResult().getWinner(), is(nullValue()));
        assertThat(game.getResult().getScoreFirstPlayer(), is(0));
        assertThat(game.getResult().getScoreSecondPlayer(), is(0));
        assertThat(game.getStartTime(), is(notNullValue()));
        assertThat(game.getFinishTime(), is(nullValue()));
        assertThat(game.getRounds().size(), is(0));

        verify(counter, times(2)).increment();
    }

    @Test
    @DisplayName("Create new room in mode VS_MACHINE")
    void createNewRoomVsMachine() throws Exception {
        // Given
        GameModes gameMode = VS_MACHINE;
        String firstPlayerName = "Miguel";
        Counter counter = mock(Counter.class);

        // When
        addMetricsMocksInteractions(counter, null);
        ResultActions resultActions = doCreateRoomPost(firstPlayerName, gameMode.name());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        String roomId = getRoomIdFromResultActions(resultActions);
        Room room = roomService.getRoom(roomId);

        assertThat(room.getId(), is(notNullValue()));
        assertThat(room.getMode(), is(gameMode));
        assertThat(room.getFirstPlayer().getId(), is(notNullValue()));
        assertThat(room.getFirstPlayer().getName(), is(firstPlayerName));
        assertThat(room.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(room.getSecondPlayer().getId(), is(notNullValue()));
        assertThat(room.getSecondPlayer().getName(), is("HAL 9000"));
        assertThat(room.getSecondPlayer().getPlayerState(), is(READY));
        assertThat(room.getCreationTime(), is(notNullValue()));
        assertThat(room.getInvitationAcceptedTime(), is(notNullValue()));
        assertThat(room.getGames().size(), is(1));

        Game game = room.getGame(1);
        assertThat(game.getGameNumber(), is(1));
        assertThat(game.getState(), is(GameStates.ACCEPTED));
        assertThat(game.getRounds().size(), is(0));
        assertThat(game.getResult().getWinner(), is(nullValue()));
        assertThat(game.getResult().getScoreFirstPlayer(), is(0));
        assertThat(game.getResult().getScoreSecondPlayer(), is(0));
        assertThat(game.getStartTime(), is(notNullValue()));
        assertThat(game.getFinishTime(), is(nullValue()));
        assertThat(game.getRounds().size(), is(0));

        verify(counter, times(2)).increment();
    }

    @Test
    @DisplayName("Create new room in mode VS_RANDOM_PLAYER where there are NOT available rooms")
    void createNewRoomVsRandomPlayerWithoutRoomsAvailable() throws Exception {
        // Given
        String firstPlayerName = "Miguel";
        GameModes gameMode = VS_RANDOM_PLAYER;
        Counter counter = mock(Counter.class);

        // When
        addMetricsMocksInteractions(counter, null);
        ResultActions resultActions = doCreateRoomPost(firstPlayerName, gameMode.name());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        String roomId = getRoomIdFromResultActions(resultActions);
        Room room = roomService.getRoom(roomId);

        assertThat(room.getId(), is(notNullValue()));
        assertThat(room.getMode(), is(gameMode));
        assertThat(room.getFirstPlayer().getId(), is(notNullValue()));
        assertThat(room.getFirstPlayer().getName(), is(firstPlayerName));
        assertThat(room.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(room.getSecondPlayer(), is(nullValue()));
        assertThat(room.getCreationTime(), is(notNullValue()));
        assertThat(room.getInvitationAcceptedTime(), is(nullValue()));
        assertThat(room.getGames().size(), is(1));

        Game game = room.getGame(1);
        assertThat(game.getGameNumber(), is(1));
        assertThat(game.getState(), is(WAITING));
        assertThat(game.getRounds().size(), is(0));
        assertThat(game.getResult().getWinner(), is(nullValue()));
        assertThat(game.getResult().getScoreFirstPlayer(), is(0));
        assertThat(game.getResult().getScoreSecondPlayer(), is(0));
        assertThat(game.getStartTime(), is(notNullValue()));
        assertThat(game.getFinishTime(), is(nullValue()));
        assertThat(game.getRounds().size(), is(0));

        verify(counter, times(2)).increment();
    }

    @Test
    @DisplayName("Joins an available created room in mode VS_RANDOM_PLAYER")
    void joinAvailableRoomVsRandomPlayer() throws Exception {
        // Given
        String firstPlayerName = "Miguel";
        Counter counter = mock(Counter.class);
        GameModes gameMode = VS_RANDOM_PLAYER;
        LocalDateTime timeOutWaitMaxWindow = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(maxWaitRandomPlayerMinutes + 1);
        createRoomRandomPlayerMongoDB("Peter", timeOutWaitMaxWindow);

        Room roomAvailable1 = createRoomRandomPlayerMongoDB("Tom", LocalDateTime.now(ZoneOffset.UTC));
        createRoomRandomPlayerMongoDB("Paul", LocalDateTime.now(ZoneOffset.UTC));

        // When
        addMetricsMocksInteractions(counter, null);
        ResultActions resultActions = doCreateRoomPost(firstPlayerName, gameMode.name());

        // Then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        String roomId = getRoomIdFromResultActions(resultActions);
        Room room = roomService.getRoom(roomId);

        assertThat(room.getId(), is(roomAvailable1.getId()));
        assertThat(room.getMode(), is(gameMode));
        assertThat(room.getFirstPlayer().getId(), is(roomAvailable1.getFirstPlayer().getId()));
        assertThat(room.getFirstPlayer().getName(), is("Tom"));
        assertThat(room.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(room.getSecondPlayer().getId(), is(notNullValue()));
        assertThat(room.getSecondPlayer().getName(), is(firstPlayerName));
        assertThat(room.getSecondPlayer().getPlayerState(), is(READY));
        assertThat(room.getCreationTime(), is(notNullValue()));
        assertThat(room.getInvitationAcceptedTime(), is(notNullValue()));
        assertThat(room.getGames().size(), is(1));

        Game game = room.getGame(1);
        assertThat(game.getGameNumber(), is(1));
        assertThat(game.getState(), is(GameStates.ACCEPTED));
        assertThat(game.getRounds().size(), is(0));
        assertThat(game.getResult().getWinner(), is(nullValue()));
        assertThat(game.getResult().getScoreFirstPlayer(), is(0));
        assertThat(game.getResult().getScoreSecondPlayer(), is(0));
        assertThat(game.getStartTime(), is(notNullValue()));
        assertThat(game.getFinishTime(), is(nullValue()));
        assertThat(game.getRounds().size(), is(0));

        verify(counter, times(0)).increment();
        verify(notificationService, times(1))
                .sendNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Raised exception while trying to create a room with incorrect game mode")
    void raiseInvalidGameModeException() throws Exception {
        // When
        ResultActions resultActions = doCreateRoomPost("Miguel", "VS_NOBODY");

        //Then
        resultActions
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Accepted friend room invitation in VS_FRIEND mode")
    void acceptedFriendRoomInvitation() throws Exception {
        // Given
        GameModes gameMode = VS_FRIEND;
        String firstPlayerName = "Miguel";
        String secondPlayerName = "Paul";
        Timer timer = mock(Timer.class);
        Counter counter = mock(Counter.class);
        Room friendCreatedRoom = roomService.createRoom(firstPlayerName, gameMode);

        // When
        addMetricsMocksInteractions(counter, timer);
        ResultActions resultActions = doAcceptRoomInvitationPost(friendCreatedRoom.getId(), secondPlayerName);

        // Then
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        String roomId = getRoomIdFromResultActions(resultActions);
        Room room = roomService.getRoom(roomId);

        assertThat(room.getId(), is(friendCreatedRoom.getId()));
        assertThat(room.getMode(), is(gameMode));
        assertThat(room.getFirstPlayer().getId(), is(friendCreatedRoom.getFirstPlayer().getId()));
        assertThat(room.getFirstPlayer().getName(), is(firstPlayerName));
        assertThat(room.getFirstPlayer().getPlayerState(), is(READY));
        assertThat(room.getSecondPlayer().getId(), is(notNullValue()));
        assertThat(room.getSecondPlayer().getName(), is(secondPlayerName));
        assertThat(room.getSecondPlayer().getPlayerState(), is(READY));
        assertThat(room.getCreationTime(), is(notNullValue()));
        assertThat(room.getInvitationAcceptedTime(), is(notNullValue()));
        assertThat(room.getGames().size(), is(1));

        Game game = room.getGame(1);
        assertThat(game.getGameNumber(), is(1));
        assertThat(game.getState(), is(GameStates.ACCEPTED));
        assertThat(game.getRounds().size(), is(0));
        assertThat(game.getResult().getWinner(), is(nullValue()));
        assertThat(game.getResult().getScoreFirstPlayer(), is(0));
        assertThat(game.getResult().getScoreSecondPlayer(), is(0));
        assertThat(game.getStartTime(), is(notNullValue()));
        assertThat(game.getFinishTime(), is(nullValue()));
        assertThat(game.getRounds().size(), is(0));

        verify(counter, times(1)).increment();
        verify(timer, times(1)).record(anyLong(), any());
        verify(notificationService, times(1))
                .sendNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Raised room NOT FOUND exception while accepting room with incorrect id")
    void roomAlreadyDoesNotExistsWhileAcceptingFriendInvite() throws Exception {
        // Given
        roomService.createRoom("Miguel", VS_RANDOM_PLAYER);

        // When
        String invalidRoomId = "INVALID_ID";
        ResultActions resultActions = doAcceptRoomInvitationPost(invalidRoomId, "Luis");

        //Then
        String error = resultActions
                .andExpect(status().isNotFound())
                .andReturn().getResolvedException().getMessage();

        String expectedError = String.format("Room [%s] NOT FOUND", invalidRoomId);
        assertThat(error, containsString(expectedError));
    }

    private void addMetricsMocksInteractions(Counter counter, Timer timer) {
        when(meterRegistry.find(any())).thenReturn(search);
        when(search.tags((String[]) any())).thenReturn(search);
        when(search.counter()).thenReturn(counter);

        Optional.ofNullable(timer)
                .ifPresent(t -> when(search.timer()).thenReturn(t));
    }

    private ResultActions doRoomGet(String roomId) throws Exception {
        return mockMvc.perform(
                get("/api/1.0/rooms/" + roomId)
                        .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions doCreateRoomPost(String firstPlayerName, String gameModeName) throws Exception {
        return mockMvc.perform(
                post("/api/1.0/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param(PARAM_CREATE_ROOM_FIRST_PLAYER_NAME, firstPlayerName)
                        .param(PARAM_CREATE_ROOM_GAME_MODE, gameModeName));
    }

    private ResultActions doAcceptRoomInvitationPost(String roomId, String secondPlayerName) throws Exception {
        return mockMvc.perform(
                post("/api/1.0/rooms/" + roomId + "/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param(PARAM_ACCEPT_ROOM_INVITATION_ROOM_ID, roomId)
                        .param(PARAM_ACCEPT_ROOM_INVITATION_SECOND_PLAYER_NAME, secondPlayerName));
    }

    private Room createRoomRandomPlayerMongoDB(String firstPlayerName, LocalDateTime creationTime) {
        String roomId = UUID.randomUUID().toString();
        Player firstPlayer = new Player(ObjectId.get().toString(), firstPlayerName, READY);
        Room room = new Room(roomId, firstPlayer, null, VS_RANDOM_PLAYER);
        room.setCreationTime(creationTime);
        room.addGame(new Game(WAITING));
        return roomRepository.save(room);
    }

    private String getRoomIdFromResultActions(ResultActions resultActions) throws Exception {
        String contentAsString = resultActions.andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(contentAsString, RoomDto.class).getId();
    }
}