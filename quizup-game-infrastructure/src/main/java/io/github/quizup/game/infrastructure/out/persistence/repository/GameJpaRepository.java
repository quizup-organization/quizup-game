package io.github.quizup.game.infrastructure.out.persistence.repository;

import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameJpaRepository extends JpaRepository<GameEntity, String>, JpaSpecificationExecutor<GameEntity> {

    @Query("""
            select g from GameEntity g
            where g.status = :status
              and g.mode = :mode
              and g.opponent = :opponent
              and (g.player1Id = :playerId or g.player2Id = :playerId)
            order by g.createdAt desc
            """)
    List<GameEntity> findActiveGamesByPlayerId(@Param("playerId") String playerId,
                                               @Param("status") GameStatus status,
                                               @Param("mode") GameMode mode,
                                               @Param("opponent") GamePlayerType opponent);
}
