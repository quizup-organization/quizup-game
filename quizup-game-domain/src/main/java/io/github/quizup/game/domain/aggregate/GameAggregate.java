package io.github.quizup.game.domain.aggregate;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.*;
import io.github.quizup.game.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;
import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * GameAggregate — Cycle de vie d'une partie à deux joueurs (humain ou bot).
 * <p>
 * Utilise {@link GamePlayerAggregate} pour encapsuler l'état de chaque joueur
 * (identité, présence, score). La partie ne démarre que lorsque les deux joueurs ont rejoint
 * la salle d'attente. Les timeouts sont orchestrés par les sagas applicatives.
 */
@Aggregate
public class GameAggregate {
    private static final Logger logger = LoggerFactory.getLogger(GameAggregate.class);

    @AggregateIdentifier
    private String gameId;
    private String topicId;
    private GameStatus status;
    private final Map<GamePlayer, GamePlayerAggregate> players = new EnumMap<>(GamePlayer.class);
    private final Map<GameRoundType, GameRoundAggregate> rounds = new EnumMap<>(GameRoundType.class);
    private GameRoundType currentRound;

    private String rematchRequesterId;
    private boolean rematchAccepted;
    private Set<Language> rematchLanguages;
    private String rematchNewGameId;

    protected GameAggregate() {
    }

    // =============================================
    // COMMAND HANDLERS
    // =============================================

    @CommandHandler
    public GameAggregate(GameCommand.CreateGameCommand command,
                         QuestionRepositoryPort questionRepositoryPort) {
        logger.info("Creating game: gameId={}, topicId={}, player1={}, player2={}, player2Type={}",
                command.gameId(), command.topicId(), command.player1Id(), command.player2Id(), command.player2Type());

        if (StringUtils.isBlank(command.topicId())) {
            throw new GameExceptions.MissingTopicProblem(command.gameId());
        }
        if (StringUtils.isBlank(command.player1Id())) {
            throw new GameExceptions.MissingPlayerProblem(command.gameId(), GamePlayer.PLAYER_1);
        }
        if (StringUtils.isBlank(command.player2Id())) {
            throw new GameExceptions.MissingPlayerProblem(command.gameId(), GamePlayer.PLAYER_2);
        }

        List<GameQuestion> questions = questionRepositoryPort.findRandomApprovedByTopicId(
                command.topicId(),
                GameRules.TOTAL_ROUNDS,
                command.languages()
        );

        if (questions.size() < GameRules.TOTAL_ROUNDS) {
            throw new GameExceptions.NotEnoughQuestionsProblem(
                    command.gameId(), command.topicId(), questions.size(), command.languages());
        }

        apply(
                new GameEvent.GameCreatedEvent(
                        command.gameId(),
                        command.topicId(),
                        command.player1Id(),
                        command.player1Name(),
                        command.player2Id(),
                        command.player2Name(),
                        command.player2Type(),
                        questions,
                        command.botDifficulty(),
                        Instant.now()
                )
        );
    }

    /**
     * Un joueur confirme sa présence : dans la salle d'attente (avant démarrage) ou sur l'écran
     * de résultat (après la fin de la partie). Idempotent : un joueur déjà présent ne fait rien.
     * Quand les deux joueurs sont présents avant démarrage → statut {@code READY}.
     */
    @CommandHandler
    public void handle(GameCommand.JoinGameCommand command) {
        logger.info("Joining game: gameId={}, playerId={}", gameId, command.playerId());

        GamePlayerAggregate player = resolvePlayer(command.playerId());

        if (player.isPresent()) {
            logger.debug("Player already present, ignoring join: gameId={}, playerId={}", gameId, command.playerId());
            return;
        }
        if (status == GameStatus.CREATED || status == GameStatus.FINISHED) {
            apply(new GameEvent.GameJoinedEvent(gameId, command.playerId(), Instant.now()));
            return;
        }

        throw new GameExceptions.GameNotJoinableProblem(gameId, status.name());
    }

