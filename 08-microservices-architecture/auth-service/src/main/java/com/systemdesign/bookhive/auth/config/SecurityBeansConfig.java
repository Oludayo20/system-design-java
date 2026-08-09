package com.systemdesign.bookhive.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Just the BCrypt hasher - this service deliberately does not depend on
 * spring-boot-starter-security (no filter chain, no SecurityContext, no login page). Password
 * hashing is the only crypto primitive this service needs.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
