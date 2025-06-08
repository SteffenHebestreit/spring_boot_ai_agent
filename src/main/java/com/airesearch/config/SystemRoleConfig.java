package com.airesearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SystemRoleConfig {
    private static final Logger logger = LoggerFactory.getLogger(SystemRoleConfig.class);

    @Value("${openai.api.roleFile}")
    private Resource roleFile;

    /**
     * Reads the system role from the file and sets it as a property
     */
    @Bean
    public CommandLineRunner setSystemRole(Environment environment) {
        return args -> {
            try {
                String envSystemRole = environment.getProperty("OPENAI_API_SYSTEM_ROLE");
                String currentSystemRole = environment.getProperty("openai.api.systemRole");

                System.out.println("DEBUG SystemRoleConfig: OPENAI_API_SYSTEM_ROLE env var = '" + envSystemRole + "'");
                System.out.println("DEBUG SystemRoleConfig: openai.api.systemRole property = '" + currentSystemRole + "'");

                // Read the content of the role.yml file
                String roleContent = StreamUtils.copyToString(
                        roleFile.getInputStream(),
                        StandardCharsets.UTF_8
                );
                System.out.println("DEBUG SystemRoleConfig: role.yml content length = " + roleContent.length());

                // Load from file if env var is null/empty OR if current property is empty
                if ((envSystemRole == null || envSystemRole.trim().isEmpty()) &&
                        (currentSystemRole == null || currentSystemRole.trim().isEmpty()) &&
                        environment instanceof ConfigurableEnvironment) {

                    ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
                    Properties props = new Properties();
                    props.put("openai.api.systemRole", roleContent);

                    PropertiesPropertySource propertySource =
                            new PropertiesPropertySource("roleYmlProperties", props);
                    configurableEnv.getPropertySources().addFirst(propertySource);

                    System.out.println("DEBUG SystemRoleConfig: System role loaded from " + roleFile.getFilename() + " with content length: " + roleContent.length());
                } else {
                    System.out.println("DEBUG SystemRoleConfig: NOT loading from file. envSystemRole='" + envSystemRole + "', currentSystemRole='" + currentSystemRole + "'");
                }
            } catch (IOException e) {
                System.err.println("Failed to read system role file: " + e.getMessage());
                throw e;
            }
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        logger.debug("=== SystemRoleConfig Application Ready ===");

        Environment environment = event.getApplicationContext().getEnvironment();

        // Check both environment variable and property value
        String envVar = environment.getProperty("OPENAI_API_SYSTEM_ROLE");
        String propertyValue = environment.getProperty("openai.api.systemRole");

        logger.debug("OPENAI_API_SYSTEM_ROLE env var: '{}'", envVar);
        logger.debug("openai.api.systemRole property: '{}'", propertyValue);
    }
}
