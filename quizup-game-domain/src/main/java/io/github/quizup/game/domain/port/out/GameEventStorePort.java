package io.github.quizup.game.domain.port.out;

import io.github.quizup.game.domain.event.GameEvent;

import java.util.List;

public interface GameEventStorePort {

    List<GameEvent> findEventsByGameId(String gameId);
}

