package com.luciodowglas.userapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RestController;

import com.luciodowglas.userapi.security.JwtUtil;

import br.com.luciodowglas.openapi.api.AuthApi;
import br.com.luciodowglas.openapi.model.LoginRequest;
import br.com.luciodowglas.openapi.model.LoginResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request, String xCorrelationId) {
        log.info("login_attempt email={}", request.getEmail());

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        String token = jwtUtil.generateToken(request.getEmail(), role);
        log.info("login_success email={} role={}", request.getEmail(), role);
        return ResponseEntity.ok(new LoginResponse().token(token));
    }
}
