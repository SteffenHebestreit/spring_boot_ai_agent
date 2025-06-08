package com.airesearch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Component
public class RoleFileLoader implements EnvironmentPostProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleFileLoader.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            // Only override if not set by environment variable
            if (environment.getProperty("OPENAI_API_SYSTEM_ROLE") != null) {
                return;
            }
            
            // Get the role file path from properties or use default
            String roleFilePath = environment.getProperty("openai.api.roleFile", "classpath:role.yml");
            
            String content;
            if (roleFilePath.startsWith("classpath:")) {
                String resourcePath = roleFilePath.substring("classpath:".length());
                Resource resource = new ClassPathResource(resourcePath);
                content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                content = new String(Files.readAllBytes(Paths.get(roleFilePath)), StandardCharsets.UTF_8);
            }
            
            // Set the systemRole property
            Properties props = new Properties();
            props.put("openai.api.systemRole", content);
            environment.getPropertySources().addFirst(new PropertiesPropertySource("roleFileContent", props));
            
            logger.info("Successfully loaded system role from {}", roleFilePath);
        } catch (IOException e) {
            logger.error("Failed to load role file: {}", e.getMessage());
            // The default value in application.properties will be used
        }
    }
}
