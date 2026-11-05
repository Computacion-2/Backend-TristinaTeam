package com.icesi.cuidapetproyect.api.security;

import com.icesi.cuidapetproyect.service.AppUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
public class ApiSecurityConfig {
    @Bean
    AuthenticationManager apiAuthenticationManager(AppUserDetailsService users, PasswordEncoder passwords) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        return new ProviderManager(provider);
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, AppUserDetailsService users)
            throws Exception {
        http.securityMatcher("/api/v1/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/users", "/api/v1/users/**",
                                "/api/v1/user-roles", "/api/v1/user-roles/**").hasAuthority("MANAGE_USERS")
                        .requestMatchers("/api/v1/roles", "/api/v1/roles/**",
                                "/api/v1/permissions", "/api/v1/permissions/**",
                                "/api/v1/role-permissions", "/api/v1/role-permissions/**").hasAuthority("MANAGE_ROLES")
                        .anyRequest().authenticated())
                // Spring instala BearerTokenAuthenticationFilter: valida firma, emisor y fechas.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                            try {
                                UserDetails user = users.loadUserByUsername(token.getSubject());
                                new AccountStatusUserDetailsChecker().check(user);
                                // Consultar permisos actuales evita conservar privilegios retirados.
                                return UsernamePasswordAuthenticationToken.authenticated(
                                        user, null, user.getAuthorities());
                            } catch (AuthenticationException ex) {
                                throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_token"), "Usuario no disponible");
                            }
                        }))
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Se requiere un token válido\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"No tiene permisos para esta operación\"}");
                        }));
        return http.build();
    }
}
