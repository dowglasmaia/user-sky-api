package com.luciodowglas.userapi.service;

import static com.luciodowglas.userapi.fixture.UserFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.exception.UserAlreadyExistsException;
import com.luciodowglas.userapi.exception.UserNotFoundException;
import com.luciodowglas.userapi.mapper.UserMapper;
import com.luciodowglas.userapi.repository.UserRepository;
import com.luciodowglas.userapi.security.RoleEnum;

import br.com.luciodowglas.openapi.model.UpdateUserRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private final UserMapper userMapper = new UserMapper();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, userMapper);
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    void user_canRegisterSuccessfully_whenEmailIsNotYetTaken() {
        // given
        var request = aCreateUserRequest();
        var saved = aUser();

        when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("$2a$12$encodedpasswordhash");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        // when
        var result = userService.createUser(request);

        // then
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void user_registrationAlwaysAssignsRoleUser_neverAdmin() {
        // given
        when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("$encoded");
        when(userRepository.save(any(User.class))).thenReturn(aUser());

        // when
        userService.createUser(aCreateUserRequest());

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(RoleEnum.ROLE_USER);
    }

    @Test
    void user_passwordIsNeverStoredInPlainText() {
        // given
        when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("$2a$12$encodedpasswordhash");
        when(userRepository.save(any(User.class))).thenReturn(aUser());

        // when
        userService.createUser(aCreateUserRequest());

        // then
        verify(passwordEncoder).encode(RAW_PASSWORD);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$12$encodedpasswordhash");
        assertThat(captor.getValue().getPassword()).doesNotContain(RAW_PASSWORD);
    }

    @Test
    void user_cannotRegister_whenEmailAlreadyExists() {
        // given
        when(userRepository.existsByEmail(USER_EMAIL)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> userService.createUser(aCreateUserRequest()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(USER_EMAIL);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    @Test
    void user_canBeFoundById_whenExists() {
        // given
        var user = aUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // when
        var result = userService.getUserById(USER_ID);

        // then
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getEmail()).isEqualTo(USER_EMAIL);
    }

    @Test
    void user_retrievalFails_whenIdIsUnknown() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userService.getUserById(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void user_canUpdateEmailAndName_whenNewEmailIsAvailable() {
        // given
        var user = User.builder().id(USER_ID).email("old@test.com").name("Alice").role(RoleEnum.ROLE_USER).build();
        var request = new UpdateUserRequest().email("new@test.com").name("Alice Updated");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        // when
        var result = userService.updateUser(USER_ID, request);

        // then
        assertThat(result.getEmail()).isEqualTo("new@test.com");
        verify(userRepository).save(user);
    }

    @Test
    void user_emailUpdate_skipsUniquenessCheck_whenEmailUnchanged() {
        // given
        var user = User.builder().id(USER_ID).email(USER_EMAIL).name("Alice").role(RoleEnum.ROLE_USER).build();
        var request = new UpdateUserRequest().email(USER_EMAIL).name("New Name");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // when
        userService.updateUser(USER_ID, request);

        // then — email is the same, no uniqueness check should be performed
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void user_cannotUpdateEmail_whenNewEmailIsTakenByAnotherUser() {
        // given
        var user = User.builder().id(USER_ID).email("old@test.com").name("Alice").role(RoleEnum.ROLE_USER).build();
        var request = new UpdateUserRequest().email("taken@test.com");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> userService.updateUser(USER_ID, request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("taken@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void user_nameIsNotCleared_whenNameIsOmittedFromUpdateRequest() {
        // given
        var user = User.builder().id(USER_ID).email(USER_EMAIL).name("Alice").role(RoleEnum.ROLE_USER).build();
        var request = new UpdateUserRequest().name(null); // name intentionally omitted

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // when
        userService.updateUser(USER_ID, request);

        // then — null name must not overwrite the existing name
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Alice");
    }

    @Test
    void user_updateFails_whenUserDoesNotExist() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userService.updateUser(USER_ID, anUpdateUserRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── Listing ───────────────────────────────────────────────────────────────

    @Test
    void users_areListedAlphabetically_byNameAscending() {
        // given
        var page = new PageImpl<>(List.of(aUser()), PageRequest.of(0, 20, Sort.by("name").ascending()), 1);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        // when
        userService.listUsers(0, 20);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("name");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void users_listRespects_pageAndSizeParameters() {
        // given
        PageImpl<User> page = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        // when
        userService.listUsers(2, 5);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    @Test
    void user_canBeDeleted_whenExists() {
        // given
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        // when
        userService.deleteUser(USER_ID);

        // then — must use custom JPQL query, not the default JPA deleteById
        verify(userRepository).deleteByIdDirect(USER_ID);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void user_deletionFails_whenUserDoesNotExist() {
        // given
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());

        verify(userRepository, never()).deleteByIdDirect(any());
    }
}
