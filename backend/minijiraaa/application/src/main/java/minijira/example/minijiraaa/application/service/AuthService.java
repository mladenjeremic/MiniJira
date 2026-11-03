package minijira.example.minijiraaa.application.service;

import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.dto.AuthResponse;
import minijira.example.minijiraaa.application.dto.LoginRequest;
import minijira.example.minijiraaa.application.dto.RegisterRequest;
import minijira.example.minijiraaa.application.dto.UserResponse;
import minijira.example.minijiraaa.domain.exception.ConflictException;
import minijira.example.minijiraaa.domain.exception.InvalidCredentialsException;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.application.security.JwtService;
import minijira.example.minijiraaa.domain.store.UserStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userStore.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    "User with this email already exists."
            );
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        User savedUser = userStore.save(user);

        return new AuthResponse(
                jwtService.generateToken(savedUser),
                UserResponse.from(savedUser)
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userStore
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(
                jwtService.generateToken(user),
                UserResponse.from(user)
        );
    }
}