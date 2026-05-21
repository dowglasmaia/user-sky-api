package com.luciodowglas.userapi.service;

import static com.luciodowglas.userapi.fixture.UserFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luciodowglas.userapi.entity.UserProjectLink;
import com.luciodowglas.userapi.exception.ExternalIntegrationException;
import com.luciodowglas.userapi.exception.ProjectAlreadyExistsException;
import com.luciodowglas.userapi.exception.ProjectNotFoundException;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.integration.project.ExternalProjectGateway;
import com.luciodowglas.userapi.mapper.ProjectMapper;
import com.luciodowglas.userapi.repository.UserProjectLinkRepository;
import com.luciodowglas.userapi.repository.UserRepository;

import br.com.luciodowglas.openapi.model.LinkProjectRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class UserProjectLinkServiceTest {

    @Mock private UserProjectLinkRepository linkRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExternalProjectGateway projectGateway;

    private final ProjectMapper projectMapper = new ProjectMapper();
    private SimpleMeterRegistry meterRegistry;
    private UserProjectLinkService svc;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        svc = new UserProjectLinkService(linkRepository, userRepository, projectGateway, projectMapper, meterRegistry);
    }

    // ── Linking ───────────────────────────────────────────────────────────────

    @Test
    void user_canLinkProject_whenBothExistAndNotYetLinked() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(anExternalProjectDto());
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);
        when(linkRepository.save(any(UserProjectLink.class))).thenReturn(aUserProjectLink());

        // when
        svc.linkProject(USER_ID, PROJECT_ID, null);

        // then
        ArgumentCaptor<UserProjectLink> captor = ArgumentCaptor.forClass(UserProjectLink.class);
        verify(linkRepository).save(captor.capture());
        assertThat(captor.getValue().getId().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getId().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(captor.getValue().getName()).isEqualTo("Atlas");
    }

    @Test
    void projectLink_usesRequestDescription_whenProvidedAndNonBlank() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(anExternalProjectDto());
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);
        when(linkRepository.save(any(UserProjectLink.class))).thenReturn(aUserProjectLink());

        // when
        svc.linkProject(USER_ID, PROJECT_ID, new LinkProjectRequest().description("Custom override desc"));

        // then
        ArgumentCaptor<UserProjectLink> captor = ArgumentCaptor.forClass(UserProjectLink.class);
        verify(linkRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Custom override desc");
    }

    @Test
    void projectLink_fallsBackToExternalDescription_whenRequestDescriptionIsBlank() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(anExternalProjectDto());
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);
        when(linkRepository.save(any(UserProjectLink.class))).thenReturn(aUserProjectLink());

        // when
        svc.linkProject(USER_ID, PROJECT_ID, new LinkProjectRequest().description("   "));

        // then
        ArgumentCaptor<UserProjectLink> captor = ArgumentCaptor.forClass(UserProjectLink.class);
        verify(linkRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Customer platform");
    }

    @Test
    void projectLink_fallsBackToExternalDescription_whenRequestIsNull() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(anExternalProjectDto());
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);
        when(linkRepository.save(any(UserProjectLink.class))).thenReturn(aUserProjectLink());

        // when
        svc.linkProject(USER_ID, PROJECT_ID, null);

        // then
        ArgumentCaptor<UserProjectLink> captor = ArgumentCaptor.forClass(UserProjectLink.class);
        verify(linkRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Customer platform");
    }

    @Test
    void linkCounter_isIncremented_onSuccessfulLink() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(anExternalProjectDto());
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);
        when(linkRepository.save(any(UserProjectLink.class))).thenReturn(aUserProjectLink());

        // when
        svc.linkProject(USER_ID, PROJECT_ID, null);

        // then
        double count = meterRegistry.get("user.project.link.created").counter().count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void user_cannotLinkProject_whenUserDoesNotExist() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> svc.linkProject(USER_ID, PROJECT_ID, null))
                .isInstanceOf(UserNotFoundException.class);

        verify(projectGateway, never()).findById(any());
        verify(linkRepository, never()).save(any());
    }

    @Test
    void user_cannotLinkProject_whenExternalProjectDoesNotExist() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenThrow(new ProjectNotFoundException(PROJECT_ID));

        // when / then
        assertThatThrownBy(() -> svc.linkProject(USER_ID, PROJECT_ID, null))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(linkRepository, never()).save(any());
    }

    @Test
    void user_cannotLinkProject_whenExternalServiceIsDown() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID))
                .thenThrow(new ExternalIntegrationException("projects-api unavailable"));

        // when / then
        assertThatThrownBy(() -> svc.linkProject(USER_ID, PROJECT_ID, null))
                .isInstanceOf(ExternalIntegrationException.class);

        verify(linkRepository, never()).save(any());
    }

    @Test
    void user_cannotLinkProject_whenAlreadyLinked() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(anExternalProjectDto());
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> svc.linkProject(USER_ID, PROJECT_ID, null))
                .isInstanceOf(ProjectAlreadyExistsException.class);

        verify(linkRepository, never()).save(any());
    }

    @Test
    void persistenceFailure_isWrappedAsExternalIntegrationException() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(anExternalProjectDto());
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);
        when(linkRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // when / then
        assertThatThrownBy(() -> svc.linkProject(USER_ID, PROJECT_ID, null))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    // ── Unlinking ─────────────────────────────────────────────────────────────

    @Test
    void user_canUnlinkProject_whenLinkExists() {
        // given
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(true);

        // when
        svc.unlinkProject(USER_ID, PROJECT_ID);

        // then
        verify(linkRepository).deleteByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID);
    }

    @Test
    void user_cannotUnlinkProject_whenLinkDoesNotExist() {
        // given
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> svc.unlinkProject(USER_ID, PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(linkRepository, never()).deleteByIdUserIdAndIdProjectId(any(), any());
    }

    // ── Project listing ───────────────────────────────────────────────────────

    @Test
    void user_projectList_includesAllLinkedProjects_withoutExternalApiCall() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(linkRepository.findAllByIdUserId(USER_ID)).thenReturn(List.of(aUserProjectLink(), aUserProjectLink()));

        // when
        var result = svc.getProjects(USER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo("Alice");
        verify(projectGateway, never()).findById(any());
    }

    @Test
    void user_projectList_returnsEmptyList_whenNoProjectsLinked() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(aUser()));
        when(linkRepository.findAllByIdUserId(USER_ID)).thenReturn(List.of());

        // when
        var result = svc.getProjects(USER_ID);

        // then
        assertThat(result.getProjects()).isNotNull().isEmpty();
    }

    @Test
    void user_projectList_fails_whenUserDoesNotExist() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> svc.getProjects(USER_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(linkRepository, never()).findAllByIdUserId(any());
    }
}
