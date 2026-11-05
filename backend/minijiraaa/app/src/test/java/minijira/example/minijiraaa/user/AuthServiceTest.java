package minijira.example.minijiraaa.user;

import minijira.example.minijiraaa.application.dto.AuthResponse;
import minijira.example.minijiraaa.application.dto.LoginRequest;
import minijira.example.minijiraaa.application.dto.RegisterRequest;
import minijira.example.minijiraaa.domain.exception.ConflictException;
import minijira.example.minijiraaa.domain.exception.InvalidCredentialsException;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.application.security.JwtService;
import minijira.example.minijiraaa.application.service.AuthService;
import minijira.example.minijiraaa.domain.store.UserStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import minijira.example.minijiraaa.application.mapper.UserMapper;
import minijira.example.minijiraaa.application.mapper.UserMapperImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserStore userStore;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @Spy UserMapper userMapper = new UserMapperImpl();

    @InjectMocks AuthService authService;

    @Test
    void register_savesNormalizedUserWithHashedPassword() {
        RegisterRequest request = new RegisterRequest("  Ana  ", " ANA@Mail.com ", "secret1");

        when(userStore.existsByEmailIgnoreCase("ana@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("HASH");
        when(userStore.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userStore).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Ana");
        assertThat(saved.getEmail()).isEqualTo("ana@mail.com");
        assertThat(saved.getPasswordHash()).isEqualTo("HASH");   // nikad cista lozinka

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("ana@mail.com");
    }

    @Test
    void register_throwsConflictWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("Ana", "ana@mail.com", "secret1");
        when(userStore.existsByEmailIgnoreCase("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(userStore, never()).save(any());
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana");
        user.setEmail("ana@mail.com");
        user.setPasswordHash("HASH");

        when(userStore.findByEmailIgnoreCase("ana@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret1", "HASH")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest(" ana@mail.com ", "secret1"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(1L);
    }

    @Test
    void login_failsForUnknownEmail() {
        when(userStore.findByEmailIgnoreCase("nobody@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@mail.com", "x")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_failsForWrongPassword_withSameExceptionAsUnknownEmail() {
        User user = new User();
        user.setPasswordHash("HASH");

        when(userStore.findByEmailIgnoreCase("ana@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@mail.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}