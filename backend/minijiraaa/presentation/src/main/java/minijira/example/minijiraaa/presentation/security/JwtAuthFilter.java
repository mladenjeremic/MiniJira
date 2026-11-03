package minijira.example.minijiraaa.presentation.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minijira.example.minijiraaa.application.security.JwtService;
import minijira.example.minijiraaa.domain.store.UserStore;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserStore userStore;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null
                && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                // Izvlačimo ID korisnika iz JWT tokena.
                String token = header.substring(7);
                Long userId = jwtService.extractUserId(token);

                // Proveravamo da li korisnik postoji u bazi.
                var userOptional = userStore.findById(userId);

                if (userOptional.isPresent()) {
                    var user = userOptional.get();

                    var auth = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of()
                    );

                    auth.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(auth);

                    log.debug("Korisnik je autentifikovan: ID = {}", userId);

                } else {
                    log.warn(
                            "JWT je validan, ali korisnik ne postoji u bazi. ID = {}",
                            userId
                    );
                }

            } catch (JwtException | IllegalArgumentException e) {
                log.warn(
                        "JWT validation failed: {} - {}",
                        e.getClass().getSimpleName(),
                        e.getMessage()
                );
            }
        }

        chain.doFilter(request, response);
    }
}