package io.github.quizup.game.infrastructure.in.api;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.in.api.ResponseEntityBuilder;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.IdResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.port.in.*;
import io.github.quizup.game.infrastructure.in.api.mapper.GameResponseMapper;
import io.github.quizup.game.infrastructure.in.api.request.AnswerQuestionRequest;
import io.github.quizup.game.infrastructure.in.api.request.CreateAsyncGameRequest;
import io.github.quizup.game.infrastructure.in.api.request.CreateBotGameRequest;
import io.github.quizup.game.infrastructure.in.api.request.JoinGameRequest;
import io.github.quizup.game.infrastructure.in.api.response.GameResponse;
import io.github.quizup.game.infrastructure.in.api.response.ServerTimeResponse;
import io.github.quizup.game.infrastructure.out.messaging.mapper.GameEventNotificationMapper;
import io.github.quizup.game.infrastructure.out.messaging.response.GameNotification;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    private final CancelGameUseCase cancelGameUseCase;
    private final AbandonGameUseCase abandonGameUseCase;

    public GameController(CreateGameUseCase createGameUseCase,
                          JoinGameUseCase joinGameUseCase,
                          AnswerQuestionUseCase answerQuestionUseCase,
                          GetGameUseCase getGameUseCase,
                          GetGameEventsUseCase getGameEventsUseCase,
                          SearchGameUseCase searchGameUseCase,
                          CancelGameUseCase cancelGameUseCase,
                          AbandonGameUseCase abandonGameUseCase) {
        this.createGameUseCase = createGameUseCase;
        this.joinGameUseCase = joinGameUseCase;
        this.answerQuestionUseCase = answerQuestionUseCase;
        this.getGameUseCase = getGameUseCase;
        this.getGameEventsUseCase = getGameEventsUseCase;
        this.searchGameUseCase = searchGameUseCase;
        this.cancelGameUseCase = cancelGameUseCase;
        this.abandonGameUseCase = abandonGameUseCase;
    }

    /**
     * Temps serveur courant — base de la synchronisation d'horloge du client
     * (timer de manche fiable, indépendant du skew de l'horloge locale).
     */
    @GetMapping("/time")
    public ResponseEntity<ServerTimeResponse> serverTime() {
        Instant now = Instant.now();
        return ResponseEntity.ok(new ServerTimeResponse(now, now.toEpochMilli()));
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
                        request.playerName(),
                        QuizUpConstants.SYSTEM_USER_ID,
                        QuizUpConstants.SYSTEM_USER_NAME,
                        GameMode.SYNC,
                        GamePlayerType.BOT,
                        BotDifficulty.fromOrDefault(request.difficulty())
                )
                .thenApply(aggregateId -> ResponseEntityBuilder.creation(ENDPOINT, aggregateId));
    }

    /**
     * Créer une partie asynchrone : run solo (sans {@code ghostGameId}) ou replay contre un run
     * enregistré (avec {@code ghostGameId}). Orchestrée par
     * {@link io.github.quizup.game.application.saga.GameFlowSaga}.
     */
    @PostMapping("/async")
    public CompletableFuture<ResponseEntity<IdResponse>> createAsyncGame(@RequestBody @Valid CreateAsyncGameRequest request) {
        String gameId = UUID.randomUUID().toString();
        boolean replay = request.ghostGameId() != null && !request.ghostGameId().isBlank();
        return createGameUseCase.create(
                        gameId,
                        request.topicId(),
                        request.playerId(),
                        request.playerName(),
                        replay ? request.opponentId() : null,
                        replay ? request.opponentName() : null,
                        GameMode.ASYNC,
                        replay ? GamePlayerType.GHOST : GamePlayerType.HUMAN,
                        null,
                        request.ghostGameId()
                )
                .thenApply(aggregateId -> ResponseEntityBuilder.creation(ENDPOINT, aggregateId));
    }

    /**
     * Annuler une partie (abandon d'un joueur).
     */
    @PostMapping("/{gameId}/cancel")
    public CompletableFuture<ResponseEntity<IdResponse>> cancelGame(@PathVariable String gameId) {
        return cancelGameUseCase.cancel(gameId, "PLAYER_CANCELLED")
                .thenApply(ResponseEntityBuilder::ok);
    }

    /**
     * Abandonner une partie en cours : l'abandon donne la victoire à l'adversaire (forfait),
     * quelle que soit la nature de l'adversaire (humain, bot, fantôme) ou un run async solo.
     */
    @PostMapping("/{gameId}/abandon")
    public CompletableFuture<ResponseEntity<IdResponse>> abandonGame(@PathVariable String gameId) {
        String playerId = SecurityHelper.getUserId();
        return abandonGameUseCase.abandon(gameId, playerId)
                .thenApply(ResponseEntityBuilder::ok);
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
     * Récupérer les événements d'une partie (historique de notifications, avec métadonnées
     * d'ordre : identifiant, séquence, horodatage). Même contrat que le push WebSocket.
     */
    @GetMapping("/{gameId}/notifications")
    public CompletableFuture<ResponseEntity<Collection<NotificationEnvelope<GameNotification>>>> getGameNotificationsById(@PathVariable String gameId) {
        logger.debug("Getting game notifications: gameId={}", gameId);
        return getGameEventsUseCase.getEvents(gameId)
                .thenApply(envelopes -> envelopes.stream()
                        .map(envelope -> GameEventNotificationMapper.toNotification(envelope.payload())
                                .map(notification -> new NotificationEnvelope<>(
                                        envelope.notificationId(),
                                        envelope.aggregateId(),
                                        envelope.sequenceNumber(),
                                        envelope.occurredAt(),
                                        notification)))
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
