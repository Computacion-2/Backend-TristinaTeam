package com.icesi.cuidapetproyect.api.security;

import com.icesi.cuidapetproyect.api.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiration-minutes:30}") long expirationMinutes) {
        if (expirationMinutes < 1 || expirationMinutes > 1440) {
            throw new IllegalArgumentException("La duración JWT debe estar entre 1 y 1440 minutos");
        }
        this.encoder = encoder;
        this.expirationMinutes = expirationMinutes;
    }

    public TokenResponse createToken(UserDetails user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);
        List<String> roles = roles(user);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER).subject(user.getUsername())
                .issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("email", user.getUsername()).claim("roles", roles)
                .claim("permissions", permissions(user)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiresAt, user.getUsername(), roles);
    }

    public static CurrentUserResponse userInfo(UserDetails user, Long userId) {
        return new CurrentUserResponse(userId, user.getUsername(), roles(user), permissions(user));
    }

    private static List<String> roles(UserDetails user) {
        return authorityNames(user).filter(value -> value.startsWith("ROLE_"))
                .map(value -> value.substring(5)).toList();
    }

    private static List<String> permissions(UserDetails user) {
        return authorityNames(user).filter(value -> !value.startsWith("ROLE_")).toList();
    }

    private static Stream<String> authorityNames(UserDetails user) {
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted();
    }
}
