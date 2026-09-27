package io.github.quizup.game.domain.model;

import lombok.Builder;

import java.util.List;

/**
 * Page d'historique des parties d'un joueur (query dédiée, sans {@code SearchRequest}).
 */
@Builder(toBuilder = true)
public record PlayerGamesPage(
        List<Game> games,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
