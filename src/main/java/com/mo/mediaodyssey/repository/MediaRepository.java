package com.mo.mediaodyssey.repository;

import com.mo.mediaodyssey.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {

    // Sort all media by likes (highest first)
    List<Media> findAllByOrderByLikesDesc();

    // Sort all media by views (highest first)
    List<Media> findAllByOrderByViewsDesc();

    /**
     * Top 10 by Point System: (views * 1) + (likes * 5)
     * Used for the main ranked list when no category filter is applied.
     */
    @Query("SELECT m FROM Media m ORDER BY (m.views * 1 + m.likes * 5) DESC LIMIT 10")
    List<Media> findTop10ByScore();

    /**
     * Top 10 by Point System filtered by a specific category (Game / Movie / Song).
     * Used when the user clicks a tab on the community page.
     */
    @Query("SELECT m FROM Media m WHERE m.category = :category ORDER BY (m.views * 1 + m.likes * 5) DESC LIMIT 10")
    List<Media> findTop10ByScoreAndCategory(@Param("category") String category);

    /**
     * Fast-Rising Top 5: sorted by (likes - likesLastWeek) descending.
     * Items with the greatest weekly growth appear first.
     */
    @Query("SELECT m FROM Media m ORDER BY (m.likes - m.likesLastWeek) DESC LIMIT 5")
    List<Media> findTop5Trending();
}