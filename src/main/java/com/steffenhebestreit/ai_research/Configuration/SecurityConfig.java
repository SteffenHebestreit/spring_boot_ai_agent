package com.steffenhebestreit.ai_research.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the AI Research application.
 * 
 * This class configures security settings including CORS policies,
 * CSRF protection (disabled for this application), and request authorization.
 * In this configuration, all requests are permitted without authentication,
 * as the actual authentication is handled by Keycloak in production.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults()) // Enable CORS using the corsConfigurationSource bean
                .csrf(csrf -> csrf.disable()) // Disable CSRF protection
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll() // Allow all requests without authentication
                );
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000"
        )); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true); // If you need to send cookies or use authorization headers
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply this configuration to all paths
        return source;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}