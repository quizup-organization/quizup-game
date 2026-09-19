package io.github.quizup.game.infrastructure.in.api.response;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO de synchronisation d'horloge client : temps serveur courant.
 */
public record ServerTimeResponse(
        Instant serverTime,
        long epochMillis
) implements Serializable {
}
