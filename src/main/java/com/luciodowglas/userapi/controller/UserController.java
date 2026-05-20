package com.luciodowglas.userapi.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.luciodowglas.userapi.service.UserProjectLinkService;
import com.luciodowglas.userapi.service.UserService;

import br.com.luciodowglas.openapi.api.UsersApi;
import br.com.luciodowglas.openapi.model.CreateUserRequest;
import br.com.luciodowglas.openapi.model.ExternalProjectResponse;
import br.com.luciodowglas.openapi.model.LinkProjectRequest;
import br.com.luciodowglas.openapi.model.UpdateUserRequest;
import br.com.luciodowglas.openapi.model.UserPageResponse;
import br.com.luciodowglas.openapi.model.UserProjectsResponse;
import br.com.luciodowglas.openapi.model.UserResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

    private final UserService userService;
    private final UserProjectLinkService linkService;

    @Override
    @RateLimiter(name = "userApiLimiter")
    public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest, String xCorrelationId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(createUserRequest));
    }

    @Override
    @RateLimiter(name = "userApiLimiter")
    public ResponseEntity<UserResponse> getUserById(UUID id, String xCorrelationId) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Override
    @RateLimiter(name = "userApiLimiter")
    public ResponseEntity<UserResponse> updateUser(UUID id, UpdateUserRequest updateUserRequest, String xCorrelationId) {
        return ResponseEntity.ok(userService.updateUser(id, updateUserRequest));
    }

    @Override
    @RateLimiter(name = "userApiLimiter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(UUID id, String xCorrelationId) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RateLimiter(name = "userApiLimiter")
    public ResponseEntity<UserPageResponse> listUsers(Integer page, Integer size, String xCorrelationId) {
        return ResponseEntity.ok(userService.listUsers(
                page != null ? page : 0,
                size != null ? size : 20));
    }

    @Override
    @RateLimiter(name = "userApiLimiter")
    public ResponseEntity<UserProjectsResponse> getUserProjects(UUID id, String xCorrelationId) {
        return ResponseEntity.ok(linkService.getProjects(id));
    }

    @Override
    @RateLimiter(name = "userApiLimiter")
    public ResponseEntity<ExternalProjectResponse> linkProjectToUser(UUID userId, UUID projectId,
                                                                      String xCorrelationId,
                                                                      LinkProjectRequest linkProjectRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(linkService.linkProject(userId, projectId, linkProjectRequest));
    }

    @Override
    @RateLimiter(name = "userApiLimiter")
    public ResponseEntity<Void> unlinkProjectFromUser(UUID userId, UUID projectId, String xCorrelationId) {
        linkService.unlinkProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }
}
