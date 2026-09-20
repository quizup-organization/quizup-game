package io.github.quizup.game.application.handler.event;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.port.out.GameMetricsPort;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Alimente les KPI métier du gameplay à partir des événements du domaine Game.
 *
 * <p>Découplé de la projection de lecture : le domaine n'émet aucun compteur, c'est
 * l'infrastructure qui traduit les événements en métriques via {@link GameMetricsPort}.
 * Les handlers sont {@link DisallowReplay} : un reset/replay des projections ne réincrémente
 * pas les compteurs.
 */
@Component
public class GameMetricsEventHandler {

    private final GameMetricsPort metrics;

    public GameMetricsEventHandler(GameMetricsPort metrics) {
        this.metrics = metrics;
    }

    @EventHandler
    @DisallowReplay
    public void on(GameEvent.GameCreatedEvent event) {
        metrics.gameCreated(event.topicId(), name(event.mode()), name(event.player2Type()));
    }

    @EventHandler
    @DisallowReplay
    public void on(GameEvent.GameStartedEvent event) {
        metrics.gameStarted(name(event.mode()));
    }

    @EventHandler
    @DisallowReplay
    public void on(GameEvent.QuestionAnsweredEvent event) {
        metrics.answerSubmitted(name(event.playerType()), event.correct(), event.timeMs());
    }

    @EventHandler
    @DisallowReplay
    public void on(GameEvent.GameEndedEvent event) {
        boolean forfeited = event.forfeitById() != null;
        String outcome = forfeited ? "FORFEIT" : (event.winnerId() == null ? "DRAW" : "WIN");
        metrics.gameEnded(outcome, forfeited, event.player1FinalScore() + event.player2FinalScore());
    }

    @EventHandler
    @DisallowReplay
    public void on(GameEvent.GameCancelledEvent event) {
        metrics.gameCancelled(event.reason());
    }

    @EventHandler
    @DisallowReplay
    public void on(GameEvent.GameRunRecordedEvent event) {
        metrics.runRecorded(event.topicId(), event.score());
    }

    private static String name(Enum<?> value) {
        return value == null ? "unknown" : value.name();
    }
}
