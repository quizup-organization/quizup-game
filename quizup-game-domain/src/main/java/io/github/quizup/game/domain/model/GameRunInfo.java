package io.github.quizup.game.domain.model;

/**
 * Informations minimales d'une partie pour la validation d'un run asynchrone :
 * sujet, propriétaire (joueur 1) et mode.
 */
public record GameRunInfo(
        String gameId,
        String topicId,
        String player1Id,
        GameMode mode
) {
}
