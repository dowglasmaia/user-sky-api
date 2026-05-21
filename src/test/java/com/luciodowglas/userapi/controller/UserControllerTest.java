package com.luciodowglas.userapi.controller;

import static com.luciodowglas.userapi.fixture.UserFixture.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luciodowglas.userapi.exception.ExternalIntegrationException;
import com.luciodowglas.userapi.exception.ProjectAlreadyExistsException;
import com.luciodowglas.userapi.exception.ProjectNotFoundException;
import com.luciodowglas.userapi.exception.UserAlreadyExistsException;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.security.JwtAuthFilter;
import com.luciodowglas.userapi.security.JwtUtil;
import com.luciodowglas.userapi.security.UserDetailsServiceImpl;
import com.luciodowglas.userapi.service.UserProjectLinkService;
import com.luciodowglas.userapi.service.UserService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.MethodSecurityConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityConfig {}

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean UserProjectLinkService linkService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtAuthFilter jwtAuthFilter;

    // ── POST /users ───────────────────────────────────────────────────────────

    @Test
    void user_canRegister_withValidCredentials() throws Exception {
        // given
        when(userService.createUser(any())).thenReturn(aUserResponse());

        // when / then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Secret123!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(USER_EMAIL));
    }

    @Test
    void user_registrationFails_whenEmailAlreadyTaken() throws Exception {
        // given
        when(userService.createUser(any())).thenThrow(new UserAlreadyExistsException(USER_EMAIL));

        // when / then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com","password":"Secret123!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("User already exists with email: " + USER_EMAIL));
    }

    @Test
    void user_registrationFails_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"not-an-email","password":"Secret123!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void user_registrationFails_whenPasswordIsMissing() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@test.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void user_registrationFails_whenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Alice"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ── GET /users ────────────────────────────────────────────────────────────

    @Test
    void authenticatedUser_canListAllUsers_inPagedFormat() throws Exception {
        // given
        when(userService.listUsers(0, 20)).thenReturn(aUserPageResponse());

        // when / then
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void pageAndSize_arePassedCorrectly_toService() throws Exception {
        // given
        when(userService.listUsers(2, 5)).thenReturn(aUserPageResponse());

        // when
        mockMvc.perform(get("/users").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        // then
        verify(userService).listUsers(2, 5);
    }

    // ── GET /users/{id} ───────────────────────────────────────────────────────

    @Test
    void authenticatedUser_canRetrieveUserById() throws Exception {
        // given
        when(userService.getUserById(USER_ID)).thenReturn(aUserResponse());

        // when / then
        mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(USER_EMAIL));
    }

    @Test
    void user_notFound_whenIdIsUnknown() throws Exception {
        // given
        when(userService.getUserById(any())).thenThrow(new UserNotFoundException(USER_ID));

        // when / then
        mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── PUT /users/{id} ───────────────────────────────────────────────────────

    @Test
    void authenticatedUser_canUpdateEmailAndName() throws Exception {
        // given
        when(userService.updateUser(eq(USER_ID), any())).thenReturn(aUserResponse());

        // when / then
        mockMvc.perform(put("/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"updated@test.com","name":"Alice Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL));
    }

    @Test
    void update_fails_whenUserNotFound() throws Exception {
        // given
        when(userService.updateUser(any(), any())).thenThrow(new UserNotFoundException(USER_ID));

        // when / then
        mockMvc.perform(put("/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_fails_whenNewEmailAlreadyTaken() throws Exception {
        // given
        when(userService.updateUser(any(), any())).thenThrow(new UserAlreadyExistsException("taken@test.com"));

        // when / then
        mockMvc.perform(put("/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"taken@test.com"}
                                """))
                .andExpect(status().isConflict());
    }

    // ── DELETE /users/{id} — ADMIN only ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canDeleteUser_whenUserExists() throws Exception {
        mockMvc.perform(delete("/users/{id}", USER_ID))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(USER_ID);
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUser_isBlocked_fromDeletingUsers() throws Exception {
        mockMvc.perform(delete("/users/{id}", USER_ID))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_deleteUser_fails_whenUserDoesNotExist() throws Exception {
        // given
        Mockito.doThrow(new UserNotFoundException(USER_ID)).when(userService).deleteUser(USER_ID);

        mockMvc.perform(delete("/users/{id}", USER_ID))
                .andExpect(status().isNotFound());
    }

    // ── GET /users/{id}/projects ───────────────────────────────────────────────

    @Test
    void authenticatedUser_canViewLinkedProjects() throws Exception {
        // given
        when(linkService.getProjects(USER_ID)).thenReturn(aUserProjectsResponse());

        // when / then
        mockMvc.perform(get("/users/{id}/projects", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("Alice"))
                .andExpect(jsonPath("$.projects").isArray());
    }

    @Test
    void projectListing_fails_whenUserNotFound() throws Exception {
        // given
        when(linkService.getProjects(any())).thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(get("/users/{id}/projects", USER_ID))
                .andExpect(status().isNotFound());
    }

    // ── POST /users/{userId}/projects/{projectId} ─────────────────────────────

    @Test
    void authenticatedUser_canLinkProjectToAccount() throws Exception {
        // given
        when(linkService.linkProject(eq(USER_ID), eq(PROJECT_ID), any()))
                .thenReturn(anExternalProjectResponse());

        // when / then
        mockMvc.perform(post("/users/{userId}/projects/{projectId}", USER_ID, PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Custom override desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Atlas"));
    }

    @Test
    void projectLink_succeedsWithoutBody_usingExternalDescription() throws Exception {
        // given — body is optional
        when(linkService.linkProject(eq(USER_ID), eq(PROJECT_ID), any()))
                .thenReturn(anExternalProjectResponse());

        mockMvc.perform(post("/users/{userId}/projects/{projectId}", USER_ID, PROJECT_ID))
                .andExpect(status().isCreated());
    }

    @Test
    void projectLink_fails_whenUserNotFound() throws Exception {
        // given
        when(linkService.linkProject(any(), any(), any()))
                .thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(post("/users/{userId}/projects/{projectId}", USER_ID, PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void projectLink_fails_whenProjectAlreadyLinked() throws Exception {
        // given
        when(linkService.linkProject(any(), any(), any()))
                .thenThrow(new ProjectAlreadyExistsException(PROJECT_ID, USER_ID));

        mockMvc.perform(post("/users/{userId}/projects/{projectId}", USER_ID, PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void projectLink_fails_whenExternalServiceIsUnavailable() throws Exception {
        // given
        when(linkService.linkProject(any(), any(), any()))
                .thenThrow(new ExternalIntegrationException("upstream down"));

        // when / then
        mockMvc.perform(post("/users/{userId}/projects/{projectId}", USER_ID, PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    // ── DELETE /users/{userId}/projects/{projectId} ───────────────────────────

    @Test
    void authenticatedUser_canUnlinkProject_whenLinkExists() throws Exception {
        mockMvc.perform(delete("/users/{userId}/projects/{projectId}", USER_ID, PROJECT_ID))
                .andExpect(status().isNoContent());

        verify(linkService).unlinkProject(USER_ID, PROJECT_ID);
    }

    @Test
    void unlink_fails_whenLinkDoesNotExist() throws Exception {
        // given
        Mockito.doThrow(new ProjectNotFoundException(PROJECT_ID, USER_ID))
                .when(linkService).unlinkProject(USER_ID, PROJECT_ID);

        mockMvc.perform(delete("/users/{userId}/projects/{projectId}", USER_ID, PROJECT_ID))
                .andExpect(status().isNotFound());
    }
}
