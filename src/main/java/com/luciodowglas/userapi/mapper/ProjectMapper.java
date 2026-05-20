package com.luciodowglas.userapi.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.luciodowglas.userapi.entity.UserProjectLink;

import br.com.luciodowglas.openapi.model.ExternalProjectResponse;
import br.com.luciodowglas.openapi.model.UserProjectsResponse;

@Component
public class ProjectMapper {

    public ExternalProjectResponse toResponse(UserProjectLink link) {
        return new ExternalProjectResponse()
                .id(link.getId().getProjectId())
                .name(link.getName())
                .description(link.getDescription());
    }

    public List<ExternalProjectResponse> toResponseList(List<UserProjectLink> links) {
        return links.stream().map(this::toResponse).toList();
    }

    public UserProjectsResponse toUserProjectsResponse(String userName, List<UserProjectLink> links) {
        return new UserProjectsResponse()
                .user(userName)
                .projects(toResponseList(links));
    }
}
