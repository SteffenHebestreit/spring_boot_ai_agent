package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Configuration.LlmConfigProperties;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Model.ProviderModel;
import com.steffenhebestreit.ai_research.Model.ProviderModelAdapter;
import com.steffenhebestreit.ai_research.Model.LlmConfigurationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Service for managing LLM configurations and determining LLM capabilities.
 * <p>
 * This service provides functionality to:
 * <ul>
 *   <li>Retrieve all configured LLMs and their capabilities</li>
 *   <li>Check if specific LLMs support various data types (text, images, PDFs)</li>
 *   <li>Get the default LLM configuration</li>
 *   <li>Validate LLM compatibility for multimodal content processing</li>
 *   <li>Merge local configurations with provider models</li>
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
    
    @Autowired
    @Lazy
    private OpenAIService openAIService;
    
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
    
    /**
     * Merges local LLM configurations with provider models.
     * <p>
     * This method combines the LLM configurations defined in the application
     * properties with those available from the model provider (e.g., OpenAI).
     * It ensures that the application has an up-to-date view of all available
     * models and their capabilities.
     * <p>     * The merge strategy is as follows:
     * <ul>
     *   <li>Provider models overwrite local configurations with the same ID</li>
     *   <li>All local configurations are included, even if not present in provider models</li>
     * </ul>
     * 
     * @return List of merged LLM configurations
     */
    public List<LlmConfiguration> getMergedLlmConfigurations() {
        List<LlmConfiguration> localConfigurations = getAllLlmConfigurations();
        List<ProviderModel> providerModels;
        
        try {
            providerModels = openAIService.getAvailableModels();
        } catch (Exception e) {
            logger.warn("Failed to fetch models from provider, using local configurations only: {}", e.getMessage());
            return localConfigurations;
        }
        
        logger.info("Merging LLM configurations: {} local, {} from provider", 
                   localConfigurations.size(), providerModels.size());

        // Convert provider models to LlmConfiguration format
        List<LlmConfiguration> providerConfigurations = convertProviderModelsToConfigurations(providerModels);

        // Combine configurations, prioritizing provider models to override/expand local ones
        Map<String, LlmConfiguration> combinedConfigMap = new HashMap<>();

        // Add all local configurations first as the base
        for (LlmConfiguration config : localConfigurations) {
            if (config != null && config.getId() != null) {
                combinedConfigMap.put(config.getId(), config);
                logger.debug("Added local configuration for model: {}", config.getId());
            }
        }

        // Add/override with provider configurations - provider models should expand or override local list
        for (LlmConfiguration providerConfig : providerConfigurations) {
            if (providerConfig != null && providerConfig.getId() != null) {
                String modelId = providerConfig.getId();
                LlmConfiguration existingLocal = combinedConfigMap.get(modelId);
                
                if (existingLocal != null) {
                    // Model exists locally - merge capabilities, prioritizing local enhancements but keeping provider data
                    LlmConfiguration mergedConfig = mergeLocalAndProviderConfigurations(existingLocal, providerConfig);
                    combinedConfigMap.put(modelId, mergedConfig);
                    logger.debug("Merged configuration for model: {} (local + provider)", modelId);
                } else {
                    // New model from provider - add it directly
                    combinedConfigMap.put(modelId, providerConfig);
                    logger.debug("Added new provider model: {}", modelId);
                }
            }
        }

        List<LlmConfiguration> combinedConfigurations = new ArrayList<>(combinedConfigMap.values());
        logger.info("Final merged configuration contains {} models", combinedConfigurations.size());
        
        return combinedConfigurations;
    }

    /**
     * Converts provider models to LlmConfiguration objects.
     */
    private List<LlmConfiguration> convertProviderModelsToConfigurations(List<ProviderModel> providerModels) {
        List<LlmConfiguration> configurations = new ArrayList<>();
        
        for (ProviderModel model : providerModels) {
            LlmConfiguration config = new LlmConfiguration();
            config.setId(model.getId());
            config.setName(ProviderModelAdapter.getDisplayName(model));
            config.setDescription(model.getDescription());
            config.setSupportsText(ProviderModelAdapter.isSupportsText(model));
            config.setSupportsImage(ProviderModelAdapter.isSupportsImage(model));
            config.setSupportsPdf(ProviderModelAdapter.isSupportsPdf(model));
            
            // Set additional fields from ProviderModel if available
            LlmConfigurationAdapter.setMaxTokens(config, ProviderModelAdapter.getTokenLimit(model));
            LlmConfigurationAdapter.setSupportsJson(config, ProviderModelAdapter.isSupportsJson(model));
            LlmConfigurationAdapter.setSupportsTools(config, ProviderModelAdapter.isSupportsTools(model));
            
            configurations.add(config);
        }
        
        return configurations;
    }

    /**
     * Merges local and provider configurations, prioritizing local enhancements.
     */
    private LlmConfiguration mergeLocalAndProviderConfigurations(LlmConfiguration local, LlmConfiguration provider) {
        LlmConfiguration merged = new LlmConfiguration();
        
        // Use local ID and name if available, otherwise provider
        merged.setId(local.getId() != null ? local.getId() : provider.getId());
        merged.setName(local.getName() != null && !local.getName().isEmpty() ? local.getName() : provider.getName());
        
        // Use local description if available, otherwise provider
        merged.setDescription(local.getDescription() != null && !local.getDescription().isEmpty() 
            ? local.getDescription() : provider.getDescription());
        merged.setNotes(local.getNotes());
        
        // Merge capabilities - use local if explicitly set, otherwise provider
        merged.setSupportsText(local.isSupportsText() || provider.isSupportsText());
        merged.setSupportsImage(local.isSupportsImage() || provider.isSupportsImage());
        merged.setSupportsPdf(local.isSupportsPdf() || provider.isSupportsPdf());
        
        // Merge advanced capabilities
        Integer localMaxTokens = LlmConfigurationAdapter.getMaxTokens(local);
        Integer providerMaxTokens = LlmConfigurationAdapter.getMaxTokens(provider);
        LlmConfigurationAdapter.setMaxTokens(merged, localMaxTokens != null ? localMaxTokens : providerMaxTokens);
        
        boolean localSupportsJson = LlmConfigurationAdapter.isSupportsJson(local);
        boolean providerSupportsJson = LlmConfigurationAdapter.isSupportsJson(provider);
        LlmConfigurationAdapter.setSupportsJson(merged, localSupportsJson || providerSupportsJson);
        
        boolean localSupportsTools = LlmConfigurationAdapter.isSupportsTools(local);
        boolean providerSupportsTools = LlmConfigurationAdapter.isSupportsTools(provider);
        LlmConfigurationAdapter.setSupportsTools(merged, localSupportsTools || providerSupportsTools);
        
        return merged;
    }

    /**
     * Gets LLM configuration by ID, checking both local and provider sources.
     * This method first checks local configurations, then falls back to provider models.
     */
    public LlmConfiguration getLlmConfigurationMerged(String llmId) {
        // First check local configuration
        LlmConfiguration localConfig = getLlmConfiguration(llmId);
        if (localConfig != null) {
            return localConfig;
        }
        
        // If not found locally, check provider models
        try {
            List<ProviderModel> providerModels = openAIService.getAvailableModels();
            for (ProviderModel model : providerModels) {
                if (llmId.equals(model.getId())) {
                    // Convert provider model to LlmConfiguration
                    List<LlmConfiguration> configs = convertProviderModelsToConfigurations(List.of(model));
                    return configs.isEmpty() ? null : configs.get(0);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch models from provider when looking for {}: {}", llmId, e.getMessage());
        }
        
        return null;
    }
}
