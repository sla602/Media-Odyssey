package com.mo.mediaodyssey.socialFeature.repositories;

import com.mo.mediaodyssey.socialFeature.models.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByBoardIdAndResolvedFalseOrderByCreatedAtDesc(Long boardId);

    long countByBoardIdAndResolvedFalse(Long boardId);

    boolean existsByPostIdAndReportedByUserId(Long postId, Long reportedByUserId);

    boolean existsByCommentIdAndReportedByUserId(Long commentId, Long reportedByUserId);

    @Modifying
    @Query("DELETE FROM Report r WHERE r.boardId = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);
}