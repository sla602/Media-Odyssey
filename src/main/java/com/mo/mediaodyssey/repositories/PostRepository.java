package com.mo.mediaodyssey.repositories;

import com.mo.mediaodyssey.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Integer> {
    List<Post> findByCommunityId(Integer communityId);

    List<Post> findByAuthorId(Integer authorId);

    boolean existsByIdAndAuthorId(Integer postId, Integer authorId);

}
