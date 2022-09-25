package com.mlmo.rpsgame.controller;

import com.mlmo.rpsgame.exception.*;
import com.mlmo.rpsgame.model.ErrorMessage;
import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Log
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleEntityNotFoundException(EntityNotFoundException entityNotFoundException) {
        return buildResponseEntity(entityNotFoundException, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GameOverException.class)
    public ResponseEntity<ErrorMessage> handleGameOverException(GameOverException gameOverException) {
        return buildResponseEntity(gameOverException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IncorrectPlayerRoundTurnException.class)
    public ResponseEntity<ErrorMessage> handleIncorrectPlayerRoundTurnException(IncorrectPlayerRoundTurnException incorrectPlayerRoundTurnException) {
        return buildResponseEntity(incorrectPlayerRoundTurnException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPlayerException.class)
    public ResponseEntity<ErrorMessage> handleInvalidPlayerExceptionException(InvalidPlayerException invalidPlayerException) {
        return buildResponseEntity(invalidPlayerException, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorMessage> buildResponseEntity(Throwable throwable, HttpStatus httpStatus) {
        ErrorMessage errorMessage = new ErrorMessage(throwable.getMessage());
        log.severe(errorMessage.getMessage());
        return new ResponseEntity<>(errorMessage, httpStatus);
    }

}




