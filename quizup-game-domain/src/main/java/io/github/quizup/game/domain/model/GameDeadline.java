package io.github.quizup.game.domain.model;

import java.time.Duration;

import static io.github.quizup.game.domain.model.GameRules.GAME_TIMEOUT_HOURS;
import static io.github.quizup.game.domain.model.GameRules.ROUND_TIMEOUT_SECONDS;

public interface GameDeadline {

    String GAME_EXPIRED = "game-expired";
    Duration GAME_EXPIRED_TIMEOUT = Duration.ofHours(GAME_TIMEOUT_HOURS);

    String ROUND_EXPIRED = "round-expired";
    Duration ROUND_EXPIRED_TIMEOUT = Duration.ofSeconds(ROUND_TIMEOUT_SECONDS);

    String NEXT_ROUND_STARTS = "next-round-starts";
    Duration NEXT_ROUND_STARTS_TIMEOUT = Duration.ofSeconds(3);
}