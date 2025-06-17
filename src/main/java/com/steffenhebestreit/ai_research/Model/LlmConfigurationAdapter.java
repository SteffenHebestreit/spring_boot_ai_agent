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
     * @return The maximum tokens
     */
    public static int getMaxTokens(LlmConfiguration config) {
        if (config != null) {
            return config.getMaxContextTokens();
        }
        return 0;
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
}
