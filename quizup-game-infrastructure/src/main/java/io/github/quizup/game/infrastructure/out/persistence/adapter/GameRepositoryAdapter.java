package io.github.quizup.game.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameHistoryOutcome;
import io.github.quizup.game.domain.model.GameHistorySort;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.model.TopicPopularity;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import io.github.quizup.game.infrastructure.out.persistence.mapper.GameEntityMapper;
import io.github.quizup.game.infrastructure.out.persistence.repository.GameJpaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
    public Optional<Game> findActiveHumanGameByPlayerId(String playerId) {
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
    public List<Game> findInProgressGamesByPlayerId(String playerId) {
        return gameJpaRepository
                .findInProgressGamesByPlayerId(playerId, GameStatus.IN_PROGRESS)
                .stream()
                .map(GameEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse<Game> findAll(SearchRequest request) {
        return gameJpaSearchAdapter.findAll(request)
                .map(GameEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerGamesPage findPlayerGames(String playerId,
                                           String topicId,
                                           String opponentId,
                                           GamePlayerType opponentType,
                                           GameHistoryOutcome outcome,
                                           GameHistorySort sort,
                                           int page,
                                           int size) {
        GameHistorySort order = sort == null ? GameHistorySort.RECENT : sort;
        Page<GameEntity> result = gameJpaRepository.findAll(
                playerGamesSpecification(playerId, topicId, opponentId, opponentType, outcome, order),
                PageRequest.of(page, size, chronologicalSort(order)));
        return PlayerGamesPage.builder()
                .games(result.getContent().stream().map(GameEntityMapper::toDomain).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Filtres de l'historique d'un joueur (tous optionnels hors participation). L'issue est
     * <b>relative au joueur interrogé</b> : {@code WIN}/{@code LOSS} se lisent sur le vainqueur,
     * jamais sur un côté fixe. Le tri {@link GameHistorySort#SCORE} impose son ordre ici (une
     * expression {@code case} sur le côté du joueur ne peut pas être portée par un {@link Sort}),
     * et jamais sur la requête de comptage.
     */
    private static Specification<GameEntity> playerGamesSpecification(String playerId,
                                                                     String topicId,
                                                                     String opponentId,
                                                                     GamePlayerType opponentType,
                                                                     GameHistoryOutcome outcome,
                                                                     GameHistorySort sort) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(
                    cb.equal(root.get("player1Id"), playerId),
                    cb.equal(root.get("player2Id"), playerId)));
            if (topicId != null && !topicId.isBlank()) {
                predicates.add(cb.equal(root.get("topicId"), topicId));
            }
            if (opponentId != null && !opponentId.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get("player1Id"), opponentId),
                        cb.equal(root.get("player2Id"), opponentId)));
            }
            if (opponentType != null) {
                predicates.add(cb.equal(root.get("opponent"), opponentType));
            }
            if (outcome != null && outcome != GameHistoryOutcome.ALL) {
                predicates.add(outcomePredicate(root, cb, playerId, outcome));
            }
            if (sort == GameHistorySort.SCORE && !Long.class.equals(query.getResultType())) {
                query.orderBy(cb.desc(myScore(root, cb, playerId)), cb.asc(root.get("gameId")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate outcomePredicate(Root<GameEntity> root,
                                              CriteriaBuilder cb,
                                              String playerId,
                                              GameHistoryOutcome outcome) {
        Path<GameStatus> status = root.get("status");
        Path<String> winnerId = root.get("winnerId");
        return switch (outcome) {
            case WIN -> cb.and(cb.equal(status, GameStatus.FINISHED), cb.equal(winnerId, playerId));
            case LOSS -> cb.and(
                    cb.equal(status, GameStatus.FINISHED),
                    cb.isNotNull(winnerId),
                    cb.notEqual(winnerId, playerId));
            case DRAW -> cb.and(cb.equal(status, GameStatus.FINISHED), cb.isNull(winnerId));
            case IN_PROGRESS -> cb.equal(status, GameStatus.IN_PROGRESS);
            case CANCELLED -> cb.equal(status, GameStatus.CANCELED);
            case ALL -> cb.conjunction();
        };
    }

    /** Score du joueur interrogé, quel que soit son côté dans la partie. */
    private static Expression<Integer> myScore(Root<GameEntity> root,
                                               CriteriaBuilder cb,
                                               String playerId) {
        return cb.<Integer>selectCase()
                .when(cb.equal(root.get("player1Id"), playerId), root.<Integer>get("player1Score"))
                .otherwise(root.<Integer>get("player2Score"));
    }

    private static Sort chronologicalSort(GameHistorySort sort) {
        if (sort == GameHistorySort.OLDEST) {
            return Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("gameId"));
        }
        if (sort == GameHistorySort.SCORE) {
            // Ordre porté par la Specification (expression `case`), jamais par le pageable.
            return Sort.unsorted();
        }
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("gameId"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicPopularity> findPopularTopics(Instant since, int limit) {
        return gameJpaRepository.findPopularTopics(since, Limit.of(limit)).stream()
                .map(row -> new TopicPopularity((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countAnsweredQuestions(String playerId, String topicId) {
        return (int) gameJpaRepository.countAnsweredQuestions(playerId, topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPlayedQuestionIds(String topicId, Collection<String> playerIds) {
        return gameJpaRepository.findPlayedQuestionIds(topicId, playerIds);
    }
}
