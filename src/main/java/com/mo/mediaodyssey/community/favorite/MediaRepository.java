package com.mo.mediaodyssey.community.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Media table.
 *
 * Used for:
 * - finding cached metadata rows
 * - rating-based ranking
 * - locating media by external API id + category
 */
public interface MediaRepository extends JpaRepository<Media, Long> {

    /**
     * Finds a cached media row using external API id and category.
     */
    Optional<Media> findByExternalApiIdAndCategoryIgnoreCase(String externalApiId, String category);

    /**
     * Top 10 by average rating across all categories.
     *
     * Ordered by:
     * 1. average rating desc
     * 2. rating count desc
     * 3. title asc
     */
    @Query(value = """
                SELECT *
                FROM media
                WHERE rating_count > 0
                ORDER BY
                    CAST(rating_sum AS double precision) / NULLIF(rating_count, 0) DESC,
                    rating_count DESC,
                    title ASC
                LIMIT 10
            """, nativeQuery = true)
    List<Media> findTop10ByRating();

    /**
     * Top 10 by average rating within a specific category.
     */
    @Query(value = """
                SELECT *
                FROM media
                WHERE rating_count > 0
                  AND UPPER(category) = UPPER(:category)
                ORDER BY
                    CAST(rating_sum AS double precision) / NULLIF(rating_count, 0) DESC,
                    rating_count DESC,
                    title ASC
                LIMIT 10
            """, nativeQuery = true)
    List<Media> findTop10ByRatingAndCategory(@Param("category") String category);
}
