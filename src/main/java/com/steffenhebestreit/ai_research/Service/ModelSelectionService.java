package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Model.ProviderModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for managing LLM model information and selection recommendations.
 * 
 * <p>This service provides comprehensive functionality for LLM model management including
 * provider integration, capability analysis, and intelligent model selection based on
 * user requirements and content types.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>Provider Integration:</strong> Fetch real-time model data from LLM providers</li>
 * <li><strong>Capability Analysis:</strong> Analyze and enhance model capability information</li>
 * <li><strong>Smart Recommendations:</strong> Provide intelligent model selection suggestions</li>
 * <li><strong>Configuration Merging:</strong> Combine provider data with local configurations</li>
 * </ul>
 * 
 * <h3>Selection Criteria:</h3>
 * <ul>
 * <li><strong>Content Type Support:</strong> Text, vision, multimodal capabilities</li>
 * <li><strong>Performance Requirements:</strong> Speed vs quality optimization</li>
 * <li><strong>Cost Considerations:</strong> Free, standard, premium tier matching</li>
 * <li><strong>Technical Features:</strong> Function calling, JSON mode, context length</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0 * @since 1.0
 * @see OpenAIService
 * @see LlmCapabilityService
 * @see ProviderModel
 */
@Service
public class ModelSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelSelectionService.class);
    
    @Autowired
    private OpenAIService openAIService;
    
    @Autowired
    private LlmCapabilityService llmCapabilityService;
    
    /**
     * Analyzes available models and provides usage statistics and insights.
     * 
     * <p>This method examines all available models from both provider APIs and local
     * configurations to generate comprehensive statistics about model capabilities,
     * distribution, and potential usage patterns.</p>
     * 
     * @return Map containing detailed model analytics and statistics
     */
    public Map<String, Object> getModelAnalytics() {
        try {
            List<ProviderModel> providerModels = openAIService.getAvailableModels();
            List<LlmConfiguration> localModels = llmCapabilityService.getAllLlmConfigurations();
            
            // Calculate statistics
            long visionEnabledCount = providerModels.stream()
                    .mapToLong(model -> Boolean.TRUE.equals(model.getVisionEnabled()) ? 1 : 0)
                    .sum();
            
            long functionCallingCount = providerModels.stream()
                    .mapToLong(model -> Boolean.TRUE.equals(model.getFunctionCallingEnabled()) ? 1 : 0)
                    .sum();
              Map<String, Long> ownerDistribution = providerModels.stream()
                    .filter(model -> model.getOwnedBy() != null)
                    .collect(Collectors.groupingBy(ProviderModel::getOwnedBy, Collectors.counting()));
            
            return Map.of(
                "totalProviderModels", providerModels.size(),
                "totalLocalConfigurations", localModels.size(),
                "visionEnabledModels", visionEnabledCount,
                "functionCallingModels", functionCallingCount,
                "ownerDistribution", ownerDistribution,
                "analysisTimestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error("Error generating model analytics: {}", e.getMessage(), e);
            return Map.of(
                "error", "Failed to generate analytics: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Validates model compatibility with specific requirements.
     * 
     * <p>This method checks if a specific model meets the given requirements
     * for content type support, performance characteristics, and feature availability.</p>
     * 
     * @param modelId The ID of the model to validate
     * @param contentType Required content type support
     * @param requiredFeatures List of required features
     * @return Map containing validation results and compatibility details
     */
    public Map<String, Object> validateModelCompatibility(String modelId, String contentType, List<String> requiredFeatures) {
        try {
            // Get model information
            ProviderModel providerModel = openAIService.getAvailableModels().stream()
                    .filter(model -> model.getId().equals(modelId))
                    .findFirst()
                    .orElse(null);
            
            LlmConfiguration localConfig = llmCapabilityService.getLlmConfiguration(modelId);
            
            if (providerModel == null && localConfig == null) {
                return Map.of(
                    "compatible", false,
                    "error", "Model not found: " + modelId,
                    "modelId", modelId
                );
            }
            
            // Validate content type support
            boolean contentTypeSupported = validateContentTypeSupport(providerModel, localConfig, contentType);
            
            // Validate required features
            Map<String, Boolean> featureSupport = validateFeatureSupport(providerModel, localConfig, requiredFeatures);
            
            boolean allFeaturesSupported = featureSupport.values().stream().allMatch(Boolean::booleanValue);
            
            return Map.of(
                "compatible", contentTypeSupported && allFeaturesSupported,
                "modelId", modelId,
                "contentTypeSupported", contentTypeSupported,
                "featureSupport", featureSupport,
                "modelInfo", createModelSummary(providerModel, localConfig),
                "validationTimestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error("Error validating model compatibility for {}: {}", modelId, e.getMessage(), e);
            return Map.of(
                "compatible", false,
                "error", "Validation failed: " + e.getMessage(),
                "modelId", modelId
            );
        }
    }
    
    /**
     * Validates content type support for a model.
     */
    private boolean validateContentTypeSupport(ProviderModel providerModel, LlmConfiguration localConfig, String contentType) {
        if (contentType == null || "text".equalsIgnoreCase(contentType)) {
            return true; // All models support text
        }
        
        switch (contentType.toLowerCase()) {
            case "image":
            case "vision":
                if (providerModel != null && Boolean.TRUE.equals(providerModel.getVisionEnabled())) {
                    return true;
                }
                return localConfig != null && localConfig.isSupportsImage();
                
            case "pdf":
                return localConfig != null && localConfig.isSupportsPdf();
                
            case "multimodal":
                boolean hasVision = (providerModel != null && Boolean.TRUE.equals(providerModel.getVisionEnabled())) ||
                                   (localConfig != null && localConfig.isSupportsImage());
                boolean hasPdf = localConfig != null && localConfig.isSupportsPdf();
                return hasVision || hasPdf;
                
            default:
                return true;
        }
    }
    
    /**
     * Validates feature support for a model.
     */
    private Map<String, Boolean> validateFeatureSupport(ProviderModel providerModel, LlmConfiguration localConfig, List<String> requiredFeatures) {
        if (requiredFeatures == null || requiredFeatures.isEmpty()) {
            return Map.of();
        }
        
        return requiredFeatures.stream().collect(Collectors.toMap(
            feature -> feature,
            feature -> checkFeatureSupport(providerModel, localConfig, feature)
        ));
    }
    
    /**
     * Checks if a specific feature is supported by the model.
     */
    private boolean checkFeatureSupport(ProviderModel providerModel, LlmConfiguration localConfig, String feature) {
        switch (feature.toLowerCase()) {
            case "function_calling":
            case "tools":
                return providerModel != null && Boolean.TRUE.equals(providerModel.getFunctionCallingEnabled());
                
            case "json_mode":
                return providerModel != null && Boolean.TRUE.equals(providerModel.getJsonModeEnabled());
                
            case "vision":
            case "image":
                return (providerModel != null && Boolean.TRUE.equals(providerModel.getVisionEnabled())) ||
                       (localConfig != null && localConfig.isSupportsImage());
                
            case "large_context":
                return providerModel != null && providerModel.getMaxContextTokens() != null && 
                       providerModel.getMaxContextTokens() > 32000;
                
            case "system_message":
                return providerModel == null || !Boolean.FALSE.equals(providerModel.getSystemMessageSupport());
                
            default:
                logger.warn("Unknown feature requested for validation: {}", feature);
                return false;
        }
    }
    
    /**
     * Creates a summary of model information from available sources.
     */
    private Map<String, Object> createModelSummary(ProviderModel providerModel, LlmConfiguration localConfig) {
        Map<String, Object> summary = new java.util.HashMap<>();
          if (providerModel != null) {
            summary.put("providerId", providerModel.getId());
            summary.put("providerName", providerModel.getName());
            summary.put("ownedBy", providerModel.getOwnedBy());
            summary.put("maxContextTokens", providerModel.getMaxContextTokens());
        }
        
        if (localConfig != null) {
            summary.put("localId", localConfig.getId());
            summary.put("localName", localConfig.getName());
            summary.put("localNotes", localConfig.getNotes());
            summary.put("supportsText", localConfig.isSupportsText());
            summary.put("supportsImage", localConfig.isSupportsImage());
            summary.put("supportsPdf", localConfig.isSupportsPdf());
        }
        
        return summary;
    }
    
    /**
     * Gets model selection history and usage patterns.
     * 
     * <p>This method would typically integrate with usage tracking to provide
     * insights about model selection patterns and performance metrics.</p>
     * 
     * @return Map containing usage statistics and selection patterns
     */
    public Map<String, Object> getModelUsageInsights() {
        // This is a placeholder for future implementation
        // In a real system, this would query usage logs and metrics
        
        return Map.of(
            "message", "Model usage tracking not yet implemented",
            "suggestedImplementation", "Integrate with application metrics and usage logging",
            "timestamp", System.currentTimeMillis()
        );
    }
    
    /**
     * Identifies models that have inferred capabilities and may need manual configuration.
     * 
     * @return Map containing unknown models and configuration suggestions
     */
    public Map<String, Object> getUnknownModelsReport() {
        try {
            List<ProviderModel> providerModels = openAIService.getAvailableModels();
            List<LlmConfiguration> localConfigs = llmCapabilityService.getAllLlmConfigurations();
            
            // Find models with inferred capabilities
            List<Map<String, Object>> unknownModels = new ArrayList<>();
            List<String> suggestedConfigurations = new ArrayList<>();
            
            for (ProviderModel model : providerModels) {
                if (isModelCapabilitiesInferred(model)) {
                    Map<String, Object> modelInfo = new HashMap<>();                    modelInfo.put("id", model.getId());
                    modelInfo.put("name", model.getName());
                    modelInfo.put("ownedBy", model.getOwnedBy());
                    modelInfo.put("inferredCapabilities", model.getCapabilities());
                    modelInfo.put("description", model.getDescription());
                    
                    // Check if there's a local config
                    boolean hasLocalConfig = localConfigs.stream()
                            .anyMatch(config -> config.getId().equals(model.getId()));
                    modelInfo.put("hasLocalConfiguration", hasLocalConfig);
                    
                    unknownModels.add(modelInfo);
                    
                    // Generate configuration suggestion
                    String configSuggestion = generateConfigurationSuggestion(model);
                    suggestedConfigurations.add(configSuggestion);
                }
            }
            
            return Map.of(
                "unknownModels", unknownModels,
                "totalUnknown", unknownModels.size(),
                "totalModels", providerModels.size(),
                "configurationSuggestions", suggestedConfigurations,
                "recommendedAction", "Review and configure unknown models for better accuracy",
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error("Error generating unknown models report: {}", e.getMessage(), e);
            return Map.of(
                "error", "Failed to generate report: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Checks if a model's capabilities were inferred rather than explicitly known.
     */
    private boolean isModelCapabilitiesInferred(ProviderModel model) {
        if (model.getCapabilities() == null) {
            return false;
        }
        
        // Check if capabilities string contains "Inferred" or if it's a basic fallback
        return model.getCapabilities().contains("(Inferred)") || 
               model.getCapabilities().contains("(Basic)") ||
               (model.getDescription() != null && model.getDescription().contains("pattern matching"));
    }
    
    /**
     * Generates a configuration suggestion for an unknown model.
     */
    private String generateConfigurationSuggestion(ProviderModel model) {
        StringBuilder suggestion = new StringBuilder();
        suggestion.append("# Configuration for ").append(model.getId()).append("\n");
        suggestion.append("llm.configurations[].id=").append(model.getId()).append("\n");
        suggestion.append("llm.configurations[].name=").append(model.getName() != null ? model.getName() : "Custom " + model.getId()).append("\n");
        suggestion.append("llm.configurations[].supportsText=true\n");
        suggestion.append("llm.configurations[].supportsImage=").append(Boolean.TRUE.equals(model.getVisionEnabled())).append("\n");
        suggestion.append("llm.configurations[].supportsPdf=false\n");
        suggestion.append("llm.configurations[].notes=").append(model.getDescription() != null ? model.getDescription() : "Custom configuration needed").append("\n");
        
        return suggestion.toString();
    }
    
    /**
     * Provides intelligent recommendations for configuring unknown models.
     * 
     * @param modelId The ID of the model to get configuration help for
     * @return Map containing configuration recommendations and detected patterns
     */
    public Map<String, Object> getModelConfigurationHelp(String modelId) {
        try {
            List<ProviderModel> providerModels = openAIService.getAvailableModels();
            ProviderModel targetModel = providerModels.stream()
                    .filter(model -> model.getId().equals(modelId))
                    .findFirst()
                    .orElse(null);
            
            if (targetModel == null) {
                return Map.of(
                    "error", "Model not found: " + modelId,
                    "available", false
                );
            }
            
            // Analyze model patterns
            List<String> detectedPatterns = analyzeModelPatterns(modelId);
            List<String> configurationSteps = generateConfigurationSteps(targetModel);
            Map<String, Object> similarModels = findSimilarModels(targetModel, providerModels);
            
            return Map.of(
                "modelId", modelId,
                "currentCapabilities", targetModel.getCapabilities(),
                "detectedPatterns", detectedPatterns,
                "configurationSteps", configurationSteps,
                "similarModels", similarModels,
                "isInferred", isModelCapabilitiesInferred(targetModel),
                "recommendedConfiguration", generateConfigurationSuggestion(targetModel),
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            logger.error("Error generating configuration help for model {}: {}", modelId, e.getMessage(), e);
            return Map.of(
                "error", "Failed to generate configuration help: " + e.getMessage(),
                "modelId", modelId,
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Analyzes patterns in the model ID to suggest likely capabilities.
     */
    private List<String> analyzeModelPatterns(String modelId) {
        List<String> patterns = new ArrayList<>();
        String lowerModelId = modelId.toLowerCase();
        
        if (lowerModelId.contains("vision") || lowerModelId.contains("v") || lowerModelId.contains("multimodal")) {
            patterns.add("Vision capabilities likely (contains vision-related keywords)");
        }
        
        if (lowerModelId.contains("instruct") || lowerModelId.contains("chat") || lowerModelId.contains("turbo")) {
            patterns.add("Instruction-following model (may support function calling)");
        }
        
        if (lowerModelId.contains("32k") || lowerModelId.contains("128k") || lowerModelId.contains("200k")) {
            patterns.add("Extended context window detected in name");
        }
        
        if (lowerModelId.contains("mini") || lowerModelId.contains("small") || lowerModelId.contains("7b")) {
            patterns.add("Smaller model (likely lower cost, faster inference)");
        }
        
        if (lowerModelId.contains("large") || lowerModelId.contains("70b") || lowerModelId.contains("405b")) {
            patterns.add("Large model (likely higher capability, higher cost)");
        }
        
        return patterns;
    }
    
    /**
     * Generates step-by-step configuration instructions.
     */
    private List<String> generateConfigurationSteps(ProviderModel model) {
        List<String> steps = new ArrayList<>();
        
        steps.add("1. Test the model with basic text prompts to verify connectivity");
        steps.add("2. Test with images if vision capabilities are suspected");
        steps.add("3. Try function calling if the model appears to be instruction-tuned");
        steps.add("4. Measure actual context window by testing with long inputs");
        steps.add("5. Add local configuration with verified capabilities");
        steps.add("6. Update pricing tier based on provider documentation");
        
        return steps;
    }
    
    /**
     * Finds models with similar characteristics for comparison.
     */
    private Map<String, Object> findSimilarModels(ProviderModel targetModel, List<ProviderModel> allModels) {
        List<String> similarModels = new ArrayList<>();
        String targetOwner = targetModel.getOwnedBy();
        
        // Find models from same provider
        for (ProviderModel model : allModels) {
            if (!model.getId().equals(targetModel.getId()) && 
                targetOwner != null && targetOwner.equals(model.getOwnedBy())) {
                similarModels.add(model.getId() + " (same provider)");
            }
        }
        
        // Find models with similar capabilities
        for (ProviderModel model : allModels) {
            if (!model.getId().equals(targetModel.getId()) && 
                haveSimilarCapabilities(targetModel, model)) {
                similarModels.add(model.getId() + " (similar capabilities)");
            }
        }
        
        return Map.of(
            "models", similarModels,
            "suggestion", "Compare with these models to validate capability assumptions"
        );
    }
    
    /**
     * Checks if two models have similar capabilities.
     */    private boolean haveSimilarCapabilities(ProviderModel model1, ProviderModel model2) {
        return Objects.equals(model1.getVisionEnabled(), model2.getVisionEnabled()) &&
               Objects.equals(model1.getFunctionCallingEnabled(), model2.getFunctionCallingEnabled()) &&
               Objects.equals(model1.getJsonModeEnabled(), model2.getJsonModeEnabled());
    }
}
