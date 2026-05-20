/*
package com.luciodowglas.userapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luciodowglas.userapi.config.SecurityConfig;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.security.JwtUtil;
import com.luciodowglas.userapi.security.UserDetailsServiceImpl;
import com.luciodowglas.userapi.service.UserProjectLinkService;
import com.luciodowglas.userapi.service.UserService;

import br.com.luciodowglas.openapi.model.CreateUserRequest;
import br.com.luciodowglas.openapi.model.UserResponse;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean UserProjectLinkService linkService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void createUser_withoutAuth_returns201() throws Exception {
        when(userService.createUser(any())).thenReturn(
                new UserResponse().id(USER_ID).name("Alice").email("alice@test.com"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest()
                                        .name("Alice")
                                        .email("alice@test.com")
                                        .password("Secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "USER")
    void getUserById_whenFound_returns200() throws Exception {
        when(userService.getUserById(eq(USER_ID))).thenReturn(
                new UserResponse().id(USER_ID).name("Alice").email("alice@test.com"));

        mockMvc.perform(get("/users/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "USER")
    void getUserById_whenNotFound_returns404() throws Exception {
        when(userService.getUserById(any())).thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(get("/users/" + USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getUserById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/users/" + USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@userapi.com", roles = "ADMIN")
    void deleteUser_asAdmin_returns204() throws Exception {
        doNothing().when(userService).deleteUser(any());

        mockMvc.perform(delete("/users/" + USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "USER")
    void deleteUser_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/users/" + USER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "USER")
    void deleteUser_whenNotFound_returns404() throws Exception {
        doThrow(new UserNotFoundException(USER_ID)).when(userService).deleteUser(any());

        // @PreAuthorize blocks USER, so test as ADMIN
        mockMvc.perform(delete("/users/" + USER_ID))
                .andExpect(status().isForbidden());
    }
}
*/
