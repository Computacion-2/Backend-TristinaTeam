package com.icesi.cuidapetproyect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/error").permitAll()
                // Las vistas MVC y la API REST comparten las mismas reglas de autorización.
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
