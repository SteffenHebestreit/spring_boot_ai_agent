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
 * Security configuration for the AI Research application's HTTP security and CORS policies.
 * 
 * <p>This configuration class defines comprehensive security settings for the Spring Boot
 * application, including Cross-Origin Resource Sharing (CORS) policies, CSRF protection
 * settings, and request authorization rules. It's designed to support both development
 * environments and production deployments with appropriate security measures.</p>
 * 
 * <h3>Security Strategy:</h3>
 * <ul>
 * <li><strong>Development-Friendly:</strong> Permissive settings for local development</li>
 * <li><strong>CORS Enabled:</strong> Configured cross-origin support for frontend integration</li>
 * <li><strong>CSRF Disabled:</strong> Disabled for stateless API operations</li>
 * <li><strong>Open Access:</strong> All endpoints publicly accessible (authentication via external systems)</li>
 * </ul>
 * 
 * <h3>CORS Configuration:</h3>
 * <ul>
 * <li><strong>Allowed Origins:</strong> Configured for specific frontend URLs</li>
 * <li><strong>HTTP Methods:</strong> Full HTTP method support including OPTIONS</li>
 * <li><strong>Headers:</strong> Standard headers plus Authorization and custom headers</li>
 * <li><strong>Credentials:</strong> Enabled for authentication cookie and header support</li>
 * </ul>
 * 
 * <h3>Authentication Architecture:</h3>
 * <p>This configuration assumes external authentication handling (e.g., Keycloak, API Gateway)
 * and focuses on enabling secure communication between frontend and backend services.</p>
 * 
 * <h3>Production Considerations:</h3>
 * <ul>
 * <li>Consider enabling authentication for production deployments</li>
 * <li>Restrict CORS origins to production frontend URLs</li>
 * <li>Add request rate limiting and additional security headers</li>
 * <li>Implement proper session management if needed</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * Configures the main security filter chain for HTTP requests.
     * 
     * <p>Defines the primary security configuration including CORS policy application,
     * CSRF protection settings, and request authorization rules. This configuration
     * supports a stateless API architecture with external authentication.</p>
     * 
     * <h3>Security Features:</h3>
     * <ul>
     * <li><strong>CORS Integration:</strong> Uses corsConfigurationSource for cross-origin policies</li>
     * <li><strong>CSRF Disabled:</strong> Appropriate for stateless REST APIs</li>
     * <li><strong>Permit All:</strong> All requests allowed (external auth assumed)</li>
     * </ul>
     * 
     * <h3>Request Processing:</h3>
     * <p>All HTTP requests pass through this filter chain, where CORS policies
     * are applied first, followed by authorization checks. The permissive
     * authorization allows focus on business logic rather than security enforcement.</p>
     * 
     * @param http HttpSecurity configuration object for defining security rules
     * @return Configured SecurityFilterChain for request processing
     * @throws Exception if security configuration fails
     * @see #corsConfigurationSource()
     */
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
    
    /**
     * Configures Cross-Origin Resource Sharing (CORS) policies for the application.
     * 
     * <p>Defines comprehensive CORS configuration to enable secure communication
     * between frontend applications and this backend service. The configuration
     * supports modern web application patterns including SPA (Single Page Application)
     * architectures and mobile app backends.</p>
     * 
     * <h3>CORS Settings:</h3>
     * <ul>
     * <li><strong>Allowed Origins:</strong> Specific frontend application URLs</li>
     * <li><strong>HTTP Methods:</strong> Full REST API method support</li>
     * <li><strong>Headers:</strong> Standard and custom headers for API communication</li>
     * <li><strong>Credentials:</strong> Cookie and authorization header support</li>
     * </ul>
     * 
     * <h3>Supported Origins:</h3>
     * <ul>
     * <li>http://localhost:3000 - React development server</li>
     * <li>Additional origins can be added for different environments</li>
     * </ul>
     * 
     * <h3>Security Considerations:</h3>
     * <ul>
     * <li>Origins are explicitly listed rather than using wildcards</li>
     * <li>Credentials enabled for secure authentication workflows</li>
     * <li>Standard headers allowed for common API interactions</li>
     * </ul>
     * 
     * <h3>Production Configuration:</h3>
     * <p>For production deployments, update the allowed origins list to include
     * only the actual frontend domain URLs and remove development origins.</p>
     * 
     * @return CorsConfigurationSource defining cross-origin policies for all endpoints
     * @see CorsConfiguration
     * @see UrlBasedCorsConfigurationSource
     */
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

    /**
     * Provides a RestTemplate bean for HTTP client operations.
     * 
     * <p>Creates a configured RestTemplate instance for making HTTP requests to
     * external services, APIs, and integrations. This bean can be injected into
     * services that need to communicate with external systems.</p>
     * 
     * <h3>Usage:</h3>
     * <ul>
     * <li>External API integrations</li>
     * <li>MCP server communications</li>
     * <li>A2A peer agent interactions</li>
     * <li>Third-party service calls</li>
     * </ul>
     * 
     * <h3>Configuration:</h3>
     * <p>The RestTemplate is created with default settings. For production use,
     * consider adding connection pooling, timeouts, and error handling configuration.</p>
     * 
     * @return RestTemplate instance for HTTP client operations
     * @see RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}