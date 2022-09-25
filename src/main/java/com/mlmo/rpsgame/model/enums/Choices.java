package com.mlmo.rpsgame.model.enums;

import com.mlmo.rpsgame.exception.EntityNotFoundException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Choices {
    ROCK(0),
    PAPER(1),
    SCISSORS(2);

    @Getter
    private final int value;

    Choices(int value) {
        this.value = value;
    }

    public static Choices findByValue(int value) {
        List<Choices> choices =
                Collections.synchronizedList(new ArrayList<>(Arrays.asList(Choices.values())));

        return choices.stream()
                .filter(choice -> choice.getValue() == value)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Invalid Choice. It should be ROCK, PAPER or SCISSORS"));
    }
}
