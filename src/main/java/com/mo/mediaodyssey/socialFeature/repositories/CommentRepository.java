package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment,Integer> {

    @Query("""
SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO(
    c.id,
    c.postId,
    c.authorId,
    c.parentId,
    c.content,
    u.username,
    0,
    c.deleted
)
FROM Comment c
JOIN User u ON c.authorId = u.id
WHERE c.postId = :postId
ORDER BY c.createdAt ASC
""")
    List<CommentDTO> findCommentsWithUser(@Param("postId") Integer postId);

}
