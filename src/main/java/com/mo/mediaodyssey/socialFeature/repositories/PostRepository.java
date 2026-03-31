package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {

    List<Post> findByAuthorId(Long authorId);

    boolean existsByIdAndAuthorId(Long postId, Long authorId);

    @Query("""

            SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO(
    p.id,
    p.boardId,
    p.authorId,
    p.title,
    p.content,
    p.deleted,
    p.createdAt
)
FROM Post p
JOIN User u ON p.authorId = u.id
WHERE p.boardId = :boardId
ORDER BY p.createdAt DESC
""")
    List<PostDTO> findPostsWithUserByBoardId(@Param("boardId") Long boardId);
}