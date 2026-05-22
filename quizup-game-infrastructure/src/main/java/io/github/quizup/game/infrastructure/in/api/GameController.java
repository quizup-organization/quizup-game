package io.github.quizup.game.infrastructure.in.api;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.IdResponse;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.port.in.*;
import io.github.quizup.game.infrastructure.in.api.mapper.GameResponseMapper;
import io.github.quizup.game.infrastructure.in.api.request.AnswerQuestionRequest;
import io.github.quizup.game.infrastructure.in.api.request.CreateBotGameRequest;
import io.github.quizup.game.infrastructure.in.api.request.JoinGameRequest;
import io.github.quizup.game.infrastructure.in.api.response.GameResponse;
import io.github.quizup.game.infrastructure.out.messaging.mapper.GameEventNotificationMapper;
import io.github.quizup.game.infrastructure.out.messaging.response.GameNotification;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.github.quizup.game.infrastructure.in.api.GameController.ENDPOINT;


@RestController
@RequestMapping(ENDPOINT)
public class GameController {

    public static final String ENDPOINT = "/api/games";
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    private final CreateGameUseCase createGameUseCase;
    private final JoinGameUseCase joinGameUseCase;
    private final AnswerQuestionUseCase answerQuestionUseCase;
    private final GetGameUseCase getGameUseCase;
    private final GetGameEventsUseCase getGameEventsUseCase;
    private final SearchGameUseCase searchGameUseCase;

    public GameController(CreateGameUseCase createGameUseCase,
                          JoinGameUseCase joinGameUseCase,
                          AnswerQuestionUseCase answerQuestionUseCase,
                          GetGameUseCase getGameUseCase,
                          GetGameEventsUseCase getGameEventsUseCase,
                          SearchGameUseCase searchGameUseCase) {
        this.createGameUseCase = createGameUseCase;
        this.joinGameUseCase = joinGameUseCase;
        this.answerQuestionUseCase = answerQuestionUseCase;
        this.getGameUseCase = getGameUseCase;
        this.getGameEventsUseCase = getGameEventsUseCase;
        this.searchGameUseCase = searchGameUseCase;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<GameResponse>>> search(@RequestBody SearchRequest searchRequest) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchGameUseCase.search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(GameResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Créer une partie contre un bot.
     * Génère un gameId, envoie CreateGameCommand et retourne le gameId.
     * La saga SyncBotGameFlowSaga prend le relais (auto-join bot, start, etc.)
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<IdResponse>> createBotGame(@RequestBody @Valid CreateBotGameRequest request) {
        String gameId = UUID.randomUUID().toString();
        return createGameUseCase.create(
                        gameId,
                        request.topicId(),
                        request.playerId(),
                        QuizUpConstants.BOT_USER_ID,
                        GameMode.SYNC,
                        GamePlayerType.BOT
                )
                .thenApply(aggregateId -> ResponseEntityBuilder.creation(ENDPOINT, aggregateId));
    }

    /**
     * Récupérer l'état complet d'une partie (game + rounds)
     */
    @GetMapping("/{gameId}")
    public CompletableFuture<ResponseEntity<GameResponse>> getGameById(@PathVariable String gameId) {
        logger.debug("Getting game: gameId={}", gameId);
        return getGameUseCase.getById(gameId)
                .thenApply(GameResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }


    /**
     * Récupérer les événements d'une partie
     */
    @GetMapping("/{gameId}/notifications")
    public CompletableFuture<ResponseEntity<Collection<GameNotification>>> getGameNotificationsById(@PathVariable String gameId) {
        logger.debug("Getting game notifications: gameId={}", gameId);
        return getGameEventsUseCase.getEvents(gameId)
                .thenApply(events -> events.stream()
                        .map(GameEventNotificationMapper::toNotification)
                        .flatMap(java.util.Optional::stream)
                        .toList())
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Rejoindre une partie (confirmer sa présence).
     * Quand les 2 joueurs ont rejoint → la partie est READY.
     */
    @PostMapping("/{gameId}/join")
    public CompletableFuture<ResponseEntity<IdResponse>> joinGame(@PathVariable String gameId, @RequestBody @Valid JoinGameRequest request) {
        return joinGameUseCase.join(gameId, request.playerId())
                .thenApply(ResponseEntityBuilder::ok);
    }

    /**
     * Répondre à la question du round courant
     */
    @PostMapping("/{gameId}/answer")
    public CompletableFuture<ResponseEntity<IdResponse>> answerQuestion(@PathVariable String gameId, @RequestBody @Valid AnswerQuestionRequest request) {
        return answerQuestionUseCase.answer(
                        gameId,
                        request.playerId(),
                        request.choice()
                )
                .thenApply(ResponseEntityBuilder::ok);
    }
}
