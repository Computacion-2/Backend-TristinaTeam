package com.icesi.cuidapetproyect.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidationTest {
    private final JwtConfig config = new JwtConfig();
    private final String secret = Base64.getEncoder().encodeToString(new byte[32]);
    private final JwtEncoder encoder = config.jwtEncoder(config.jwtKey(secret));
    private final JwtDecoder decoder = config.jwtDecoder(config.jwtKey(secret));

    private String token(String issuer, Instant issued, Instant expires, String subject) {
        var builder = JwtClaimsSet.builder().issuer(issuer).issuedAt(issued);
        if (subject != null) builder.subject(subject);
        if (expires != null) builder.expiresAt(expires);
        var claims = builder.build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void rejectsExpiredToken() {
        String token = token(JwtConfig.ISSUER, Instant.now().minusSeconds(600),
                Instant.now().minusSeconds(300), "owner@example.com");
        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsOtherIssuer() {
        String token = token("other-api", Instant.now(), Instant.now().plusSeconds(300), "owner@example.com");
        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsWrongSignatureAndAlteredToken() {
        String token = token(JwtConfig.ISSUER, Instant.now(), Instant.now().plusSeconds(300), "owner@example.com");
        String otherSecret = Base64.getEncoder().encodeToString("another-test-secret-32-bytes-long!".getBytes());
        JwtDecoder other = config.jwtDecoder(config.jwtKey(otherSecret));
        assertThrows(JwtException.class, () -> other.decode(token));
        String[] parts = token.split("\\.");
        String altered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin@example.com\"}".getBytes()) + "." + parts[2];
        assertThrows(JwtException.class, () -> decoder.decode(altered));
    }

    @Test
    void rejectsMissingExpirationOrSubject() {
        String noExpiry = token(JwtConfig.ISSUER, Instant.now(), null, "owner@example.com");
        assertThrows(JwtException.class, () -> decoder.decode(noExpiry));
        String noSubject = token(JwtConfig.ISSUER, Instant.now(), Instant.now().plusSeconds(300), null);
        assertThrows(JwtException.class, () -> decoder.decode(noSubject));
    }

    @Test
    void rejectsWeakOrInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> config.jwtKey("not base64!"));
        assertThrows(IllegalArgumentException.class, () -> config.jwtKey("c2hvcnQ="));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(encoder, 0));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(encoder, 1441));
    }
}
