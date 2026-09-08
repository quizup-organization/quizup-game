package io.github.quizup.game.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import io.github.quizup.game.infrastructure.out.persistence.mapper.GameEntityMapper;
import io.github.quizup.game.infrastructure.out.persistence.repository.GameJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public List<Game> findByUserId(String userId) {
        return gameJpaRepository.findByPlayer1IdOrPlayer2Id(userId, userId)
                .stream()
                .map(GameEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> findByUserIdAndStatus(String userId, GameStatus status) {
        if (status == null) {
            return findByUserId(userId);
        }
        return gameJpaRepository.findByPlayer1IdAndStatusOrPlayer2IdAndStatus(userId, status, userId, status)
                .stream()
                .map(GameEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Game> findAll(SearchCriteria searchCriteria) {
        return gameJpaSearchAdapter.findAll(searchCriteria)
                .map(GameEntityMapper::toDomain);
    }
}

