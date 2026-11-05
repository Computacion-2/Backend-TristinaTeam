package com.icesi.cuidapetproyect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/error",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // MVC y rutas heredadas; /api/v1/** tiene su propia cadena JWT.
                .requestMatchers("/users", "/users/**", "/usuarios", "/usuarios/**")
                    .hasAuthority("MANAGE_USERS")
                .requestMatchers("/roles", "/roles/**", "/permisos", "/permisos/**",
                        "/api/roles", "/api/roles/**", "/api/permissions", "/api/permissions/**")
                    .hasAuthority("MANAGE_ROLES")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login")
                    .usernameParameter("email")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error")
                    .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        // CSRF permanece habilitado. Thymeleaf añade el token en los formularios POST.
        return http.build();
    }
}
