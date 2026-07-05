package com.mo.mediaodyssey.socialFeature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.layout.repositories.BoardsRepository;
import com.mo.mediaodyssey.socialFeature.repositories.CommentRepository;
import com.mo.mediaodyssey.socialFeature.repositories.PostRepository;
import com.mo.mediaodyssey.socialFeature.repositories.ReportRepository;
import com.mo.mediaodyssey.socialFeature.services.ModerationService;
import com.mo.mediaodyssey.shared.util.SqlLikeEscaper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ReportRepository reportRepo;

    @Mock
    private BoardRoleRepository roleRepo;

    @Mock
    private PostRepository postRepo;

    @Mock
    private CommentRepository commentRepo;

    @Mock
    private BoardsRepository boardsRepo;

    @InjectMocks
    private ModerationService service;

    @Test
    void searchBoardMembers_returnsEmptyForBlankQuery() {
        assertThat(service.searchBoardMembers(12L, "   ")).isEmpty();
        verifyNoInteractions(roleRepo);
    }

    @Test
    void searchBoardMembers_escapesLikeWildcardsBeforeQuerying() {
        Long boardId = 12L;
        String rawQuery = "12%_\\member";

        when(roleRepo.searchMembersByBoardId(eq(boardId), eq(SqlLikeEscaper.escape(rawQuery))))
                .thenReturn(List.of());

        assertThat(service.searchBoardMembers(boardId, rawQuery)).isEmpty();
        verify(roleRepo).searchMembersByBoardId(eq(boardId), eq("12\\%\\_\\\\member"));
    }
}
