package com.luciodowglas.userapi.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.exception.InsufficientPrivilegesException;
import com.luciodowglas.userapi.exception.UserAlreadyExistsException;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.mapper.UserMapper;
import com.luciodowglas.userapi.repository.UserRepository;
import com.luciodowglas.userapi.security.RoleEnum;

import br.com.luciodowglas.openapi.model.CreateUserRequest;
import br.com.luciodowglas.openapi.model.UpdateUserRequest;
import br.com.luciodowglas.openapi.model.UserRole;
import br.com.luciodowglas.openapi.model.UserPageResponse;
import br.com.luciodowglas.openapi.model.UserResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("[USER][CREATE][STARTED] email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[USER][CREATE][FAILED] reason=email_duplicate email={}", request.getEmail());
            throw new UserAlreadyExistsException(request.getEmail());
        }

        RoleEnum role = request.getRole() != null
                ? RoleEnum.valueOf(request.getRole().getValue())
                : RoleEnum.ROLE_USER;

        if (role == RoleEnum.ROLE_ADMIN) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(RoleEnum.ROLE_ADMIN.name()));
            if (!isAdmin) {
                log.warn("[USER][CREATE][FAILED] reason=insufficient_privileges requested_role=ROLE_ADMIN");
                throw new InsufficientPrivilegesException("Only administrators can create users with ROLE_ADMIN.");
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User saved = userRepository.save(user);
        log.info("[USER][CREATE][SUCCESS] id={} email={}", saved.getId(), saved.getEmail());
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.debug("[USER][FETCH][STARTED] id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("[USER][UPDATE][STARTED] id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                log.error("[USER][UPDATE][FAILED] reason=email_duplicate id={} email={}", id, request.getEmail());
                throw new UserAlreadyExistsException(request.getEmail());
            }
            log.debug("[USER][UPDATE][EMAIL_CHANGED] id={} old={} new={}", id, user.getEmail(), request.getEmail());
            user.setEmail(request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        User saved = userRepository.save(user);
        log.info("[USER][UPDATE][SUCCESS] id={}", id);
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserPageResponse listUsers(int page, int size) {
        log.debug("list_users page={} size={}", page, size);
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return userMapper.toPageResponse(userRepository.findAll(pageable));
    }

    @Transactional
    public void deleteUser(UUID id) {
        log.info("[USER][DELETE][STARTED] id={}", id);

        if (!userRepository.existsById(id)) {
            log.error("[USER][DELETE][FAILED] reason=not_found id={}", id);
            throw new UserNotFoundException(id);
        }

        userRepository.deleteByIdDirect(id);
        log.info("[USER][DELETE][SUCCESS] id={}", id);
    }
}
