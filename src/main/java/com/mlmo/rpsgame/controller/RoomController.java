package com.mlmo.rpsgame.controller;

import com.mlmo.rpsgame.mapper.RoomMapper;
import com.mlmo.rpsgame.model.ErrorMessage;
import com.mlmo.rpsgame.model.Room;
import com.mlmo.rpsgame.model.dto.RoomDto;
import com.mlmo.rpsgame.model.enums.GameModes;
import com.mlmo.rpsgame.service.RoomService;
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
@Tag(name = "Room", description = "RpsGame - Rooms")
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RoomController {

    private final RoomMapper roomMapper;
    private final RoomService roomService;


    @PreAuthorize ("hasRole('PLAYER_ROLE')")
    @GetMapping(value = {"/rooms/{roomId}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Obtain Room", description = "Obtain room information", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoomDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Error", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
    })
    public ResponseEntity<RoomDto> getRoom(
            @Parameter(name = "roomId", required = true, description = "Room Id.") @PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        return new ResponseEntity<>(roomMapper.roomToDto(room), HttpStatus.OK);
    }


    @PreAuthorize ("hasRole('PLAYER_ROLE')")
    @PostMapping(value = {"/rooms"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Create RpsGame room", description = "Create a RpsGame room", responses = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoomDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Error", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public ResponseEntity<RoomDto> createRoom(
            @Parameter(name = "firstPlayerName", required = true, description = "First player name") @RequestParam String firstPlayerName,
            @Parameter(name = "gameMode", required = true, description = "Game mode") @RequestParam GameModes gameMode) {

        Room createdRoom = roomService.createRoom(firstPlayerName, gameMode);
        return new ResponseEntity<>(roomMapper.roomToDto(createdRoom), HttpStatus.CREATED);
    }


    @PreAuthorize ("hasRole('PLAYER_ROLE')")
    @PostMapping(value = {"/rooms/{roomId}/accept-invite"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Accept Room Invitation", description = "Accept Room Invitation (VS_FRIEND game mode)", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RoomDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Error", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
    })
    public ResponseEntity<RoomDto> acceptRoomInvitation(
            @Parameter(name = "roomId", required = true, description = "Room Id.") @PathVariable String roomId,
            @Parameter(name = "secondPlayerName", required = true, description = "Second player name") @RequestParam String secondPlayerName) {

        Room room = roomService.acceptRoomInvitation(roomId, secondPlayerName);
        return new ResponseEntity<>(roomMapper.roomToDto(room), HttpStatus.OK);
    }

}