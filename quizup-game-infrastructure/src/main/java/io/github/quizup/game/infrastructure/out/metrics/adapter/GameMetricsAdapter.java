package io.github.quizup.game.infrastructure.out.metrics.adapter;

import io.github.quizup.game.domain.port.out.GameMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Adapter Micrometer du {@link GameMetricsPort}.
 *
 * <p>Les tags communs {@code application}/{@code environment}/{@code version} sont ajoutés
 * automatiquement par le SDK (`ObservabilityAutoConfiguration`).
 */
@Component
public class GameMetricsAdapter implements GameMetricsPort {

    private final MeterRegistry registry;

    public GameMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void gameCreated(String topicId, String mode, String opponentType) {
        Counter.builder("quizup.game.games.created")
                .tag("topic", safe(topicId))
                .tag("mode", safe(mode))
                .tag("opponent", safe(opponentType))
                .register(registry)
                .increment();
    }

    @Override
    public void gameStarted(String mode) {
        Counter.builder("quizup.game.games.started")
                .tag("mode", safe(mode))
                .register(registry)
                .increment();
    }

    @Override
    public void answerSubmitted(String playerType, boolean correct, long timeMs) {
        String type = safe(playerType);
        String correctness = Boolean.toString(correct);

        Counter.builder("quizup.game.answers")
                .tag("player_type", type)
                .tag("correct", correctness)
                .register(registry)
                .increment();

        Timer.builder("quizup.game.answer.duration")
                .tag("player_type", type)
                .tag("correct", correctness)
                // Histogramme de buckets pour calculer p50/p95/p99 côté PromQL.
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.ofMillis(Math.max(timeMs, 0L)));
    }

    @Override
    public void gameEnded(String outcome, boolean forfeited, int totalPoints) {
        Counter.builder("quizup.game.games.ended")
                .tag("outcome", safe(outcome))
                .tag("forfeited", Boolean.toString(forfeited))
                .register(registry)
                .increment();

        DistributionSummary.builder("quizup.game.final.points")
                .register(registry)
                .record(Math.max(totalPoints, 0));
    }

    @Override
    public void gameCancelled(String reason) {
        Counter.builder("quizup.game.games.cancelled")
                .tag("reason", safe(reason))
                .register(registry)
                .increment();
    }

    @Override
    public void runRecorded(String topicId, int score) {
        Counter.builder("quizup.game.runs.recorded")
                .tag("topic", safe(topicId))
                .register(registry)
                .increment();

        DistributionSummary.builder("quizup.game.run.score")
                .register(registry)
                .record(Math.max(score, 0));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
