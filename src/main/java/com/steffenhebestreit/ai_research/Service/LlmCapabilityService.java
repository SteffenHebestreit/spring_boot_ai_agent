package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Configuration.LlmConfigProperties;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing LLM configurations and determining LLM capabilities.
 * <p>
 * This service provides functionality to:
 * <ul>
 *   <li>Retrieve all configured LLMs and their capabilities</li>
 *   <li>Check if specific LLMs support various data types (text, images, PDFs)</li>
 *   <li>Get the default LLM configuration</li>
 *   <li>Validate LLM compatibility for multimodal content processing</li>
 * </ul>
 * <p>
 * The service reads LLM configurations from {@link LlmConfigProperties} and provides
 * methods to programmatically determine which LLMs can handle different types of content.
 * This enables the application to show appropriate warnings or enable/disable features
 * based on the selected LLM's capabilities.
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 */
@Service
public class LlmCapabilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmCapabilityService.class);
    
    @Autowired
    private LlmConfigProperties llmConfigProperties;
    
    @Value("${openai.api.model:}")
    private String defaultModelId;
      /**
     * Retrieves all configured LLMs with their capabilities.
     * <p>
     * This method returns a list of all LLM configurations that have been
     * defined in the application properties. Each configuration includes
     * the model's ID, name, and capability flags for different data types.
     * 
     * @return List of LLM configurations, may be empty if no LLMs are configured
     */
    public List<LlmConfiguration> getAllLlmConfigurations() {
        return llmConfigProperties.getConfigurations();
    }
      /**
     * Retrieves the configuration for a specific LLM by its ID.
     * <p>
     * This method looks up an LLM configuration using its unique identifier.
     * The ID typically corresponds to the model name or identifier used
     * in API calls (e.g., "gpt-4o", "gemma-7b").
     * 
     * @param llmId The ID of the LLM to retrieve
     * @return The LLM configuration if found, null if the ID doesn't match any configured LLM
     */
    public LlmConfiguration getLlmConfiguration(String llmId) {
        return llmConfigProperties.findById(llmId);
    }
      /**
     * Checks if the specified LLM supports a given data type.
     * <p>
     * This method is crucial for multimodal content processing as it allows
     * the application to determine whether a selected LLM can handle specific
     * types of input data before attempting to process them.
     * <p>
     * Supported data types:
     * <ul>
     *   <li><b>text</b> - Plain text input (supported by all LLMs)</li>
     *   <li><b>image</b> - Image files (JPG, PNG, etc.) for vision-enabled models</li>
     *   <li><b>pdf</b> - PDF documents for document-analysis capable models</li>
     * </ul>
     * 
     * @param llmId The ID of the LLM to check capabilities for
     * @param dataType The data type to check support for (case-insensitive)
     * @return true if the LLM supports the specified data type, false otherwise
     * @throws IllegalArgumentException if llmId is null or empty
     */
    public boolean supportsDataType(String llmId, String dataType) {
        LlmConfiguration config = getLlmConfiguration(llmId);
        if (config == null) {
            logger.warn("LLM with ID '{}' not found in configurations. Assuming it doesn't support '{}'", llmId, dataType);
            return false;
        }
        
        switch (dataType.toLowerCase()) {
            case "text":
                return config.isSupportsText();
            case "image":
                return config.isSupportsImage();
            case "pdf":
                return config.isSupportsPdf();
            default:
                logger.warn("Unknown data type: '{}'. Returning false for LLM '{}'", dataType, llmId);
                return false;
        }
    }
      /**
     * Gets the default LLM model configuration as specified by openai.api.model property.
     * <p>
     * This method retrieves the configuration for the default LLM model, which is
     * typically used when no specific model is requested. The default model is
     * determined by the {@code openai.api.model} property in the application configuration.
     * <p>
     * This is useful for fallback scenarios or when the application needs to use
     * a default model for processing requests.
     * 
     * @return The default LLM configuration if configured and found, null otherwise
     */
    public LlmConfiguration getDefaultLlmConfiguration() {
        if (defaultModelId == null || defaultModelId.isEmpty()) {
            logger.warn("No default model ID configured (openai.api.model)");
            return null;
        }
        
        LlmConfiguration config = getLlmConfiguration(defaultModelId);
        if (config == null) {
            logger.warn("Default model ID '{}' not found in LLM configurations", defaultModelId);
            return null;
        }
        
        return config;
    }
    
    /**
     * Gets the current default model ID.
     * 
     * @return The default model ID from configuration
     */
    public String getDefaultModelId() {
        return defaultModelId;
    }
    
    /**
     * Checks if a model exists in the configuration.
     * 
     * @param modelId The model ID to check
     * @return true if the model exists, false otherwise
     */
    public boolean modelExists(String modelId) {
        return getLlmConfiguration(modelId) != null;
    }
}
