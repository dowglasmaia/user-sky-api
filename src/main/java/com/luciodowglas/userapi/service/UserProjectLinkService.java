package com.luciodowglas.userapi.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.entity.UserProjectLink;
import com.luciodowglas.userapi.entity.UserProjectLinkId;
import com.luciodowglas.userapi.exception.ProjectAlreadyExistsException;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.integration.project.ExternalProjectGateway;
import com.luciodowglas.userapi.integration.project.dto.ExternalProjectDto;
import com.luciodowglas.userapi.mapper.ProjectMapper;
import com.luciodowglas.userapi.repository.UserProjectLinkRepository;
import com.luciodowglas.userapi.repository.UserRepository;

import br.com.luciodowglas.openapi.model.ExternalProjectResponse;
import br.com.luciodowglas.openapi.model.LinkProjectRequest;
import br.com.luciodowglas.openapi.model.UserProjectsResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Owns the lifecycle of the association between a user and an external project.
 *
 * <p>Projects are never created here: they must already exist in projects-api.
 * This service only resolves both ends and persists the link plus a local
 * snapshot of the project's display fields.</p>
 */
@Service
public class UserProjectLinkService {

    private static final Logger log = LoggerFactory.getLogger(UserProjectLinkService.class);

    private final UserProjectLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final ExternalProjectGateway projectGateway;
    private final ProjectMapper projectMapper;
    private final Counter linkCreatedCounter;

    public UserProjectLinkService(UserProjectLinkRepository linkRepository,
                                  UserRepository userRepository,
                                  ExternalProjectGateway projectGateway,
                                  ProjectMapper projectMapper,
                                  MeterRegistry meterRegistry) {
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.projectGateway = projectGateway;
        this.projectMapper = projectMapper;
        this.linkCreatedCounter = Counter.builder("user.project.link.created")
                .description("Number of user/project links created")
                .register(meterRegistry);
    }

    /**
     * Links an existing user to an existing external project.
     *
     * <p>Flow: (1) load the user, (2) resolve the project via the external
     * gateway, (3) reject duplicates, (4) persist the link.</p>
     */
    @Transactional
    public ExternalProjectResponse linkProject(UUID userId, UUID projectId, LinkProjectRequest request) {
        log.info("link_project_started userId={} projectId={}", userId, projectId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("link_project_user_missing userId={}", userId);
                    return new UserNotFoundException(userId);
                });

        ExternalProjectDto project = projectGateway.findById(projectId);

        if (linkRepository.existsByIdUserIdAndIdProjectId(userId, projectId)) {
            log.warn("link_project_duplicate userId={} projectId={}", userId, projectId);
            throw new ProjectAlreadyExistsException(projectId, userId);
        }

        String description = (request != null && StringUtils.hasText(request.getDescription()))
                ? request.getDescription()
                : project.description();

        UserProjectLink link = UserProjectLink.builder()
                .id(new UserProjectLinkId(userId, projectId))
                .user(user)
                .name(project.name())
                .description(description)
                .build();

        UserProjectLink saved = linkRepository.save(link);
        linkCreatedCounter.increment();
        log.info("link_project_created userId={} projectId={} projectName={}", userId, projectId, project.name());
        return projectMapper.toResponse(saved);
    }

    @Transactional
    public void unlinkProject(UUID userId, UUID projectId) {
        log.info("unlink_project_started userId={} projectId={}", userId, projectId);

        if (!linkRepository.existsByIdUserIdAndIdProjectId(userId, projectId)) {
            log.warn("unlink_project_missing userId={} projectId={}", userId, projectId);
            throw new com.luciodowglas.userapi.exception.ProjectNotFoundException(projectId, userId);
        }

        linkRepository.deleteByIdUserIdAndIdProjectId(userId, projectId);
        log.info("unlink_project_completed userId={} projectId={}", userId, projectId);
    }

    @Transactional(readOnly = true)
    public UserProjectsResponse getProjects(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        List<UserProjectLink> links = linkRepository.findAllByIdUserId(userId);
        log.debug("list_user_projects userId={} count={}", userId, links.size());
        return projectMapper.toUserProjectsResponse(user.getName(), links);
    }
}
