/*
package com.luciodowglas.userapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.entity.UserProjectLink;
import com.luciodowglas.userapi.entity.UserProjectLinkId;
import com.luciodowglas.userapi.exception.ProjectAlreadyExistsException;
import com.luciodowglas.userapi.exception.ProjectNotFoundException;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.integration.project.ExternalProjectGateway;
import com.luciodowglas.userapi.integration.project.dto.ExternalProjectDto;
import com.luciodowglas.userapi.mapper.ProjectMapper;
import com.luciodowglas.userapi.repository.UserProjectLinkRepository;
import com.luciodowglas.userapi.repository.UserRepository;

import br.com.luciodowglas.openapi.model.ExternalProjectResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class UserProjectLinkServiceTest {

    @Mock private UserProjectLinkRepository linkRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExternalProjectGateway projectGateway;
    @Mock private ProjectMapper projectMapper;

    private static final UUID USER_ID    = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // Rebuild service with mocks via a helper since @InjectMocks conflicts with constructor injection
    private UserProjectLinkService serviceWithMocks() {
        return new UserProjectLinkService(
                linkRepository, userRepository, projectGateway, projectMapper,
                new SimpleMeterRegistry());
    }

    @Test
    void linkProject_whenBothExistAndNotDuplicate_createsLink() {
        var svc = serviceWithMocks();
        var user = User.builder().id(USER_ID).email("alice@test.com").build();
        var dto  = new ExternalProjectDto(PROJECT_ID, "Atlas", "Customer platform");
        var link = UserProjectLink.builder()
                .id(new UserProjectLinkId(USER_ID, PROJECT_ID))
                .user(user).name("Atlas").description("Customer platform").build();
        var expected = new ExternalProjectResponse().id(PROJECT_ID).name("Atlas");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectGateway.findById(PROJECT_ID)).thenReturn(dto);
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);
        when(linkRepository.save(any(UserProjectLink.class))).thenReturn(link);
        when(projectMapper.toResponse(link)).thenReturn(expected);

        var result = svc.linkProject(USER_ID, PROJECT_ID, null);

        assertThat(result.getId()).isEqualTo(PROJECT_ID);
        assertThat(result.getName()).isEqualTo("Atlas");
        verify(linkRepository).save(any(UserProjectLink.class));
    }

    @Test
    void linkProject_whenUserMissing_throwsNotFound() {
        var svc = serviceWithMocks();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.linkProject(USER_ID, PROJECT_ID, null))
                .isInstanceOf(UserNotFoundException.class);

        verify(linkRepository, never()).save(any());
    }

    @Test
    void linkProject_whenDuplicate_throwsConflict() {
        var svc = serviceWithMocks();
        var user = User.builder().id(USER_ID).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(projectGateway.findById(PROJECT_ID))
                .thenReturn(new ExternalProjectDto(PROJECT_ID, "Atlas", "desc"));
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(true);

        assertThatThrownBy(() -> svc.linkProject(USER_ID, PROJECT_ID, null))
                .isInstanceOf(ProjectAlreadyExistsException.class);

        verify(linkRepository, never()).save(any());
    }

    @Test
    void unlinkProject_whenExists_deletes() {
        var svc = serviceWithMocks();
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(true);

        svc.unlinkProject(USER_ID, PROJECT_ID);

        verify(linkRepository).deleteByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID);
    }

    @Test
    void unlinkProject_whenMissing_throwsNotFound() {
        var svc = serviceWithMocks();
        when(linkRepository.existsByIdUserIdAndIdProjectId(USER_ID, PROJECT_ID)).thenReturn(false);

        assertThatThrownBy(() -> svc.unlinkProject(USER_ID, PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void getProjects_whenUserExists_returnsList() {
        var svc = serviceWithMocks();
        var link = UserProjectLink.builder()
                .id(new UserProjectLinkId(USER_ID, PROJECT_ID)).name("Atlas").build();
        var expected = new ExternalProjectResponse().id(PROJECT_ID).name("Atlas");

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(linkRepository.findAllByIdUserId(USER_ID)).thenReturn(List.of(link));
        when(projectMapper.toResponseList(List.of(link))).thenReturn(List.of(expected));

        var result = svc.getProjects(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Atlas");
    }

    @Test
    void getProjects_whenUserMissing_throwsNotFound() {
        var svc = serviceWithMocks();
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> svc.getProjects(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }
}
*/
