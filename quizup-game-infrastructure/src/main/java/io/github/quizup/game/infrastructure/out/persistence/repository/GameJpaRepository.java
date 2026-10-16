package io.github.quizup.game.infrastructure.out.persistence.repository;

import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
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

    /**
     * Questions distinctes déjà jouées par les joueurs sur un thème : rounds réellement affichés
     * ({@code status != CREATED}) des parties où le joueur a participé (peu importe le côté).
     */
    @Query("""
            select distinct r.questionId
            from GameEntity g join g.rounds r
            where g.topicId = :topicId
              and r.status <> io.github.quizup.game.domain.model.GameRoundStatus.CREATED
              and (g.player1Id in :playerIds or g.player2Id in :playerIds)
            """)
    List<String> findPlayedQuestionIds(@Param("topicId") String topicId,
                                       @Param("playerIds") Collection<String> playerIds);
}
