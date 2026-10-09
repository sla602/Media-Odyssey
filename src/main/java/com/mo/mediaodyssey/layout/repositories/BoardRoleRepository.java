package com.mo.mediaodyssey.layout.repositories;

import com.mo.mediaodyssey.layout.models.BoardRole;
import com.mo.mediaodyssey.socialFeature.enums.RoleType;
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

        // All boards a user has joined (excluding banned)
        List<BoardRole> findByUserIdAndRoleTypeNot(Long userId, RoleType roleType);

        // For ownership transfer
        List<BoardRole> findAllByBoardId(Long boardId);

        // All boards a user is actively in (for homepage — excludes BANNED and LEFT)
        @Query("SELECT br FROM BoardRole br WHERE br.userId = :userId " +
                        "AND br.roleType NOT IN (com.mo.mediaodyssey.socialFeature.enums.RoleType.BANNED, " +
                        "com.mo.mediaodyssey.socialFeature.enums.RoleType.LEFT)")
        List<BoardRole> findActiveByUserId(@Param("userId") Long userId);

        // Members list — only active members (excludes BANNED and LEFT)
        @Query("SELECT br FROM BoardRole br WHERE br.boardId = :boardId " +
                        "AND br.roleType NOT IN ( " +
                        "com.mo.mediaodyssey.socialFeature.enums.RoleType.LEFT) " +
                        "ORDER BY br.userId ASC")
        List<BoardRole> findMembersByBoardId(@Param("boardId") Long boardId);

        // Search active members only
        @Query("SELECT br FROM BoardRole br WHERE br.boardId = :boardId " +
                        "AND br.roleType NOT IN ( " +
                        "com.mo.mediaodyssey.socialFeature.enums.RoleType.LEFT) " +
                        "AND CAST(br.userId AS string) LIKE %:search% " +
                        "ORDER BY br.userId ASC")
        List<BoardRole> searchMembersByBoardId(@Param("boardId") Long boardId, @Param("search") String search);

        // Hard delete support
        void deleteByBoardId(Long boardId);
}