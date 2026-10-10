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
              and g.status = :status
            order by g.createdAt desc
            """)
    List<GameEntity> findInProgressGamesByPlayerId(@Param("playerId") String playerId,
                                                   @Param("status") GameStatus status);

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

    /**
     * Questions distinctes effectivement répondues par un joueur sur un thème (timeouts exclus) :
     * la clé {@code question_id} est dédupliquée entre les manches et les parties.
     */
    @Query("""
            select count(distinct r.questionId)
            from GameEntity g join g.rounds r
            where g.topicId = :topicId
              and (
                (g.player1Id = :playerId and r.player1Choice is not null)
                or (g.player2Id = :playerId and r.player2Choice is not null)
              )
            """)
    long countAnsweredQuestions(@Param("playerId") String playerId, @Param("topicId") String topicId);
}
