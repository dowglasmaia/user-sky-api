package com.luciodowglas.userapi.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.security.RoleEnum;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TestEntityManager entityManager;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(User.builder()
                .email("alice@test.com")
                .name("Alice")
                .password("$2a$12$encoded")
                .role(RoleEnum.ROLE_USER)
                .build());
        entityManager.flush();
    }

    // ── Email existence checks ────────────────────────────────────────────────

    @Test
    void emailLookup_confirmsExistence_whenUserIsRegistered() {
        assertThat(userRepository.existsByEmail("alice@test.com")).isTrue();
    }

    @Test
    void emailLookup_returnsFalse_whenNoUserWithThatEmail() {
        assertThat(userRepository.existsByEmail("ghost@test.com")).isFalse();
    }

    // ── Find by email ─────────────────────────────────────────────────────────

    @Test
    void userCanBeFound_byEmail() {
        var result = userRepository.findByEmail("alice@test.com");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void emailSearch_returnsEmpty_whenEmailUnknown() {
        assertThat(userRepository.findByEmail("nobody@test.com")).isEmpty();
    }

    // ── Custom JPQL delete ────────────────────────────────────────────────────

    @Test
    void user_isRemovedByDirectQuery_returnsOneAffectedRow() {
        int affected = userRepository.deleteByIdDirect(alice.getId());
        entityManager.clear();

        assertThat(affected).isEqualTo(1);
        assertThat(userRepository.findById(alice.getId())).isEmpty();
    }

    @Test
    void directDelete_returnsZero_whenUserDoesNotExist() {
        int affected = userRepository.deleteByIdDirect(java.util.UUID.randomUUID());
        assertThat(affected).isEqualTo(0);
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    void userList_isSortedAlphabetically_byName() {
        userRepository.save(User.builder().email("charlie@test.com").name("Charlie").password("x").role(RoleEnum.ROLE_USER).build());
        userRepository.save(User.builder().email("bob@test.com").name("Bob").password("x").role(RoleEnum.ROLE_USER).build());
        entityManager.flush();

        var pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        var page = userRepository.findAll(pageable);

        assertThat(page.getContent())
                .extracting(User::getName)
                .containsExactly("Alice", "Bob", "Charlie");
    }

    // ── Unique constraint ─────────────────────────────────────────────────────

    @Test
    void duplicateEmail_isRejected_byDatabaseConstraint() {
        assertThatThrownBy(() -> {
            userRepository.save(User.builder()
                    .email("alice@test.com")
                    .name("Alice2")
                    .password("$2a$12$encoded")
                    .role(RoleEnum.ROLE_USER)
                    .build());
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }
}
