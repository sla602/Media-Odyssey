package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.BoardInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardInviteRepository extends JpaRepository<BoardInvite, Long> {

    /** All pending invites for a user for the Invitations section on friends.html. */
    List<BoardInvite> findByInviteeUserId(Long inviteeUserId);

    /** All pending invites for a single board (for "pending invitations" on the board settings page). */
    List<BoardInvite> findByBoardId(Long boardId);

    /** Check whether a specific user already has a pending invite to a specific board. */
    Optional<BoardInvite> findByBoardIdAndInviteeUserId(Long boardId, Long inviteeUserId);

    boolean existsByBoardIdAndInviteeUserId(Long boardId, Long inviteeUserId);

    /** Cleanup: delete all invites for a board (used when a board is hard-deleted). */
    @Modifying
    @Query("DELETE FROM BoardInvite b WHERE b.boardId = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);
}