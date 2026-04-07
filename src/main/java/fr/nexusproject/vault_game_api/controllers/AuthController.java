package fr.nexusproject.vault_game_api.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/verify")
    public JwtVerificationResponse verify(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new JwtVerificationResponse(
                true,
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getIssuer() != null ? jwt.getIssuer().toString() : null,
                jwt.getExpiresAt(),
                authorities,
                jwt.getClaims());
    }

    public record JwtVerificationResponse(
            boolean valid,
            String subject,
            String preferredUsername,
            String issuer,
            java.time.Instant expiresAt,
            List<String> authorities,
            Map<String, Object> claims) {
    }
}