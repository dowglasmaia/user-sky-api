package com.luciodowglas.userapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.repository.UserRepository;
import com.luciodowglas.userapi.security.RoleEnum;

@ExtendWith(MockitoExtension.class)
class AdminUserSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AdminUserSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new AdminUserSeeder(userRepository, passwordEncoder);
    }

    @Test
    @SuppressWarnings("null")
    void seeder_createsAdminUser_whenNotYetPresent() throws Exception {
        // given
        when(userRepository.existsByEmail(AdminUserSeeder.ADMIN_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(AdminUserSeeder.ADMIN_PASSWORD)).thenReturn("$encoded");

        // when
        seeder.run(new DefaultApplicationArguments());

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(AdminUserSeeder.ADMIN_EMAIL);
        assertThat(saved.getName()).isEqualTo(AdminUserSeeder.ADMIN_NAME);
        assertThat(saved.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
        assertThat(saved.getPassword()).isEqualTo("$encoded");
        assertThat(saved.getPassword()).doesNotContain(AdminUserSeeder.ADMIN_PASSWORD);
    }

    @Test
    @SuppressWarnings("null")
    void seeder_skipsCreation_whenAdminAlreadyExists() throws Exception {
        // given — admin was created on a previous startup
        when(userRepository.existsByEmail(AdminUserSeeder.ADMIN_EMAIL)).thenReturn(true);

        // when
        seeder.run(new DefaultApplicationArguments());

        // then — idempotent: no write must occur
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
