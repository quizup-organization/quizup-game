package io.github.quizup.game.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

import java.time.Instant;
import java.util.Set;

public interface GameQuery {
    record SearchGameQuery(SearchRequest request) implements GameQuery {
    }

    record GetGameByIdQuery(String gameId) implements GameQuery {
    }

    /**
     * Parties en cours du joueur (`IN_PROGRESS`, humain ou bot), les plus récentes d'abord.
     * Collection potentiellement vide : l'absence de partie active n'est pas une erreur.
     */
    record GetActiveGamesQuery(String playerId) implements GameQuery {
    }

    record GetGameEventsQuery(String gameId) implements GameQuery {
    }

    /**
     * Historique paginé des parties d'un joueur.
     * {@code topicId} et {@code opponentId} sont optionnels ({@code null} = pas de filtre).
     */
    record GetPlayerGamesQuery(
            String playerId,
            String topicId,
            String opponentId,
            int page,
            int size
    ) implements GameQuery {
    }

    /**
     * Thèmes les plus joués depuis {@code since} (comptage des parties créées).
     */
    record GetPopularTopicsQuery(Instant since, int limit) implements GameQuery {
    }

    /**
     * Résultat détaillé d'une partie du point de vue d'un joueur (écran de fin).
     */
    record GetGameResultQuery(String gameId, String playerId) implements GameQuery {
    }

    /**
     * Nombre de questions <b>distinctes</b> répondues par un joueur sur un thème (barre
     * « questions complétées » de la fiche sujet). Réponses effectives uniquement (timeouts exclus).
     */
    record GetTopicCompletionQuery(String playerId, String topicId) implements GameQuery {
    }

    /**
     * Questions <b>déjà jouées</b> par les joueurs sur un thème : questions de rounds réellement
     * affichés ({@code status != CREATED}) des parties où le joueur a participé. Une partie
     * annulée avant son premier round ne consomme donc aucune question. Sert à la préparation de
     * salle (éviter les répétitions) — collection potentiellement vide.
     */
    record GetPlayedQuestionIdsQuery(
            String topicId,
            Set<String> playerIds
    ) implements GameQuery {
    }
}
