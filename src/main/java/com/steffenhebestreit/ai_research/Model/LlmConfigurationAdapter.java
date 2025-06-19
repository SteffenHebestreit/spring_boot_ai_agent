package com.steffenhebestreit.ai_research.Model;

/**
 * Extension methods for LlmConfiguration to support capabilities needed in OpenAIService
 * when the standard methods aren't available.
 */
public class LlmConfigurationAdapter {
      /**
     * Get the maximum tokens for the configuration
     * 
     * @param config The configuration to query
     * @return The maximum tokens or null if not set
     */
    public static Integer getMaxTokens(LlmConfiguration config) {
        if (config != null) {
            return config.getMaxContextTokens();
        }
        return null;
    }
    
    /**
     * Check if the configuration supports JSON output
     * 
     * @param config The configuration to query
     * @return Whether JSON is supported
     */
    public static boolean isSupportsJson(LlmConfiguration config) {
        if (config != null) {
            return config.isSupportsJsonMode();
        }
        return false;
    }
    
    /**
     * Check if the configuration supports tools/functions
     * 
     * @param config The configuration to query
     * @return Whether tools are supported
     */
    public static boolean isSupportsTools(LlmConfiguration config) {
        if (config != null) {
            return config.isSupportsFunctionCalling();
        }
        return false;
    }
    
    /**
     * Set the maximum tokens for the configuration
     * 
     * @param config The configuration to update
     * @param maxTokens The maximum tokens to set
     */
    public static void setMaxTokens(LlmConfiguration config, Integer maxTokens) {
        if (config != null && maxTokens != null) {
            config.setMaxContextTokens(maxTokens);
        }
    }
    
    /**
     * Set whether the configuration supports JSON output
     * 
     * @param config The configuration to update
     * @param supportsJson Whether JSON is supported
     */
    public static void setSupportsJson(LlmConfiguration config, boolean supportsJson) {
        if (config != null) {
            config.setSupportsJsonMode(supportsJson);
        }
    }
    
    /**
     * Set whether the configuration supports tools/functions
     * 
     * @param config The configuration to update
     * @param supportsTools Whether tools are supported
     */
    public static void setSupportsTools(LlmConfiguration config, boolean supportsTools) {
        if (config != null) {
            config.setSupportsFunctionCalling(supportsTools);
        }
    }
}
