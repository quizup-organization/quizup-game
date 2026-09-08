package io.github.quizup.game.application.projection;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameRoundType;
import io.github.quizup.game.domain.model.GameRound;
import io.github.quizup.game.domain.model.GameRoundStatus;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GameProjection — Persiste l'état des parties dans les tables de projection.
 */
@Component
public class GameProjection {

    private static final Logger logger = LoggerFactory.getLogger(GameProjection.class);

    private final GameRepositoryPort gameRepositoryPort;

    public GameProjection(GameRepositoryPort gameRepositoryPort) {
        this.gameRepositoryPort = gameRepositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.GameCreatedEvent event) {
        logger.debug("Projecting GameCreatedEvent: gameId={}", event.gameId());

        List<GameQuestion> questions = event.questions();
        GameRoundType[] allRounds = GameRoundType.values();
        List<GameRound> rounds = java.util.stream.IntStream.range(0, questions.size())
                .mapToObj(index -> {
                    GameQuestion question = questions.get(index);
                    return GameRound.builder()
                            .round(allRounds[index])
                            .questionId(question.questionId())
                            .questionText(question.text())
                            .correctAnswer(question.correctAnswer())
                            .status(GameRoundStatus.CREATED)
                            .build();
                })
                .toList();

        gameRepositoryPort.save(
                Game.builder()
                        .gameId(event.gameId())
                        .topicId(event.topicId())
                        .player1Id(event.player1Id())
                        .player1Name(event.player1Name())
                        .player2Id(event.player2Id())
                        .player2Name(event.player2Name())
                        .mode(event.mode())
                        .opponent(event.player2Type())
                        .status(GameStatus.CREATED)
                        .player1Score(0)
                        .player2Score(0)
                        .createdAt(event.createdAt())
                        .rounds(rounds)
                        .build()
        );

        logger.info("Game projection created: gameId={}", event.gameId());
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.GameJoinedEvent event) {
        logger.debug("Projecting GameJoinedEvent: gameId={}, playerId={}", event.gameId(), event.playerId());
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.GameStartedEvent event) {
        logger.debug("Projecting GameStartedEvent: gameId={}", event.gameId());

        gameRepositoryPort.findById(event.gameId()).ifPresent(game ->
                gameRepositoryPort.save(game.toBuilder()
                        .status(GameStatus.IN_PROGRESS)
                        .startedAt(event.startedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.RoundStartedEvent event) {
        logger.debug("Projecting RoundStartedEvent: gameId={}, round={}", event.gameId(), event.round());
        gameRepositoryPort.findById(event.gameId()).ifPresent(game ->
                gameRepositoryPort.save(game.toBuilder()
                        .rounds(updateRound(game.rounds(), event.round(), round -> round.toBuilder()
                                .status(GameRoundStatus.STARTED)
                                .build()))
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.QuestionAnsweredEvent event) {
        logger.debug("Projecting QuestionAnsweredEvent: gameId={}, round={}, playerId={}",
                event.gameId(), event.round(), event.playerId());

        gameRepositoryPort.findById(event.gameId()).ifPresent(game -> {
            boolean isPlayer1 = event.playerId().equals(game.player1Id());
            gameRepositoryPort.save(game.toBuilder()
                    .rounds(updateRound(game.rounds(), event.round(), round -> isPlayer1
                            ? round.toBuilder().player1Choice(event.choice()).player1Points(event.pointsEarned()).build()
                            : round.toBuilder().player2Choice(event.choice()).player2Points(event.pointsEarned()).build()))
                    .build());
        });
    }


    @EventHandler
    @Transactional
    public void on(GameEvent.RoundClosedEvent event) {
        logger.debug("Projecting RoundClosedEvent: gameId={}, round={}", event.gameId(), event.closedRound());
        gameRepositoryPort.findById(event.gameId()).ifPresent(game ->
                gameRepositoryPort.save(game.toBuilder()
                        .rounds(updateRound(game.rounds(), event.closedRound(), round -> round.toBuilder()
                                .status(GameRoundStatus.CLOSED)
                                .build()))
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.GameEndedEvent event) {
        logger.debug("Projecting GameEndedEvent: gameId={}, winnerId={}", event.gameId(), event.winnerId());

        gameRepositoryPort.findById(event.gameId()).ifPresent(game ->
                gameRepositoryPort.save(game.toBuilder()
                        .status(GameStatus.FINISHED)
                        .winnerId(event.winnerId())
                        .player1Score(event.player1FinalScore())
                        .player2Score(event.player2FinalScore())
                        .endedAt(event.endedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.GameCancelledEvent event) {
        logger.debug("Projecting GameCancelledEvent: gameId={}", event.gameId());

        gameRepositoryPort.findById(event.gameId()).ifPresent(game ->
                gameRepositoryPort.save(game.toBuilder()
                        .status(GameStatus.CANCELED)
                        .build())
        );
    }

    private List<GameRound> updateRound(List<GameRound> rounds,
                                        GameRoundType targetRound,
                                        java.util.function.Function<GameRound, GameRound> updater) {
        return rounds.stream()
                .map(round -> round.round() == targetRound ? updater.apply(round) : round)
                .toList();
    }
}
