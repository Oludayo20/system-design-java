package com.systemdesign.ecommarketplace.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Mirrors src/common/guards/jwt-auth.guard.ts applied selectively via Nest's
 * per-controller @UseGuards(JwtAuthGuard): Auth, Marketplace, Health and the
 * Swagger docs are public; Users, Orders and Wallet require a valid bearer
 * JWT. No CSRF, no server-side sessions (STATELESS) - auth is a bearer
 * token, not a cookie, so either api-1 or api-2 can answer any request.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final int BCRYPT_ROUNDS = 10;

  private final JwtAuthFilter jwtAuthFilter;
  private final JwtAuthEntryPoint jwtAuthEntryPoint;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter, JwtAuthEntryPoint jwtAuthEntryPoint) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.jwtAuthEntryPoint = jwtAuthEntryPoint;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/auth/**",
                        "/health",
                        "/marketplace/**",
                        "/docs/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthEntryPoint))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /** BCRYPT_ROUNDS = 10, matching AuthService's bcrypt.hash(dto.password, BCRYPT_ROUNDS). */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(BCRYPT_ROUNDS);
  }
}
