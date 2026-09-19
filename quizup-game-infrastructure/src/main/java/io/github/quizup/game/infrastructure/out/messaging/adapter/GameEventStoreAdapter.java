package io.github.quizup.game.infrastructure.out.messaging.adapter;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GameEventStoreAdapter implements GameEventStorePort {

    private final EventStore eventStore;

    public GameEventStoreAdapter(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public List<GameEvent> findEventsByGameId(String gameId) {
        List<GameEvent> events = new ArrayList<>();
        DomainEventStream eventStream = eventStore.readEvents(gameId);
        while (eventStream.hasNext()) {
            Object payload = eventStream.next().getPayload();
            if (payload instanceof GameEvent gameEvent) {
                events.add(gameEvent);
            }
        }
        return events;
    }

    @Override
    public List<NotificationEnvelope<GameEvent>> findEventEnvelopesByGameId(String gameId) {
        List<NotificationEnvelope<GameEvent>> envelopes = new ArrayList<>();
        DomainEventStream eventStream = eventStore.readEvents(gameId);
        while (eventStream.hasNext()) {
            DomainEventMessage<?> message = eventStream.next();
            if (message.getPayload() instanceof GameEvent gameEvent) {
                envelopes.add(new NotificationEnvelope<>(
                        message.getIdentifier(),
                        message.getAggregateIdentifier(),
                        message.getSequenceNumber(),
                        message.getTimestamp(),
                        gameEvent
                ));
            }
        }
        return envelopes;
    }
}
