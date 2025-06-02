package com.steffenhebestreit.ai_research.Configuration;

import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Large Language Model (LLM) configurations in the AI Research system.
 * 
 * <p>This configuration class manages multiple LLM configurations, enabling the system to work
 * with different language models with varying capabilities, endpoints, and authentication methods.
 * It supports dynamic LLM selection based on task requirements, content types, and capability
 * matching for optimal performance and functionality.</p>
 * 
 * <h3>Multi-LLM Architecture:</h3>
 * <ul>
 * <li><strong>Configuration Arrays:</strong> Support for multiple LLM configurations</li>
 * <li><strong>Capability Mapping:</strong> Different models for text, vision, code, etc.</li>
 * <li><strong>Dynamic Selection:</strong> Runtime model selection based on task requirements</li>
 * <li><strong>Fallback Support:</strong> Primary and secondary model configurations</li>
 * </ul>
 * 
 * <h3>Configuration Structure:</h3>
 * <ul>
 * <li><strong>Property Prefix:</strong> {@code llm}</li>
 * <li><strong>Array Support:</strong> configurations[].* for multiple LLM setups</li>
 * <li><strong>ID-based Lookup:</strong> Unique identifier for each LLM configuration</li>
 * <li><strong>Capability Declaration:</strong> Supported features and content types per LLM</li>
 * </ul>
 * 
 * <h3>LLM Types Supported:</h3>
 * <ul>
 * <li><strong>Text Models:</strong> General purpose text generation and conversation</li>
 * <li><strong>Vision Models:</strong> Multimodal models supporting image analysis</li>
 * <li><strong>Code Models:</strong> Specialized models for programming and technical content</li>
 * <li><strong>Specialized Models:</strong> Domain-specific models for research, analysis, etc.</li>
 * </ul>
 * 
 * <h3>Configuration Example:</h3>
 * <pre>
 * llm:
 *   configurations:
 *     - id: "gpt-4"
 *       name: "GPT-4"
 *       endpoint: "https://api.openai.com/v1"
 *       capabilities: ["text", "conversation"]
 *     - id: "gpt-4-vision"
 *       name: "GPT-4 Vision"
 *       endpoint: "https://api.openai.com/v1"
 *       capabilities: ["text", "vision", "multimodal"]
 * </pre>
 * 
 * <h3>Lookup and Selection:</h3>
 * <p>Provides utility methods for finding LLM configurations by ID, enabling services
 * to dynamically select appropriate models based on task requirements and content types.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see LlmConfiguration
 * @see OpenAIService
 * @see LlmCapabilityService
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfigProperties {
    
    /**
     * List of LLM configurations for different models and capabilities.
     * 
     * <p>Contains configuration objects for all available LLM models that the system
     * can utilize. Each configuration includes connection details, authentication
     * information, and capability declarations for intelligent model selection.</p>
     * 
     * @see LlmConfiguration
     */
    private List<LlmConfiguration> configurations = new ArrayList<>();
    
    /**
     * Returns the list of all configured LLM models.
     * 
     * @return List of LLM configurations
     */
    public List<LlmConfiguration> getConfigurations() {
        return configurations;
    }
    
    /**
     * Sets the list of LLM configurations.
     * 
     * @param configurations List of LLM configurations to set
     */
    public void setConfigurations(List<LlmConfiguration> configurations) {
        this.configurations = configurations;
    }
    
    /**
     * Finds an LLM configuration by its unique identifier.
     * 
     * <p>Performs case-sensitive lookup of LLM configurations using the provided
     * ID. This method enables services to dynamically select appropriate models
     * based on task requirements, content types, or user preferences.</p>
     * 
     * <h3>Lookup Strategy:</h3>
     * <ul>
     * <li>Performs stream-based filtering for efficient lookup</li>
     * <li>Returns first matching configuration (IDs should be unique)</li>
     * <li>Handles null ID gracefully by returning null</li>
     * <li>Case-sensitive matching for precise configuration selection</li>
     * </ul>
     * 
     * <h3>Usage Examples:</h3>
     * <ul>
     * <li>Dynamic model selection for multimodal content</li>
     * <li>Capability-based routing for specialized tasks</li>
     * <li>User preference-based model selection</li>
     * <li>Fallback model configuration retrieval</li>
     * </ul>
     * 
     * @param id The unique LLM ID to search for
     * @return The matching LlmConfiguration or null if not found
     * @see LlmConfiguration#getId()
     */
    public LlmConfiguration findById(String id) {
        if (id == null) {
            return null;
        }
        
        return configurations.stream()
                .filter(config -> id.equals(config.getId()))
                .findFirst()
                .orElse(null);
    }
}
