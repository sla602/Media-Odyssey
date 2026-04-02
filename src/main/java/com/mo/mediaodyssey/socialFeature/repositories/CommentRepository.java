package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.Comment;
import com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment,Long> {

    @Query("SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.CommentDTO(" +
            "c.id, c.postId, c.authorId, c.parentId, c.content, c.deleted, c.createdAt) " +
            "FROM Comment c WHERE c.postId = :postId ORDER BY c.createdAt ASC")
    List<CommentDTO> findCommentsWithUser(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.postId IN :postIds")
    void deleteByPostIdIn(@Param("postIds") List<Long> postIds);
}
