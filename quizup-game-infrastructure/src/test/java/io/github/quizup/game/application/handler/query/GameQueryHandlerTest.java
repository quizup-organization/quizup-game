package io.github.quizup.game.application.handler.query;

import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.model.TopicPopularity;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.domain.query.GameQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameQueryHandlerTest {

    private final GameRepositoryPort repository = mock(GameRepositoryPort.class);
    private final GameEventStorePort eventStore = mock(GameEventStorePort.class);
    private final GameQueryHandler handler = new GameQueryHandler(repository, eventStore);

    @Test
    void player_games_delegates_all_filters() {
        PlayerGamesPage page = PlayerGamesPage.builder()
                .games(List.of())
                .page(1)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .build();
        when(repository.findPlayerGames("me", "topic-1", "opponent-1", 1, 20)).thenReturn(page);

        PlayerGamesPage result = handler.handle(
                new GameQuery.GetPlayerGamesQuery("me", "topic-1", "opponent-1", 1, 20));

        assertEquals(page, result);
        verify(repository).findPlayerGames("me", "topic-1", "opponent-1", 1, 20);
    }

    @Test
    void popular_topics_delegates_window_and_limit() {
        Instant since = Instant.parse("2026-09-01T00:00:00Z");
        when(repository.findPopularTopics(since, 5))
                .thenReturn(List.of(new TopicPopularity("topic-1", 12)));

        List<TopicPopularity> result = handler.handle(new GameQuery.GetPopularTopicsQuery(since, 5));

        assertEquals(List.of(new TopicPopularity("topic-1", 12)), result);
        verify(repository).findPopularTopics(since, 5);
    }
}
