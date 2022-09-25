package com.mlmo.rpsgame.controller;

import com.mlmo.rpsgame.mapper.RoomMapper;
import com.mlmo.rpsgame.model.ErrorMessage;
import com.mlmo.rpsgame.model.Room;
import com.mlmo.rpsgame.model.dto.RoomDto;
import com.mlmo.rpsgame.model.enums.Choices;
import com.mlmo.rpsgame.service.GamePlayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.mlmo.rpsgame.config.OpenApiConfiguration.API_VERSION;
import static com.mlmo.rpsgame.config.OpenApiConfiguration.SECURITY_SCHEME_NAME;

@RestController
@RequestMapping("/api/" + API_VERSION)
@Tag(name = "Game Play", description = "RpsGame - Game Play")
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GamePlayController {

    private final RoomMapper roomMapper;
    private final GamePlayService gamePlayService;

    @PreAuthorize ("hasRole('PLAYER_ROLE')")
    @PostMapping(value = {"/rooms/{roomId}/games/{gameNumber}/play"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Play a RPS turn", description = "Play a RPS turn for a given room & game", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoomDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Error", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public ResponseEntity<RoomDto> play(
            @Parameter(name = "roomId", required = true, description = "Room Id.") @PathVariable String roomId,
            @Parameter(name = "gameNumber", required = true, description = "Game Number") @PathVariable int gameNumber,
            @Parameter(name = "playerId", required = true, description = "Player Id.") @RequestParam String playerId,
            @Parameter(name = "choice", required = true, description = "Choice [ROCK | PAPER | SCISSORS]") @RequestParam Choices choice) {

        Room room = gamePlayService.play(roomId, gameNumber, playerId, choice);
        return new ResponseEntity<>(roomMapper.roomToDto(room), HttpStatus.OK);
    }

    @PreAuthorize ("hasRole('PLAYER_ROLE')")
    @PostMapping(value = {"/rooms/{roomId}/games"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Creates a new game in room", description = "Creates a new game in provided room", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoomDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Error", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public ResponseEntity<RoomDto> createNewGame(
            @Parameter(name = "roomId", required = true, description = "Room Id.") @PathVariable String roomId,
            @Parameter(name = "playerId", required = true, description = "Player Id.") @RequestParam String playerId) {

        Room room = gamePlayService.createNewGame(roomId, playerId);
        return new ResponseEntity<>(roomMapper.roomToDto(room), HttpStatus.OK);
    }

}