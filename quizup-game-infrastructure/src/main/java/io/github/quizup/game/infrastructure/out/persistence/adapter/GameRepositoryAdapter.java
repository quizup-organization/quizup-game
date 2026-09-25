package io.github.quizup.game.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import io.github.quizup.game.infrastructure.out.persistence.mapper.GameEntityMapper;
import io.github.quizup.game.infrastructure.out.persistence.repository.GameJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public Optional<Game> findActiveSyncGameByPlayerId(String playerId) {
        return gameJpaRepository.findActiveGamesByPlayerId(
                        playerId,
                        GameStatus.IN_PROGRESS,
                        GameMode.SYNC,
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
}
