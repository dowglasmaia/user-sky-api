package com.luciodowglas.userapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtil;

    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedRequest_setsSecurityContext_withCorrectEmailAndRole() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        when(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.jwt.token")).thenReturn("alice@test.com");
        when(jwtUtil.extractRole("valid.jwt.token")).thenReturn("ROLE_USER");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("alice@test.com");
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    void requestWithNoAuthorizationHeader_doesNotAuthenticateAndContinues() throws Exception {
        // given — no Authorization header

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).isTokenValid(any());
    }

    @Test
    void requestWithBasicAuthHeader_isIgnoredAndContinues() throws Exception {
        // given
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).isTokenValid(any());
    }

    @Test
    void requestWithInvalidToken_doesNotSetAuthentication() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer tampered.token");
        when(jwtUtil.isTokenValid("tampered.token")).thenReturn(false);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).extractEmail(any());
    }

    @Test
    void requestWithExpiredToken_doesNotSetAuthentication() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer expired.token");
        when(jwtUtil.isTokenValid("expired.token")).thenReturn(false);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // helper to avoid import of Mockito.any() conflicting with assertj
    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
