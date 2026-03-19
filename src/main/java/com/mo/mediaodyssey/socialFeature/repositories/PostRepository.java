package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import com.mo.mediaodyssey.socialFeature.models.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Integer> {

    List<Post> findByAuthorId(Integer authorId);

    boolean existsByIdAndAuthorId(Integer postId, Integer authorId);

    @Query("""
SELECT new com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO(
    p.id,
    p.socialSpaceId,
    p.title,
    p.content,
    u.username
)
FROM Post p
JOIN User u ON p.authorId = u.id
WHERE p.socialSpaceId = :communityId
ORDER BY p.createdAt DESC
""")
    List<PostDTO> findPostsWithUserByCommunityId(@Param("social_space_id") Integer socialSpaceId);

}
