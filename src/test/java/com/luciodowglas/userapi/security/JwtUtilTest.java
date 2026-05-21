package com.luciodowglas.userapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.luciodowglas.userapi.config.JwtProperties;
import com.luciodowglas.userapi.fixture.JwtTestHelper;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(new JwtProperties(JwtTestHelper.TEST_SECRET, 3_600_000L));
    }

    @Test
    void token_generatedSuccessfully_isValidJwtStructure() {
        // when
        String token = jwtUtil.generateToken("alice@test.com", "ROLE_USER");

        // then
        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.chars().filter(c -> c == '.').count()).isEqualTo(2);
    }

    @Test
    void token_containsCorrectEmail_asSubject() {
        // given
        String token = jwtUtil.generateToken("alice@test.com", "ROLE_USER");

        // when
        String extracted = jwtUtil.extractEmail(token);

        // then
        assertThat(extracted).isEqualTo("alice@test.com");
    }

    @Test
    void token_containsCorrectRole_asClaim() {
        // given
        String token = jwtUtil.generateToken("admin@test.com", "ROLE_ADMIN");

        // when
        String role = jwtUtil.extractRole(token);

        // then
        assertThat(role).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void freshToken_isValid() {
        // given
        String token = jwtUtil.generateToken("alice@test.com", "ROLE_USER");

        // when / then
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void expiredToken_isRejectedWithoutException() {
        // given — token already expired at issuance
        JwtUtil expiredUtil = new JwtUtil(new JwtProperties(JwtTestHelper.TEST_SECRET, -1L));
        String token = expiredUtil.generateToken("alice@test.com", "ROLE_USER");

        // when / then
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void tamperedToken_isRejected() {
        // given
        String valid = jwtUtil.generateToken("alice@test.com", "ROLE_USER");
        String tampered = valid.substring(0, valid.length() - 1) + "X";

        // when / then
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void nullToken_isRejectedWithoutException() {
        // when / then
        assertThat(jwtUtil.isTokenValid(null)).isFalse();
    }

    @Test
    void emptyToken_isRejectedWithoutException() {
        // when / then
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }
}
