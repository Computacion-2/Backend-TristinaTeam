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
        CurrentUserResponse info = userInfo(user);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER).subject(user.getUsername())
                .issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("email", info.email()).claim("roles", info.roles())
                .claim("permissions", info.permissions()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiresAt, user.getUsername(), info.roles());
    }

    public static CurrentUserResponse userInfo(UserDetails user) {
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return new CurrentUserResponse(user.getUsername(),
                authorities.stream().filter(value -> value.startsWith("ROLE_"))
                        .map(value -> value.substring(5)).toList(),
                authorities.stream().filter(value -> !value.startsWith("ROLE_")).toList());
    }
}
