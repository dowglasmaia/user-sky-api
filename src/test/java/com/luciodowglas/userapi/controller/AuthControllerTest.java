package com.luciodowglas.userapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.luciodowglas.userapi.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luciodowglas.userapi.config.SecurityConfig;
import com.luciodowglas.userapi.security.JwtUtil;
import com.luciodowglas.userapi.security.UserDetailsServiceImpl;

import java.util.List;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
//@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthenticationManager authenticationManager;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @MockBean
    JwtAuthFilter jwtAuthFilter;

    @Test
    void user_receivesJwtToken_onSuccessfulLogin() throws Exception {
        // given
        var auth = new UsernamePasswordAuthenticationToken(
                "alice@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken("alice@test.com", "ROLE_USER")).thenReturn("jwt.token.value");

        // when / then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@test.com","password":"Secret123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.value"));
    }

    @Test
    void admin_receivesTokenWithAdminRole_onSuccessfulLogin() throws Exception {
        // given
        var auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken("admin@test.com", "ROLE_ADMIN")).thenReturn("admin.token");

        // when
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"Secret123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("admin.token"));

        // then — verify correct role was extracted and passed to token generator
        verify(jwtUtil).generateToken("admin@test.com", "ROLE_ADMIN");
    }

    @Test
    void login_isRejected_whenCredentialsAreInvalid() throws Exception {
        // given
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        // when / then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@test.com","password":"WrongPassword1!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void login_isRejected_whenEmailIsMissing() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Secret123!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_isRejected_whenPasswordIsMissing() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@test.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_isRejected_whenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
