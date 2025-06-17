package com.steffenhebestreit.ai_research.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuration class responsible for loading system role content from external files.
 * 
 * <p>This configuration automatically loads system role content from a specified file
 * (typically role.yml) and integrates it with the OpenAI properties. The loading occurs
 * during application startup and respects environment variable overrides.</p>
 * 
 * <h3>Loading Priority:</h3>
 * <ol>
 * <li>Environment variable OPENAI_API_SYSTEM_ROLE (highest priority)</li>
 * <li>Content from role file (if environment variable not set)</li>
 * <li>Default value from OpenAIProperties (fallback)</li>
 * </ol>
 * 
 * <h3>Configuration Properties:</h3>
 * <ul>
 * <li><strong>openai.api.roleFile</strong> - Path to the role file (supports classpath: prefix)</li>
 * <li><strong>OPENAI_API_SYSTEM_ROLE</strong> - Environment variable override</li>
 * </ul>
 * 
 * @author AI Research System
 * @version 1.0
 * @since 1.0
 * @see OpenAIProperties
 */
@Configuration
@Order(1) // Ensure this runs before other components
public class SystemRoleLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemRoleLoader.class);
    
    @Value("${openai.api.roleFile:classpath:role.yml}")
    private Resource roleFile;
    
    private final OpenAIProperties openAIProperties;
    
    public SystemRoleLoader(OpenAIProperties openAIProperties) {
        this.openAIProperties = openAIProperties;
    }
    
    /**
     * Loads system role content from file during bean initialization.
     * 
     * <p>This method is triggered during Spring bean initialization, before
     * other services are created. It checks if a system role has already been 
     * set via environment variables, and only loads from file if no explicit 
     * override is present.</p>
     */
    @PostConstruct
    public void loadSystemRoleFromFile() {
        logger.debug("=== SystemRoleLoader: Starting system role loading process ===");
        try {
            // Check if system role is already set via environment variable
            String envSystemRole = System.getenv("OPENAI_API_SYSTEM_ROLE");
            logger.debug("Environment variable OPENAI_API_SYSTEM_ROLE: '{}'", envSystemRole);
            if (envSystemRole != null && !envSystemRole.trim().isEmpty()) {
                logger.info("Using system role from environment variable OPENAI_API_SYSTEM_ROLE");
                openAIProperties.setSystemRole(envSystemRole);
                return;
            }            
            // Check if role file exists and is readable
            logger.debug("Checking role file: {}", roleFile.getDescription());
            if (!roleFile.exists()) {
                logger.warn("Role file does not exist: {}. Using default system role.", roleFile.getDescription());
                return;
            }            
            // Load content from role file
            logger.debug("Loading content from role file...");
            String roleContent = StreamUtils.copyToString(
                roleFile.getInputStream(),
                StandardCharsets.UTF_8
            );
            
            logger.debug("Loaded role content length: {}", roleContent != null ? roleContent.length() : 0);
            
            if (roleContent != null && !roleContent.trim().isEmpty()) {
                openAIProperties.setSystemRole(roleContent.trim());
                logger.info("Successfully loaded system role from file: {} (length: {} characters)", 
                    roleFile.getFilename(), roleContent.trim().length());
                logger.debug("System role content preview: {}", 
                    roleContent.substring(0, Math.min(100, roleContent.length())) + "...");
            } else {
                logger.warn("Role file is empty: {}. Using default system role.", roleFile.getDescription());
            }
            
        } catch (IOException e) {
            logger.error("Failed to load system role from file: {}. Using default system role. Error: {}", 
                roleFile.getDescription(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error loading system role from file: {}. Using default system role.", 
                roleFile.getDescription(), e);
        }
    }
}
