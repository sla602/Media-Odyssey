package com.mo.mediaodyssey.layout.repositories;

import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.socialFeature.models.DTO.PostDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Replaces SocialSpaceRoleRepository.
 */
@Repository
public interface BoardRoleRepository extends JpaRepository<BoardRole, Long> {

    Optional<BoardRole> findByUserIdAndBoardId(Long userId, Long boardId);

    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    List<BoardRole> findByBoardId(Long boardId);

    void deleteByUserIdAndBoardId(Long userId, Long boardId);

    Long countByBoardId(Long boardId);

    // For ownership transfer
    List<BoardRole> findAllByBoardId(Long boardId);

    // For members list (alphabetical by userId)
    @Query("""
    SELECT br
    FROM BoardRole br
    WHERE br.boardId = :boardId
    ORDER BY br.userId ASC
""")
    List<BoardRole> findMembersByBoardId(@Param("boardId") Long boardId);


    // Search members
    @Query("""
    SELECT br
    FROM BoardRole br
    WHERE br.boardId = :boardId
    AND CAST(br.userId AS string) LIKE %:search%
    ORDER BY br.userId ASC
""")
    List<BoardRole> searchMembersByBoardId(@Param("boardId") Long boardId,
                                           @Param("search") String search);

    // Hard delete support
    void deleteByBoardId(Long boardId);
}