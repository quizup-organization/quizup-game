package io.github.quizup.game.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.model.TopicPopularity;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import io.github.quizup.game.infrastructure.out.persistence.mapper.GameEntityMapper;
import io.github.quizup.game.infrastructure.out.persistence.repository.GameJpaRepository;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class GameRepositoryAdapter implements GameRepositoryPort {

    private final GameJpaRepository gameJpaRepository;

    private final JpaSearchAdapter<GameEntity> gameJpaSearchAdapter;

    public GameRepositoryAdapter(GameJpaRepository gameJpaRepository) {
        this.gameJpaRepository = gameJpaRepository;
        this.gameJpaSearchAdapter = new JpaSearchAdapter<>(gameJpaRepository, new AnnotationSearchableEntity(GameEntity.class));
    }

    @Override
    @Transactional
    public void save(Game game) {
        gameJpaRepository.save(GameEntityMapper.toEntity(game));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findById(String gameId) {
        return gameJpaRepository.findById(gameId).map(GameEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findActiveGameByPlayerId(String playerId) {
        return gameJpaRepository.findActiveGamesByPlayerId(
                        playerId,
                        GameStatus.IN_PROGRESS,
                        GamePlayerType.HUMAN
                ).stream()
                .findFirst()
                .map(GameEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse<Game> findAll(SearchRequest request) {
        return gameJpaSearchAdapter.findAll(request)
                .map(GameEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerGamesPage findPlayerGames(String playerId, String topicId, String opponentId, int page, int size) {
        Page<GameEntity> result = gameJpaRepository.findPlayerGames(
                playerId, topicId, opponentId, PageRequest.of(page, size));
        return PlayerGamesPage.builder()
                .games(result.getContent().stream().map(GameEntityMapper::toDomain).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicPopularity> findPopularTopics(Instant since, int limit) {
        return gameJpaRepository.findPopularTopics(since, Limit.of(limit)).stream()
                .map(row -> new TopicPopularity((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
