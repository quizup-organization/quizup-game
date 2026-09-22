package io.github.quizup.game.infrastructure.out.messaging;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.infrastructure.out.messaging.mapper.GameEventNotificationMapper;
import io.github.quizup.game.infrastructure.out.messaging.response.GameNotification;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * GameNotificationService — Push les events de gameplay en temps réel via WebSocket, enrichis de
 * leurs métadonnées d'ordre (identifiant, séquence, horodatage) pour permettre au client de
 * reconstruire son état de façon déterministe (même contrat que l'historique REST).
 */
@Service
@ProcessingGroup("game-notification")
public class GameNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(GameNotificationService.class);
    private static final String DESTINATION_PREFIX = "/topic/games/";
    private final SimpMessagingTemplate messagingTemplate;

    public GameNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void onGameEvent(EventMessage<?> eventMessage) {
        if (!(eventMessage.getPayload() instanceof GameEvent event)) {
            return;
        }
        GameEventNotificationMapper.toNotification(event)
                .ifPresentOrElse(
                        notification -> send(event, eventMessage, notification),
                        () -> logger.warn("Aucun mapping de notification pour l'événement: {}", event.getClass().getSimpleName()));
    }

    private void send(GameEvent event, EventMessage<?> eventMessage, GameNotification payload) {
        if (!(eventMessage instanceof DomainEventMessage<?> domainMessage)) {
            logger.warn("Notification ignorée (événement sans métadonnées d'agrégat): {}", event.getClass().getSimpleName());
            return;
        }

        NotificationEnvelope<GameNotification> envelope = new NotificationEnvelope<>(
                domainMessage.getIdentifier(),
                event.gameId(),
                domainMessage.getSequenceNumber(),
                domainMessage.getTimestamp(),
                payload
        );

        logger.debug("{} publié: gameId={}, seq={}", payload.type(), event.gameId(), domainMessage.getSequenceNumber());
        messagingTemplate.convertAndSend(DESTINATION_PREFIX + event.gameId(), envelope);
    }
}
