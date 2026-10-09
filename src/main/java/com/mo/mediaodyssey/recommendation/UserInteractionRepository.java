package com.mo.mediaodyssey.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    // Get all interactions for a user
    List<UserInteraction> findByUserId(Long userId);

    // Get all interactions of a specific type for a user (VIEW, LIKE, RATE)
    List<UserInteraction> findByUserIdAndInteractionType(Long userId, String interactionType);

    // Check if a specific interaction already exists for a user + media item
    boolean existsByUserIdAndMediaApiIdAndInteractionType(Long userId, String mediaApiId, String interactionType);

    // Get all mediaApiIds that a user has liked
    @Query("SELECT ui.mediaApiId FROM UserInteraction ui WHERE ui.userId = :userId AND ui.interactionType = 'LIKE'")
    List<String> findLikedMediaApiIdsByUserId(@Param("userId") Long userId);

    // Delete a LIKE interaction for a user + media item (used for unlike)
    @Transactional
    @Modifying
    @Query("DELETE FROM UserInteraction ui WHERE ui.userId = :userId AND ui.mediaApiId = :mediaApiId AND ui.interactionType = 'LIKE'")
    void deleteLikeByUserIdAndMediaApiId(@Param("userId") Long userId, @Param("mediaApiId") String mediaApiId);

    // ── Community Favourites Ranking Queries ──────────────────────────────────

    /**
     * Aggregates a Point System score per mediaApiId across all users.
     *
     * Formula: score = (VIEW count × 1) + (LIKE count × 10)
     *
     * Used by MediaRankingService to determine the Top 10 ranked media items
     * on the Community Favourites page.
     *
     * Returns a list of Object[] where:
     * [0] = mediaApiId (String)
     * [1] = mediaType (String) — "MOVIE", "GAME", or "SONG"
     * [2] = totalScore (Long)
     */
    @Query("""
                SELECT ui.mediaApiId, ui.mediaType,
                       SUM(CASE ui.interactionType
                           WHEN 'LIKE' THEN 10
                           WHEN 'VIEW' THEN 1
                           ELSE 0 END) AS totalScore
                FROM UserInteraction ui
                GROUP BY ui.mediaApiId, ui.mediaType
                ORDER BY totalScore DESC
                LIMIT 10
            """)
    List<Object[]> findTop10ByScore();

    /**
     * Same as findTop10ByScore but filtered to a specific media type.
     * Used when the user clicks the Movies, Games, or Songs tab.
     *
     * @param mediaType one of "MOVIE", "GAME", or "SONG"
     */
    @Query("""
                SELECT ui.mediaApiId, ui.mediaType,
                       SUM(CASE ui.interactionType
                           WHEN 'LIKE' THEN 10
                           WHEN 'VIEW' THEN 1
                           ELSE 0 END) AS totalScore
                FROM UserInteraction ui
                WHERE ui.mediaType = :mediaType
                GROUP BY ui.mediaApiId, ui.mediaType
                ORDER BY totalScore DESC
                LIMIT 10
            """)
    List<Object[]> findTop10ByScoreAndMediaType(@Param("mediaType") String mediaType);

    /**
     * Fast-Rising Top 5: items with the most LIKE interactions in the past 7 days.
     * Used for the trending section on the Community Favourites page.
     *
     * Returns a list of Object[] where:
     * [0] = mediaApiId (String)
     * [1] = mediaType (String)
     * [2] = likeCount (Long)
     */
    @Query("""
                SELECT ui.mediaApiId, ui.mediaType, COUNT(ui) AS likeCount
                FROM UserInteraction ui
                WHERE ui.interactionType = 'LIKE'
                AND ui.timestamp >= :since
                GROUP BY ui.mediaApiId, ui.mediaType
                ORDER BY likeCount DESC
                LIMIT 5
            """)
    List<Object[]> findTop5TrendingLikesSince(@Param("since") java.time.LocalDateTime since);

    /**
     * Returns likes and views count separately per mediaApiId for Top 10 ranking.
     *
     * Returns a list of Object[] where:
     * [0] = mediaApiId (String)
     * [1] = mediaType (String)
     * [2] = totalScore (Long)
     * [3] = likeCount (Long)
     * [4] = viewCount (Long)
     */
    @Query("""
                SELECT ui.mediaApiId, ui.mediaType,
                       SUM(CASE ui.interactionType
                           WHEN 'LIKE' THEN 10
                           WHEN 'VIEW' THEN 1
                           ELSE 0 END) AS totalScore,
                       SUM(CASE WHEN ui.interactionType = 'LIKE' THEN 1 ELSE 0 END) AS likeCount,
                       SUM(CASE WHEN ui.interactionType = 'VIEW' THEN 1 ELSE 0 END) AS viewCount
                FROM UserInteraction ui
                GROUP BY ui.mediaApiId, ui.mediaType
                ORDER BY totalScore DESC
                LIMIT 10
            """)
    List<Object[]> findTop10ByScoreWithCounts();

    /**
     * Same as findTop10ByScoreWithCounts but filtered to a specific media type.
     */
    @Query("""
                SELECT ui.mediaApiId, ui.mediaType,
                       SUM(CASE ui.interactionType
                           WHEN 'LIKE' THEN 10
                           WHEN 'VIEW' THEN 1
                           ELSE 0 END) AS totalScore,
                       SUM(CASE WHEN ui.interactionType = 'LIKE' THEN 1 ELSE 0 END) AS likeCount,
                       SUM(CASE WHEN ui.interactionType = 'VIEW' THEN 1 ELSE 0 END) AS viewCount
                FROM UserInteraction ui
                WHERE ui.mediaType = :mediaType
                GROUP BY ui.mediaApiId, ui.mediaType
                ORDER BY totalScore DESC
                LIMIT 10
            """)
    List<Object[]> findTop10ByScoreWithCountsAndMediaType(@Param("mediaType") String mediaType);


    /**
     * Find users (other than the given user) who have LIKED media of a
     * specific type that the given user has also LIKED. Returns
     * (otherUserId, overlapCount) pairs, ordered by overlap desc.
     *
     * Used for taste-based friend suggestions on friends.html.
     *
     * @param userId    the viewer whose taste we're matching against
     * @param mediaType "MOVIE", "GAME", or "SONG"
     */
    @Query("""
    SELECT other.userId, COUNT(other.mediaApiId) AS overlap
    FROM UserInteraction other
    WHERE other.interactionType = 'LIKE'
      AND other.mediaType = :mediaType
      AND other.userId <> :userId
      AND other.mediaApiId IN (
          SELECT mine.mediaApiId FROM UserInteraction mine
          WHERE mine.userId = :userId
            AND mine.interactionType = 'LIKE'
            AND mine.mediaType = :mediaType)
    GROUP BY other.userId
    ORDER BY overlap DESC
""")
    List<Object[]> findUsersWithLikeOverlapByMediaType(
            @Param("userId") Long userId,
            @Param("mediaType") String mediaType);
}