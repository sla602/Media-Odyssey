package com.mo.mediaodyssey.repositories;

import com.mo.mediaodyssey.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment,Integer> {

    List<Comment> findByPostId(Integer postId);

    List<Comment> findByAuthorId(Integer authorId);

    List<Comment> findByParentId(Integer parentId);

    List<Comment> findByPostIdAndParentIdIsNull(Integer postId);

    void deleteByPostId(Integer postId);

    List<Comment> findByPostIdOrderByParentIdAscCreatedAtAsc(Integer postId);

    List<Comment> findByPostIdOrderByCreatedAtAsc(Integer postId);

}
