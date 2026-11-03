package minijira.example.minijiraaa.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import minijira.example.minijiraaa.application.dto.AuthResponse;
import minijira.example.minijiraaa.application.dto.LoginRequest;
import minijira.example.minijiraaa.application.dto.RegisterRequest;
import minijira.example.minijiraaa.application.dto.UserResponse;
import minijira.example.minijiraaa.domain.model.User;
import minijira.example.minijiraaa.application.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }
}