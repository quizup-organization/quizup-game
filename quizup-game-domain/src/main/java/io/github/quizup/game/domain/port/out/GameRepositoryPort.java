package io.github.quizup.game.domain.port.out;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.game.domain.model.Game;

import java.util.Optional;

public interface GameRepositoryPort {

    void save(Game game);

    Optional<Game> findById(String gameId);

    /**
     * Duel synchrone humain le plus récent auquel participe le joueur (pour le forfait
     * lorsqu'il passe hors ligne). Vide s'il n'a aucune partie active de ce type.
     */
    Optional<Game> findActiveSyncGameByPlayerId(String playerId);

    PageResult<Game> findAll(SearchCriteria searchCriteria);
}
