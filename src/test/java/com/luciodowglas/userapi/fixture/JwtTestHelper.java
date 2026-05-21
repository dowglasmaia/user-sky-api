package com.luciodowglas.userapi.fixture;

import com.luciodowglas.userapi.config.JwtProperties;
import com.luciodowglas.userapi.security.JwtUtil;

public final class JwtTestHelper {

    public static final String TEST_SECRET = "test-secret-key-that-is-exactly-32-chars!";
    private static final long EXPIRY_MS = 3_600_000L;
    private static final long EXPIRED_MS = -1L;

    private static final JwtUtil jwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, EXPIRY_MS));
    private static final JwtUtil expiredJwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, EXPIRED_MS));

    private JwtTestHelper() {}

    public static String tokenForUser() {
        return jwtUtil.generateToken(UserFixture.USER_EMAIL, "ROLE_USER");
    }

    public static String tokenForAdmin() {
        return jwtUtil.generateToken(UserFixture.ADMIN_EMAIL, "ROLE_ADMIN");
    }

    public static String expiredToken() {
        return expiredJwtUtil.generateToken(UserFixture.USER_EMAIL, "ROLE_USER");
    }

    public static String tokenWithBadSignature() {
        String validToken = tokenForUser();
        return validToken.substring(0, validToken.length() - 1) + "X";
    }
}
