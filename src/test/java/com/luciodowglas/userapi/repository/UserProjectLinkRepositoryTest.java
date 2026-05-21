package com.luciodowglas.userapi.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.entity.UserProjectLink;
import com.luciodowglas.userapi.entity.UserProjectLinkId;
import com.luciodowglas.userapi.security.RoleEnum;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UserProjectLinkRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private UserProjectLinkRepository linkRepository;
    @Autowired private TestEntityManager entityManager;

    private User user;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("alice@test.com")
                .name("Alice")
                .password("$2a$12$encoded")
                .role(RoleEnum.ROLE_USER)
                .build());

        projectId = UUID.fromString("11111111-0000-0000-0000-000000000001");

        linkRepository.save(UserProjectLink.builder()
                .id(new UserProjectLinkId(user.getId(), projectId))
                .user(user)
                .name("Atlas")
                .description("Customer platform")
                .build());

        entityManager.flush();
    }

    // ── Existence checks ──────────────────────────────────────────────────────

    @Test
    void linkExistence_isConfirmed_whenUserProjectPairIsLinked() {
        assertThat(linkRepository.existsByIdUserIdAndIdProjectId(user.getId(), projectId)).isTrue();
    }

    @Test
    void linkExistence_returnsFalse_forUnknownPair() {
        assertThat(linkRepository.existsByIdUserIdAndIdProjectId(user.getId(), UUID.randomUUID())).isFalse();
    }

    // ── Find by user ──────────────────────────────────────────────────────────

    @Test
    void allLinksForUser_areReturned_withCorrectUserId() {
        UUID projectId2 = UUID.fromString("22222222-0000-0000-0000-000000000002");
        linkRepository.save(UserProjectLink.builder()
                .id(new UserProjectLinkId(user.getId(), projectId2))
                .user(user)
                .name("Orion")
                .description("Logistics platform")
                .build());
        entityManager.flush();

        var links = linkRepository.findAllByIdUserId(user.getId());

        assertThat(links).hasSize(2);
        assertThat(links).allMatch(l -> l.getId().getUserId().equals(user.getId()));
    }

    @Test
    void linkList_isEmpty_whenUserHasNoLinkedProjects() {
        User other = userRepository.save(User.builder()
                .email("bob@test.com").name("Bob").password("x").role(RoleEnum.ROLE_USER).build());
        entityManager.flush();

        var links = linkRepository.findAllByIdUserId(other.getId());

        assertThat(links).isNotNull().isEmpty();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void link_isRemovedSuccessfully_whenCompositeKeyMatches() {
        linkRepository.deleteByIdUserIdAndIdProjectId(user.getId(), projectId);
        entityManager.flush();
        entityManager.clear();

        assertThat(linkRepository.existsByIdUserIdAndIdProjectId(user.getId(), projectId)).isFalse();
    }

    // ── Unique composite key ──────────────────────────────────────────────────

    @Test
    void duplicateCompositeKey_isRejected_byDatabaseConstraint() {
        assertThatThrownBy(() -> {
            linkRepository.save(UserProjectLink.builder()
                    .id(new UserProjectLinkId(user.getId(), projectId))
                    .user(user)
                    .name("Atlas Duplicate")
                    .build());
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