    /**
     * Un joueur quitte la partie : avant démarrage la partie est annulée ; après la fin de la
     * partie, seule la présence d'écran de résultat est libérée (et une éventuelle demande de
     * revanche en attente est annulée).
     */
    @CommandHandler
    public void handle(GameCommand.LeaveGameCommand command) {
        logger.info("Leaving game: gameId={}, playerId={}", gameId, command.playerId());

        if (status == GameStatus.CANCELED) {
            return;
        }
        if (status == GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameAlreadyStartedProblem(gameId, status.name());
        }

        GamePlayerAggregate player = resolvePlayer(command.playerId());

        Instant now = Instant.now();

        if (status == GameStatus.FINISHED) {
            if (!player.isPresent()) {
                return;
            }
            if (rematchPending()) {
                apply(new GameEvent.RematchCancelledEvent(gameId, "PLAYER_LEFT", now));
            }
            apply(new GameEvent.GameLeftEvent(gameId, command.playerId(), now));
            return;
        }

        apply(new GameEvent.GameLeftEvent(gameId, command.playerId(), now));
        apply(new GameEvent.GameCancelledEvent(gameId, "PLAYER_LEFT", now));
    }

    /**
     * Un joueur abandonne une partie en cours : l'adversaire est déclaré vainqueur.
     */
    @CommandHandler
    public void handle(GameCommand.ForfeitGameCommand command) {
        logger.info("Forfeiting game: gameId={}, playerId={}", gameId, command.playerId());

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameNotInProgressProblem(gameId, status == null ? "UNKNOWN" : status.name());
        }

        GamePlayerAggregate forfeiter = resolvePlayer(command.playerId());
        GamePlayerAggregate winner = opponentOf(forfeiter);

