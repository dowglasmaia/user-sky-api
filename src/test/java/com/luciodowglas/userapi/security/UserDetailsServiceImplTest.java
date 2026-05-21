package com.luciodowglas.userapi.security;

import static com.luciodowglas.userapi.fixture.UserFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.luciodowglas.userapi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserDetailsServiceImpl service;

    @Test
    void knownUser_loadsCorrectUserDetails_withEmailAndRole() {
        // given
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(aUser()));

        // when
        var details = service.loadUserByUsername(USER_EMAIL);

        // then
        assertThat(details.getUsername()).isEqualTo(USER_EMAIL);
        assertThat(details.getPassword()).isEqualTo("$2a$12$encodedpasswordhash");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    void unknownUser_throwsUsernameNotFoundException_withEmailInMessage() {
        // given
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.loadUserByUsername("ghost@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@test.com");
    }
}
