package com.hackathon.emergency108.config;

import com.hackathon.emergency108.auth.security.filter.AuthContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    // JwtAuthFilter removed - AuthContextFilter handles full authentication

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthContextFilter authContextFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                // Spring Security permissive - auth is handled by AuthGuard in controllers
                // This allows our custom auth exceptions to be thrown and handled properly
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(
                        authContextFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                )
                // Disable Spring Security's exception handling to let our exceptions propagate
                .exceptionHandling(ex -> ex.disable());

        return http.build();
    }

}

