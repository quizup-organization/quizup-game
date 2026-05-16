package io.github.quizup.game.infrastructure.out.messaging;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.infrastructure.out.messaging.mapper.GameEventNotificationMapper;
import io.github.quizup.game.infrastructure.out.messaging.response.GameNotification;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * GameNotificationService — Push les events de gameplay en temps réel via WebSocket.
 */
@Service
public class GameNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(GameNotificationService.class);
    private static final String DESTINATION_PREFIX = "/topic/games/";
    private final SimpMessagingTemplate messagingTemplate;

    public GameNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void onGameEvent(GameEvent event) {
        GameEventNotificationMapper.toNotification(event)
                .ifPresentOrElse(
                        notification -> send(event.gameId(), notification),
                        () -> logger.warn("Aucun mapping de notification pour l'événement: {}", event.getClass().getSimpleName()));
    }

    private void send(String gameId, GameNotification payload) {
        logger.debug("{} publié: gameId={}", payload.type(), gameId);
        messagingTemplate.convertAndSend(DESTINATION_PREFIX + gameId, payload);
    }
}
