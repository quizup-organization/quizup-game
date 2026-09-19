package io.github.quizup.game.domain.port.out;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;

import java.util.List;

public interface GameEventStorePort {

    List<GameEvent> findEventsByGameId(String gameId);

    /** Flux d'événements d'une partie enrichi de leurs métadonnées (historique de notifications). */
    List<NotificationEnvelope<GameEvent>> findEventEnvelopesByGameId(String gameId);
}

