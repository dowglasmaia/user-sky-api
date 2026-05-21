package com.luciodowglas.userapi.service;

import java.util.List;
import java.util.UUID;

import com.luciodowglas.userapi.exception.ExternalIntegrationException;
import com.luciodowglas.userapi.exception.ProjectNotFoundException;
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
        log.info("[PROJECT][LINK][STARTED] userId={} projectId={}", userId, projectId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[PROJECT][LINK][FAILED] reason=user_not_found userId={}", userId);
                    return new UserNotFoundException(userId);
                });

        ExternalProjectDto project = projectGateway.findById(projectId);

        if (linkRepository.existsByIdUserIdAndIdProjectId(userId, projectId)) {
            log.error("[PROJECT][LINK][FAILED] reason=duplicate userId={} projectId={}", userId, projectId);
            throw new ProjectAlreadyExistsException(projectId, userId);
        }
        try {
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
            log.info("[PROJECT][LINK][SUCCESS] userId={} projectId={} name={}", userId, projectId, project.name());
            return projectMapper.toResponse(saved);
        } catch (Exception e) {
            log.error("[PROJECT][LINK][FAILED] reason=persistence_error userId={} projectId={}", userId, projectId, e);
            throw new ExternalIntegrationException("Failed to link project " + projectId + " to user " + userId, e);
        }
    }

    @Transactional
    public void unlinkProject(UUID userId, UUID projectId) {
        log.info("[PROJECT][UNLINK][STARTED] userId={} projectId={}", userId, projectId);

        if (!linkRepository.existsByIdUserIdAndIdProjectId(userId, projectId)) {
            log.error("[PROJECT][UNLINK][FAILED] reason=link_not_found userId={} projectId={}", userId, projectId);
            throw new ProjectNotFoundException(projectId, userId);
        }

        linkRepository.deleteByIdUserIdAndIdProjectId(userId, projectId);
        log.info("[PROJECT][UNLINK][SUCCESS] userId={} projectId={}", userId, projectId);
    }

    @Transactional(readOnly = true)
    public UserProjectsResponse getProjects(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        List<UserProjectLink> links = linkRepository.findAllByIdUserId(userId);
        log.debug("[PROJECT][LIST][SUCCESS] userId={} count={}", userId, links.size());
        return projectMapper.toUserProjectsResponse(user.getName(), links);
    }
}
