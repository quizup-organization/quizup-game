package io.github.quizup.game.infrastructure.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.quizup.game.infrastructure.out.messaging.response.GameNotification;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Régression : l'enveloppe WS est sérialisée avec le type du payload **effacé** ({@code Object}).
 * Le discriminant `type` doit donc provenir de l'accesseur {@code type()} du record (et non de
 * {@code @JsonTypeInfo}, qui ne s'applique pas dans ce cas) — sinon le client ne peut pas folder.
 */
class GameNotificationSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void envelopeSerializesNotificationTypeWithoutPolymorphicTypeInfo() throws Exception {
        GameNotification notification = new GameNotification.RoundStartedNotification(
                "game-1",
                "ROUND_1",
                "q1",
                "Quel type est Pikachu ?",
                "https://example.com/pikachu.png",
                "EASY",
                Map.of("A", "Normal", "C", "Électrik"),
                false,
                Instant.parse("2026-09-18T10:00:00Z"),
                Instant.parse("2026-09-18T10:00:02Z")
        );

        NotificationEnvelope<GameNotification> envelope = new NotificationEnvelope<>(
                "n-4",
                "game-1",
                4,
                Instant.parse("2026-09-18T10:00:00Z"),
                notification
        );

        String json = objectMapper.writeValueAsString(envelope);

        assertTrue(json.contains("\"type\":\"ROUND_STARTED\""), json);
    }
}
