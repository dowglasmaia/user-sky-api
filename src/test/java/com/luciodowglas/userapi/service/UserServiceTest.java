/*
package com.luciodowglas.userapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.exception.UserAlreadyExistsException;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.mapper.UserMapper;
import com.luciodowglas.userapi.repository.UserRepository;
import com.luciodowglas.userapi.security.RoleEnum;

import br.com.luciodowglas.openapi.model.CreateUserRequest;
import br.com.luciodowglas.openapi.model.UpdateUserRequest;
import br.com.luciodowglas.openapi.model.UserResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void createUser_whenEmailFree_savesAndReturns201() {
        var request = new CreateUserRequest().name("Alice").email("alice@test.com").password("Secret123");
        var saved = User.builder().id(USER_ID).name("Alice").email("alice@test.com")
                .password("$encoded").role(RoleEnum.ROLE_USER).build();
        var expected = new UserResponse().id(USER_ID).name("Alice").email("alice@test.com");

        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("$encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(expected);

        var result = userService.createUser(request);

        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        verify(passwordEncoder).encode("Secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_whenEmailTaken_throwsConflict() {
        var request = new CreateUserRequest().name("Alice").email("alice@test.com").password("Secret123");
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_whenExists_returnsResponse() {
        var user = User.builder().id(USER_ID).email("alice@test.com").name("Alice").build();
        var expected = new UserResponse().id(USER_ID).email("alice@test.com").name("Alice");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        var result = userService.getUserById(USER_ID);

        assertThat(result.getId()).isEqualTo(USER_ID);
    }

    @Test
    void getUserById_whenMissing_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUser_whenEmailChangedAndFree_updates() {
        var user = User.builder().id(USER_ID).email("old@test.com").name("Alice").build();
        var request = new UpdateUserRequest().email("new@test.com").name("Alice Updated");
        var expected = new UserResponse().id(USER_ID).email("new@test.com").name("Alice Updated");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expected);

        var result = userService.updateUser(USER_ID, request);

        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void updateUser_whenNewEmailTaken_throwsConflict() {
        var user = User.builder().id(USER_ID).email("old@test.com").name("Alice").build();
        var request = new UpdateUserRequest().email("taken@test.com");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(USER_ID, request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void deleteUser_whenExists_callsRepository() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userService.deleteUser(USER_ID);

        verify(userRepository).deleteByIdDirect(USER_ID);
    }

    @Test
    void deleteUser_whenMissing_throwsNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).deleteByIdDirect(any());
    }
}
*/
