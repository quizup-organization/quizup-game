package io.github.quizup.game.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour répondre à une question.
 */
public record JoinGameRequest(
        @NotBlank String playerId
) {
}

