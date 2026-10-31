package com.itsm.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Authentication and role-based authorisation.
 * <ul>
 *   <li>Form login for the browser UI; HTTP Basic for REST clients (e.g. curl, Postman).</li>
 *   <li>Passwords hashed with BCrypt; plain passwords are never stored.</li>
 *   <li>URL rules give a first line of defence; services re-check ownership
 *       (e.g. "only the assigned technician may resolve"), because hiding a button
 *       does not stop someone typing a URL.</li>
 *   <li>CSRF protection is on for all browser forms.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/img/**", "/favicon.ico",
                                "/error", "/actuator/health").permitAll()
                        .requestMatchers("/admin/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/h2-console/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/tech/**").hasAnyRole("TECHNICIAN", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                        // Stateless API clients authenticate with HTTP Basic on every call, so CSRF does not apply.
                        .ignoringRequestMatchers(request -> request.getRequestURI().startsWith("/api/")
                                && String.valueOf(request.getHeader("Authorization")).startsWith("Basic ")))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
