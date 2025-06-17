package com.steffenhebestreit.ai_research.Model;

/**
 * Extension methods for ProviderModel to support capabilities needed in OpenAIService
 * when the standard methods aren't available.
 */
public class ProviderModelAdapter {
    
    /**
     * Set the display name of the model
     * 
     * @param model The model to update
     * @param displayName The display name to set
     */
    public static void setDisplayName(ProviderModel model, String displayName) {
        if (model != null) {
            model.setName(displayName);
        }
    }
    
    /**
     * Get the display name of the model
     * 
     * @param model The model to query
     * @return The display name
     */
    public static String getDisplayName(ProviderModel model) {
        if (model != null) {
            return model.getName();
        }
        return null;
    }
    
    /**
     * Set the provider of the model
     * 
     * @param model The model to update
     * @param provider The provider to set
     */
    public static void setProvider(ProviderModel model, String provider) {
        if (model != null) {
            model.setOwnedBy(provider);
        }
    }
    
    /**
     * Set the token limit for the model
     * 
     * @param model The model to update
     * @param tokenLimit The token limit to set
     */
    public static void setTokenLimit(ProviderModel model, int tokenLimit) {
        if (model != null) {
            model.setMaxContextTokens(tokenLimit);
        }
    }
    
    /**
     * Set whether the model supports text input
     * 
     * @param model The model to update
     * @param supportsText Whether the model supports text
     */
    public static void setSupportsText(ProviderModel model, boolean supportsText) {
        // No direct property, all models support text
    }
    
    /**
     * Set whether the model supports image input
     * 
     * @param model The model to update
     * @param supportsImage Whether the model supports images
     */
    public static void setSupportsImage(ProviderModel model, boolean supportsImage) {
        if (model != null) {
            model.setVisionEnabled(supportsImage);
        }
    }
    
    /**
     * Set whether the model supports PDF input
     * 
     * @param model The model to update
     * @param supportsPdf Whether the model supports PDFs
     */
    public static void setSupportsPdf(ProviderModel model, boolean supportsPdf) {
        // No direct property for PDF support in ProviderModel
        // Could add a custom field or property if needed
    }
    
    /**
     * Set whether the model supports JSON output
     * 
     * @param model The model to update
     * @param supportsJson Whether the model supports JSON
     */
    public static void setSupportsJson(ProviderModel model, boolean supportsJson) {
        if (model != null) {
            model.setJsonModeEnabled(supportsJson);
        }
    }
    
    /**
     * Set whether the model supports tools/functions
     * 
     * @param model The model to update
     * @param supportsTools Whether the model supports tools
     */
    public static void setSupportsTools(ProviderModel model, boolean supportsTools) {
        if (model != null) {
            model.setFunctionCallingEnabled(supportsTools);
        }
    }
}
