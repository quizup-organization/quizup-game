package io.github.quizup.game.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour créer une partie contre un bot.
 */
public record CreateBotGameRequest(
        @NotBlank String topicId
) {
}

