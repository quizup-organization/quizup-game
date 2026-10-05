package io.github.quizup.game.infrastructure.out.persistence.repository;

import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GameJpaRepository extends JpaRepository<GameEntity, String>, JpaSpecificationExecutor<GameEntity> {

    @Query("""
            select g from GameEntity g
            where g.status = :status
              and g.opponent = :opponent
              and (g.player1Id = :playerId or g.player2Id = :playerId)
            order by g.createdAt desc
            """)
    List<GameEntity> findActiveGamesByPlayerId(@Param("playerId") String playerId,
                                               @Param("status") GameStatus status,
                                               @Param("opponent") GamePlayerType opponent);

    @Query("""
            select g from GameEntity g
            where (g.player1Id = :playerId or g.player2Id = :playerId)
              and g.status in :statuses
            order by g.createdAt desc
            """)
    List<GameEntity> findCurrentGamesByPlayerId(@Param("playerId") String playerId,
                                                @Param("statuses") List<GameStatus> statuses,
                                                Limit limit);

    @Query("""
            select g from GameEntity g
            where (g.player1Id = :playerId or g.player2Id = :playerId)
              and g.status = :status
            order by g.endedAt desc
            """)
    List<GameEntity> findLatestFinishedGamesByPlayerId(@Param("playerId") String playerId,
                                                       @Param("status") GameStatus status,
                                                       Limit limit);

    @Query("""
            select g from GameEntity g
            where (g.player1Id = :playerId or g.player2Id = :playerId)
              and (:topicId is null or g.topicId = :topicId)
              and (:opponentId is null or g.player1Id = :opponentId or g.player2Id = :opponentId)
            order by g.createdAt desc
            """)
    Page<GameEntity> findPlayerGames(@Param("playerId") String playerId,
                                     @Param("topicId") String topicId,
                                     @Param("opponentId") String opponentId,
                                     Pageable pageable);

    @Query("""
            select g.topicId, count(g)
            from GameEntity g
            where g.createdAt >= :since
            group by g.topicId
            order by count(g) desc, g.topicId asc
            """)
    List<Object[]> findPopularTopics(@Param("since") Instant since, Limit limit);
}