        Instant now = Instant.now();
        apply(new GameEvent.GameForfeitedEvent(gameId, command.playerId(), now));
        apply(buildGameEndedEvent(winner.getPlayerId(), now));
    }

    /**
     * Clôt la partie au score (fin normale après le dernier round, ou expiration serveur).
     */
    @CommandHandler
    public void handle(GameCommand.EndGameCommand command) {
        logger.info("Ending game: gameId={}", gameId);

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameNotInProgressProblem(gameId, status == null ? "UNKNOWN" : status.name());
        }

        apply(buildGameEndedEvent(resolveWinnerByScore(), Instant.now()));
    }

    @CommandHandler
    public void handle(GameCommand.CancelGameCommand command) {
        logger.info("Canceling game: gameId={}, reason={}", gameId, command.reason());

        if (status == GameStatus.FINISHED || status == GameStatus.CANCELED) {
            return;
        }

        apply(new GameEvent.GameCancelledEvent(gameId, command.reason(), Instant.now()));
    }

    @CommandHandler
    public void handle(GameCommand.StartGameCommand command) {
        logger.info("Starting game: gameId={}, status={}", gameId, status);

        if (status != GameStatus.READY) {
            throw new GameExceptions.GameNotReadyProblem(gameId, status.name());
        }

        Instant now = Instant.now();
        apply(new GameEvent.GameStartedEvent(gameId, now, now.plusMillis(GameRules.MATCH_INTRO_MS)));
    }

    @CommandHandler
    public void handle(GameCommand.StartRoundCommand command) {
        logger.info("Starting round: gameId={}, round={}", gameId, currentRound);

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameNotInProgressProblem(gameId, status.name());
        }

        GameRoundAggregate round = rounds.get(currentRound);
        GameRoundStatus roundStatus = round.getStatus();

        if (roundStatus == GameRoundStatus.QUESTION_SHOWN || roundStatus == GameRoundStatus.ANSWERABLE) {
            throw new GameExceptions.RoundNotStartableProblem(gameId, currentRound.name(), roundStatus.name());
        }

        Instant now = Instant.now();
        apply(
                new GameEvent.RoundStartedEvent(
                        gameId,
                        currentRound,
                        round.getQuestion(),
                        now,
                        now.plusMillis(GameRules.QUESTION_REVEAL_MS)
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.RevealQuestionCommand command) {
        logger.info("Revealing question: gameId={}, round={}", gameId, currentRound);

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameNotInProgressProblem(gameId, status.name());
        }

        GameRoundAggregate round = rounds.get(currentRound);

        if (round.getStatus() != GameRoundStatus.QUESTION_SHOWN) {
            throw new GameExceptions.RoundNotRevealableProblem(gameId, currentRound.name(), round.getStatus().name());
        }

        Instant now = Instant.now();
        apply(
                new GameEvent.QuestionRevealedEvent(
                        gameId,
                        currentRound,
                        now,
                        now.plusSeconds(GameRules.ROUND_TIMEOUT_SECONDS)
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.AnswerQuestionCommand command) {
        logger.info("Answering question: gameId={}, playerId={}, choice={}", gameId, command.playerId(), command.choice());

        if (StringUtils.isBlank(command.playerId())) {
            throw new GameExceptions.MissingPlayerIdProblem(gameId);
        }
        if (isNull(command.timestamp())) {
            throw new GameExceptions.MissingTimestampProblem(gameId);
        }

        GamePlayerAggregate player = resolvePlayer(command.playerId());
        GameRoundAggregate round = rounds.get(currentRound);

        if (!round.isAnswerable()) {
            throw new GameExceptions.RoundNotRevealedProblem(gameId, currentRound.name(), round.getStatus().name());
        }
        if (round.hasPlayerAnswered(player.getPlayer())) {
            throw new GameExceptions.RoundAlreadyAnsweredProblem(gameId, currentRound.name(), command.playerId());
        }

        Duration timeToAnswer = Duration.between(round.getRevealedAt(), command.timestamp());
        boolean correct = command.choice() != null && command.choice() == round.getQuestion().correctAnswer();

        int pointsEarned = 0;
        if (correct) {
            pointsEarned = GameRules.getBasePoints(currentRound.isBonus())
                    + GameRules.calculateSpeedBonus(timeToAnswer.toSeconds(), currentRound.isBonus());
        }

        long timeMs = round.getRevealedAt() != null
                ? Math.max(0, Duration.between(round.getRevealedAt(), command.timestamp()).toMillis())
                : 0;

        apply(
                new GameEvent.QuestionAnsweredEvent(
                        gameId,
                        currentRound,
                        round.getQuestion().questionId(),
                        command.playerId(),
                        player.getPlayerType(),
                        command.choice(),
                        correct,
                        command.timestamp(),
                        pointsEarned,
                        timeMs
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.CloseRoundCommand command) {
        logger.info("Closing round: gameId={}, round={}", gameId, currentRound);

        GameRoundAggregate round = rounds.get(currentRound);

        if (round.getStatus() != GameRoundStatus.ANSWERABLE) {
            throw new GameExceptions.RoundNotRevealedProblem(gameId, currentRound.name(), round.getStatus().name());
        }

        Instant now = Instant.now();
        GameRoundType nextRound = getNextRound(currentRound);

        apply(
                new GameEvent.RoundClosedEvent(
                        gameId,
                        currentRound,
                        nextRound,
                        round.getQuestion().correctAnswer(),
                        now,
                        nextRound != null ? now.plusMillis(GameRules.ROUND_TRANSITION_MS) : null
                )
        );
    }

    /**
     * Un joueur demande une revanche après la fin de la partie. Si une demande est déjà en
     * attente, le demandeur est ignoré (idempotent) et l'adversaire vaut acceptation
     * (double-clic simultané). Seuls les duels humains avec les deux joueurs présents sur
     * l'écran de résultat peuvent se rejouer.
     */
    @CommandHandler
    public void handle(GameCommand.RequestRematchCommand command) {
        logger.info("Requesting rematch: gameId={}, playerId={}", gameId, command.playerId());

        requireFinished();
        GamePlayerAggregate player = resolvePlayer(command.playerId());
        requireHumanOpponent();
        requireBothPlayersPresent();

        if (command.languages() == null || command.languages().isEmpty()) {
            throw new GameExceptions.MissingLanguagesProblem(gameId);
        }

        Instant now = Instant.now();

        if (rematchPending()) {
            if (player.getPlayerId().equals(rematchRequesterId)) {
                logger.debug("Rematch already requested, ignoring: gameId={}, playerId={}",
                        gameId, command.playerId());
                return;
            }
            if (rematchAccepted) {
                return;
            }
            apply(new GameEvent.RematchAcceptedEvent(gameId, command.playerId(), now));
            return;
        }

        GamePlayerAggregate opponent = opponentOf(player);
        apply(
                new GameEvent.RematchRequestedEvent(
                        gameId,
                        player.getPlayerId(),
                        player.getPlayerName(),
                        opponent.getPlayerId(),
                        opponent.getPlayerName(),
                        topicId,
                        command.languages(),
                        now
                )
        );
    }

    /**
     * L'adversaire accepte la demande de revanche en attente. Le demandeur ne peut pas
     * accepter sa propre demande.
     */
    @CommandHandler
    public void handle(GameCommand.AcceptRematchCommand command) {
        logger.info("Accepting rematch: gameId={}, playerId={}", gameId, command.playerId());

        requireFinished();
        requireRematchPending();

        if (rematchAccepted) {
            return;
        }

        GamePlayerAggregate player = resolvePlayer(command.playerId());

        if (player.getPlayerId().equals(rematchRequesterId)) {
            logger.debug("Requester cannot accept own rematch request: gameId={}, playerId={}",
                    gameId, command.playerId());
            return;
        }

        requireBothPlayersPresent();
        apply(new GameEvent.RematchAcceptedEvent(gameId, command.playerId(), Instant.now()));
    }

    /**
     * L'adversaire décline la demande de revanche en attente.
     */
    @CommandHandler
    public void handle(GameCommand.DeclineRematchCommand command) {
        logger.info("Declining rematch: gameId={}, playerId={}", gameId, command.playerId());

        requireFinished();
        requireRematchPending();

        GamePlayerAggregate player = resolvePlayer(command.playerId());

        if (player.getPlayerId().equals(rematchRequesterId)) {
            logger.debug("Requester cannot decline own rematch request: gameId={}, playerId={}",
                    gameId, command.playerId());
            return;
        }

        apply(new GameEvent.RematchDeclinedEvent(gameId, command.playerId(), Instant.now()));
    }

    /**
     * Le demandeur annule sa demande de revanche en attente.
     */
    @CommandHandler
    public void handle(GameCommand.CancelRematchCommand command) {
        logger.info("Canceling rematch: gameId={}, playerId={}", gameId, command.playerId());

        requireRematchPending();

        GamePlayerAggregate player = resolvePlayer(command.playerId());

        if (!player.getPlayerId().equals(rematchRequesterId)) {
            throw new GameExceptions.NotRematchRequesterProblem(gameId, command.playerId());
        }

        apply(new GameEvent.RematchCancelledEvent(gameId, "PLAYER_CANCELLED", Instant.now()));
    }

    /**
     * La saga confirme la création de la nouvelle partie : l'état de revanche est clos.
     */
    @CommandHandler
    public void handle(GameCommand.ConfirmRematchCommand command) {
        logger.info("Confirming rematch: gameId={}, newGameId={}", gameId, command.newGameId());

        requireRematchPending();

        if (!rematchAccepted) {
            throw new GameExceptions.RematchNotAcceptedProblem(gameId);
        }

        apply(new GameEvent.RematchStartedEvent(gameId, command.newGameId(), Instant.now()));
    }

    /**
     * La saga avorte la revanche en attente (échec de création ou expiration) — idempotent.
     */
    @CommandHandler
    public void handle(GameCommand.AbortRematchCommand command) {
        logger.info("Aborting rematch: gameId={}, reason={}", gameId, command.reason());

        if (!rematchPending()) {
            return;
        }

        apply(new GameEvent.RematchCancelledEvent(gameId, command.reason(), Instant.now()));
    }

    // =============================================
    // EVENT SOURCING HANDLERS
    // =============================================

    @EventSourcingHandler
    public void on(GameEvent.GameCreatedEvent event) {
        this.gameId = event.gameId();
        this.topicId = event.topicId();
        this.status = GameStatus.CREATED;
        this.currentRound = GameRoundType.ROUND_1;

        players.put(GamePlayer.PLAYER_1, new GamePlayerAggregate(GamePlayer.PLAYER_1, event.player1Id(), event.player1Name(), GamePlayerType.HUMAN));
        players.put(GamePlayer.PLAYER_2, new GamePlayerAggregate(GamePlayer.PLAYER_2, event.player2Id(), event.player2Name(), event.player2Type()));

        GameRoundType[] allRounds = GameRoundType.values();
        List<GameQuestion> questions = event.questions();

        for (int i = 0; i < questions.size(); i++) {
            rounds.put(allRounds[i], new GameRoundAggregate(allRounds[i], questions.get(i)));
        }
    }

    @EventSourcingHandler
    public void on(GameEvent.GameJoinedEvent event) {
        resolvePlayer(event.playerId()).join();

        if (status == GameStatus.CREATED && bothPlayersPresent()) {
            this.status = GameStatus.READY;
        }
    }

    @EventSourcingHandler
    public void on(GameEvent.GameLeftEvent event) {
        resolvePlayer(event.playerId()).leave();
    }

    @EventSourcingHandler
    public void on(GameEvent.GameStartedEvent event) {
        this.status = GameStatus.IN_PROGRESS;
    }

    @EventSourcingHandler
    public void on(GameEvent.GameCancelledEvent event) {
        this.status = GameStatus.CANCELED;
    }

    @EventSourcingHandler
    public void on(GameEvent.GameForfeitedEvent event) {
        logger.debug("Game forfeited: gameId={}, forfeiterId={}", gameId, event.forfeiterId());
    }

    @EventSourcingHandler
    public void on(GameEvent.RoundStartedEvent event) {
        rounds.get(event.round()).showQuestion(event.shownAt());
    }

    @EventSourcingHandler
    public void on(GameEvent.QuestionRevealedEvent event) {
        rounds.get(event.round()).reveal(event.revealedAt(), event.answerDeadlineAt());
    }

    @EventSourcingHandler
    public void on(GameEvent.QuestionAnsweredEvent event) {
        GameRoundAggregate round = rounds.get(event.round());
        GamePlayerAggregate player = resolvePlayer(event.playerId());
        round.recordAnswer(
                new PlayerAnswer(
                        player.getPlayer(),
                        event.choice(),
                        event.correct(),
                        event.pointsEarned(),
                        event.answeredAt()
                )
        );
        player.addScore(event.pointsEarned());
    }

    @EventSourcingHandler
    public void on(GameEvent.RoundClosedEvent event) {
        rounds.get(event.closedRound()).closeRound(event.closedAt());
        this.currentRound = event.nextRound();
    }

    @EventSourcingHandler
    public void on(GameEvent.GameEndedEvent event) {
        this.status = GameStatus.FINISHED;
        getPlayer(GamePlayer.PLAYER_1).leave();
        getPlayer(GamePlayer.PLAYER_2).leave();
        clearRematch();
    }

    @EventSourcingHandler
    public void on(GameEvent.RematchRequestedEvent event) {
        this.rematchRequesterId = event.requesterId();
        this.rematchLanguages = event.languages();
        this.rematchAccepted = false;
    }

    @EventSourcingHandler
    public void on(GameEvent.RematchAcceptedEvent event) {
        this.rematchAccepted = true;
    }

    @EventSourcingHandler
    public void on(GameEvent.RematchDeclinedEvent event) {
        clearRematch();
    }

    @EventSourcingHandler
    public void on(GameEvent.RematchCancelledEvent event) {
        clearRematch();
    }

    @EventSourcingHandler
    public void on(GameEvent.RematchStartedEvent event) {
        clearRematch();
        this.rematchNewGameId = event.newGameId();
    }

    // =============================================
    // UTILITY
    // =============================================

    /**
     * Résout le {@link GamePlayerAggregate} à partir d'un playerId.
     *
     * @throws GameExceptions.PlayerNotInGameProblem si le joueur n'est ni player1 ni player2
     */
    private GamePlayerAggregate resolvePlayer(String playerId) {
        return players.values().stream()
                .filter(gamePlayerAggregate -> gamePlayerAggregate.matches(playerId))
                .findFirst()
                .orElseThrow(() -> new GameExceptions.PlayerNotInGameProblem(gameId, playerId));
    }

    private GamePlayerAggregate getPlayer(GamePlayer slot) {
        return players.get(slot);
    }

    private GamePlayerAggregate opponentOf(GamePlayerAggregate player) {
        return GamePlayer.PLAYER_1.equals(player.getPlayer())
                ? getPlayer(GamePlayer.PLAYER_2)
                : getPlayer(GamePlayer.PLAYER_1);
    }

    private boolean bothPlayersPresent() {
        return getPlayer(GamePlayer.PLAYER_1).isPresent() && getPlayer(GamePlayer.PLAYER_2).isPresent();
    }

    private boolean rematchPending() {
        return rematchRequesterId != null;
    }

    private void clearRematch() {
        this.rematchRequesterId = null;
        this.rematchAccepted = false;
        this.rematchLanguages = null;
    }

    private void requireFinished() {
        if (status != GameStatus.FINISHED) {
            throw new GameExceptions.GameNotFinishedProblem(gameId, status == null ? "UNKNOWN" : status.name());
        }
    }

    private void requireRematchPending() {
        if (!rematchPending()) {
            throw new GameExceptions.RematchNotRequestedProblem(gameId);
        }
    }

    private void requireHumanOpponent() {
        if (!GamePlayerType.HUMAN.equals(getPlayer(GamePlayer.PLAYER_2).getPlayerType())) {
            throw new GameExceptions.BotRematchNotAllowedProblem(gameId);
        }
    }

    private void requireBothPlayersPresent() {
        if (!bothPlayersPresent()) {
            throw new GameExceptions.RematchPlayersNotPresentProblem(gameId);
        }
    }

    private String resolveWinnerByScore() {
        GamePlayerAggregate player1 = getPlayer(GamePlayer.PLAYER_1);
        GamePlayerAggregate player2 = getPlayer(GamePlayer.PLAYER_2);

        if (player1.getScore() > player2.getScore()) {
            return player1.getPlayerId();
        }
        if (player2.getScore() > player1.getScore()) {
            return player2.getPlayerId();
        }
        return null;
    }

    private GameEvent.GameEndedEvent buildGameEndedEvent(String winnerId, Instant endedAt) {
        GamePlayerAggregate player1 = getPlayer(GamePlayer.PLAYER_1);
        GamePlayerAggregate player2 = getPlayer(GamePlayer.PLAYER_2);

        return new GameEvent.GameEndedEvent(
                gameId,
                winnerId,
                player1.getPlayerId(),
                player1.getPlayerName(),
                player2.getPlayerId(),
                player2.getPlayerName(),
                topicId,
                player1.getScore(),
                player2.getScore(),
                countCorrect(GamePlayer.PLAYER_1),
                countFast(GamePlayer.PLAYER_1),
                countCorrect(GamePlayer.PLAYER_2),
                countFast(GamePlayer.PLAYER_2),
                endedAt
        );
    }

    private int countCorrect(GamePlayer player) {
        return (int) rounds.values().stream()
                .map(round -> round.getAnswer(player))
                .filter(answer -> answer != null && answer.correct())
                .count();
    }

    private int countFast(GamePlayer player) {
        return (int) rounds.values().stream()
                .filter(round -> {
                    PlayerAnswer answer = round.getAnswer(player);

                    if (answer == null || !answer.correct() || answer.answeredAt() == null || round.getRevealedAt() == null) {
                        return false;
                    }

                    return Duration.between(round.getRevealedAt(), answer.answeredAt()).toSeconds()
                            < GameRules.FAST_ANSWER_SECONDS;
                })
                .count();
    }

    private GameRoundType getNextRound(GameRoundType current) {
        GameRoundType[] allRounds = GameRoundType.values();
        int idx = current.ordinal();
        return (idx + 1 < allRounds.length) ? allRounds[idx + 1] : null;
    }
}
