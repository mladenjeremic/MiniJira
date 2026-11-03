package minijira.example.minijiraaa.security;

import io.jsonwebtoken.JwtException;
import minijira.example.minijiraaa.application.security.JwtService;
import minijira.example.minijiraaa.domain.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JwtService nema zavisnosti, pa ga pravimo direktno (bez Spring-a i bez mock-ova). */
class JwtServiceTest {

    private static final String SECRET = "MiniJiraTestSecretKey2026MustBeLongEnough!!";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setEmail("ana@mail.com");
        return user;
    }

    @Test
    void generatedToken_containsUserId() {
        String token = jwtService.generateToken(user(5L));

        assertThat(jwtService.extractUserId(token)).isEqualTo(5L);
    }

    @Test
    void expiredToken_isRejected() {
        JwtService shortLived = new JwtService(SECRET, -1_000);
        String token = shortLived.generateToken(user(5L));

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        JwtService other = new JwtService("AnotherSecretKeyAnotherSecretKey123456", 60_000);
        String forged = other.generateToken(user(5L));

        assertThatThrownBy(() -> jwtService.extractUserId(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void garbageToken_isRejected() {
        assertThatThrownBy(() -> jwtService.extractUserId("not-a-token"))
                .isInstanceOf(JwtException.class);
    }
}