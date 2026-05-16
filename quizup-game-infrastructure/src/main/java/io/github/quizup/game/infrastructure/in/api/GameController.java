package io.github.quizup.game.infrastructure.in.api;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.IdResponse;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.port.in.*;
import io.github.quizup.game.infrastructure.in.api.mapper.GameResponseMapper;
import io.github.quizup.game.infrastructure.in.api.request.AnswerQuestionRequest;
import io.github.quizup.game.infrastructure.in.api.request.CreateBotGameRequest;
import io.github.quizup.game.infrastructure.in.api.response.GameResponse;
import io.github.quizup.game.infrastructure.out.messaging.mapper.GameEventNotificationMapper;
import io.github.quizup.game.infrastructure.out.messaging.response.GameNotification;
import io.github.quizup.microservice.infrastructure.security.SecurityHelper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
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
    private final CancelGameUseCase cancelGameUseCase;
    private final GetGameUseCase getGameUseCase;
    private final GetGamesByUserUseCase getGamesByUserUseCase;
    private final GetGameEventsUseCase getGameEventsUseCase;
    private final SearchGameUseCase searchGameUseCase;

    public GameController(CreateGameUseCase createGameUseCase,
                          JoinGameUseCase joinGameUseCase,
                          AnswerQuestionUseCase answerQuestionUseCase,
                          CancelGameUseCase cancelGameUseCase,
                          GetGameUseCase getGameUseCase,
                          GetGamesByUserUseCase getGamesByUserUseCase,
                          GetGameEventsUseCase getGameEventsUseCase,
                          SearchGameUseCase searchGameUseCase) {
        this.createGameUseCase = createGameUseCase;
        this.joinGameUseCase = joinGameUseCase;
        this.answerQuestionUseCase = answerQuestionUseCase;
        this.cancelGameUseCase = cancelGameUseCase;
        this.getGameUseCase = getGameUseCase;
        this.getGamesByUserUseCase = getGamesByUserUseCase;
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

        String playerId = SecurityHelper.getUserId();
        String gameId = UUID.randomUUID().toString();

        logger.info("Creating bot game: gameId={}, topicId={}, playerId={}", gameId, request.topicId(), playerId);

        return createGameUseCase.create(
                        gameId,
                        request.topicId(),
                        playerId,
                        QuizUpConstants.BOT_USER_ID,
                        GameMode.SYNC,
                        GamePlayerType.BOT
                )
                .thenApply(aggregateId -> ResponseEntityBuilder.creation(ENDPOINT, aggregateId));
    }

    /**
     * Récupérer les parties de l'utilisateur connecté
     */
    @GetMapping
    public CompletableFuture<ResponseEntity<List<GameResponse>>> getGamesForUser(@RequestParam(required = false) GameStatus status) {

        String userId = SecurityHelper.getUserId();
        logger.debug("Getting games for userId={}, status={}", userId, status);

        return getGamesByUserUseCase.getByUser(userId, status)
                .thenApply(games -> games.stream().map(GameResponseMapper::toResponse).toList())
                .thenApply(ResponseEntity::ok);
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
     * Récupérer l'état complet d'une partie (game + rounds)
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
    public CompletableFuture<ResponseEntity<IdResponse>> joinGame(@PathVariable String gameId) {

        String playerId = SecurityHelper.getUserId();
        logger.info("Joining game: gameId={}, playerId={}", gameId, playerId);

        return joinGameUseCase.join(gameId, playerId)
                .thenApply(ResponseEntityBuilder::ok);
    }

    /**
     * Répondre à la question du round courant
     */
    @PostMapping("/{gameId}/answer")
    public CompletableFuture<ResponseEntity<IdResponse>> answerQuestion(@PathVariable String gameId, @RequestBody @Valid AnswerQuestionRequest request) {

        String playerId = SecurityHelper.getUserId();

        logger.info("Answering question: gameId={}, playerId={}, choice={}", gameId, playerId, request.choice());

        GameCommand.AnswerQuestionCommand command = new GameCommand.AnswerQuestionCommand(
                gameId,
                playerId,
                GameQuestionChoice.valueOf(request.choice().name()),
                Instant.now()
        );

        return answerQuestionUseCase.answer(command)
                .thenApply(ResponseEntityBuilder::ok);
    }

    /**
     * Annuler une partie
     */
    @PostMapping("/{gameId}/cancel")
    public CompletableFuture<ResponseEntity<IdResponse>> cancelGame(
            @PathVariable String gameId,
            @RequestParam(defaultValue = "Annulée par le joueur") String reason) {

        logger.info("Canceling game: gameId={}, reason={}", gameId, reason);

        return cancelGameUseCase.cancel(gameId, reason)
                .thenApply(ResponseEntityBuilder::ok);
    }
}
