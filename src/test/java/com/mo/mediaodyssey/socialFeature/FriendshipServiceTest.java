package com.mo.mediaodyssey.socialFeature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.layout.repositories.BoardRoleRepository;
import com.mo.mediaodyssey.recommendation.UserInteractionRepository;
import com.mo.mediaodyssey.shared.util.SqlLikeEscaper;
import com.mo.mediaodyssey.socialFeature.repositories.FriendshipRepository;
import com.mo.mediaodyssey.socialFeature.repositories.ProfileRepository;
import com.mo.mediaodyssey.socialFeature.services.FriendshipService;
import com.mo.mediaodyssey.socialFeature.services.ProfileService;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepo;

    @Mock
    private BoardRoleRepository boardRoleRepo;

    @Mock
    private ProfileService profileService;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private UserInteractionRepository interactionRepo;

    @InjectMocks
    private FriendshipService service;

    @Test
    void searchUsersByUsername_escapesLikeWildcardsBeforeQuerying() {
        Long viewerId = 7L;
        String rawQuery = "50%_\\off";

        when(profileRepo.searchByUsername(eq(SqlLikeEscaper.escape(rawQuery.trim())), eq(viewerId)))
                .thenReturn(List.of());

        assertThat(service.searchUsersByUsername(viewerId, rawQuery)).isEmpty();
        verify(profileRepo).searchByUsername(eq("50\\%\\_\\\\off"), eq(viewerId));
        verifyNoInteractions(friendshipRepo);
    }
}
