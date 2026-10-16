package io.github.quizup.game.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameHistoryOutcome;
import io.github.quizup.game.domain.model.GameHistorySort;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.model.TopicPopularity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameRepositoryPort {

    void save(Game game);

    Optional<Game> findById(String gameId);

    /**
     * Partie active la plus récente à laquelle participe le joueur <b>face à un humain</b> (pour
     * le forfait lorsqu'il passe hors ligne). Vide s'il n'a aucune partie active.
     */
    Optional<Game> findActiveHumanGameByPlayerId(String playerId);

    /**
     * Parties en cours du joueur (`IN_PROGRESS`, humain ou bot), les plus récentes d'abord.
     * Potentiellement vide (reprise : aucune partie active).
     */
    List<Game> findInProgressGamesByPlayerId(String playerId);

    SearchResponse<Game> findAll(SearchRequest request);

    /**
     * Historique paginé des parties d'un joueur.
     *
     * <p>Filtres optionnels ({@code null} = pas de filtre) : {@code topicId}, {@code opponentId},
     * {@code opponentType} (type du joueur 2) et {@code outcome} (issue <b>relative au joueur
     * interrogé</b>, {@link GameHistoryOutcome#ALL} = aucun filtre). {@code sort} porte l'ordre
     * d'affichage ({@link GameHistorySort#RECENT} par défaut).</p>
     */
    PlayerGamesPage findPlayerGames(String playerId,
                                    String topicId,
                                    String opponentId,
                                    GamePlayerType opponentType,
                                    GameHistoryOutcome outcome,
                                    GameHistorySort sort,
                                    int page,
                                    int size);

    /**
     * Thèmes les plus joués (parties créées) depuis {@code since}, ordre décroissant.
     */
    List<TopicPopularity> findPopularTopics(Instant since, int limit);

    /**
     * Nombre de questions <b>distinctes</b> auxquelles le joueur a effectivement répondu sur un
     * thème (timeouts exclus) — progression « questions complétées » d'une fiche sujet.
     */
    int countAnsweredQuestions(String playerId, String topicId);

    /**
     * Questions <b>déjà jouées</b> par les joueurs sur un thème : questions de rounds réellement
     * affichés ({@code status != CREATED}), dédupliquées. Potentiellement vide.
     */
    List<String> findPlayedQuestionIds(String topicId, Collection<String> playerIds);
}
