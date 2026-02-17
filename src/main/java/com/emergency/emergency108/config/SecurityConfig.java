package com.emergency.emergency108.config;

import com.emergency.emergency108.auth.security.filter.AuthContextFilter;
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
                        AuthContextFilter authContextFilter) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                // Spring Security permissive - auth is handled by AuthGuard in controllers
                                // This allows our custom auth exceptions to be thrown and handled properly
                                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                                .addFilterBefore(
                                                authContextFilter,
                                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                                // Disable Spring Security's exception handling to let our exceptions propagate
                                .exceptionHandling(ex -> ex.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

                return http.build();
        }

        @Bean
        public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
                org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
                configuration.setAllowedOriginPatterns(java.util.List.of("*")); // Allow all origins (dev/prod)
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);

                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

}
