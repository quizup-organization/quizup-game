package io.github.quizup.game.infrastructure.in.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de création d'une partie asynchrone.
 *
 * <p>Sans {@code ghostGameId} : run solo enregistré (le joueur 1 joue seul).
 * Avec {@code ghostGameId} : replay contre le run enregistré
 * ({@code opponentId}/{@code opponentName} identifient le joueur rejoué).</p>
 */
public record CreateAsyncGameRequest(
        @NotBlank String topicId,
        @NotBlank String playerId,
        @NotBlank String playerName,
        String opponentId,
        String opponentName,
        String ghostGameId
) {
}
