package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model representing a language model available from an LLM provider.
 * 
 * <p>This class encapsulates information about individual models available from
 * OpenAI-compatible API providers, including their capabilities, context limits,
 * and other metadata that helps in model selection and feature availability.</p>
 * 
 * <h3>Core Information:</h3>
 * <ul>
 * <li><strong>Model Identity:</strong> ID, name, and provider identification</li>
 * <li><strong>Capabilities:</strong> Supported features like vision, function calling, etc.</li>
 * <li><strong>Context Limits:</strong> Maximum token context window and output limits</li>
 * <li><strong>Pricing Information:</strong> Cost tier information for usage optimization</li>
 * </ul>
 * 
 * <h3>Feature Detection:</h3>
 * <ul>
 * <li><strong>Vision Support:</strong> Whether the model can process images</li>
 * <li><strong>Function Calling:</strong> Support for tool/function execution</li>
 * <li><strong>JSON Mode:</strong> Structured output generation capabilities</li>
 * <li><strong>System Messages:</strong> Support for system role instructions</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see LlmConfiguration
 */
public class ProviderModel {
    
    /**
     * Unique identifier for the model as used in API requests.
     * This matches the model ID used in OpenAI-compatible API calls.
     */
    private String id;
    
    /**
     * Human-readable name of the model for display purposes.
     * May include version information and provider details.
     */
    private String name;
    
    /**
     * The organization or provider that created/owns this model.
     */
    private String ownedBy;
    
    /**
     * Timestamp when this model was created (Unix timestamp).
     */
    private Long created;
    
    /**
     * Maximum context window size in tokens.
     * Indicates how much conversation history and input the model can process.
     */
    private Integer maxContextTokens;
    
    /**
     * Maximum number of tokens the model can generate in a single response.
     */
    private Integer maxOutputTokens;
    
    /**
     * Whether this model supports vision/image processing capabilities.
     */
    private Boolean visionEnabled;
    
    /**
     * Whether this model supports function/tool calling.
     */
    private Boolean functionCallingEnabled;
    
    /**
     * Whether this model supports JSON mode for structured output.
     */
    private Boolean jsonModeEnabled;
      /**
     * Whether this model supports system messages.
     */
    private Boolean systemMessageSupport;
    
    /**
     * Brief description of the model's strengths and use cases.
     */
    private String description;
    
    /**
     * Additional capabilities or limitations as a free-form string.
     */
    private String capabilities;

    // Default constructor
    public ProviderModel() {
    }

    // Constructor with essential fields
    public ProviderModel(String id, String name, String ownedBy) {
        this.id = id;
        this.name = name;
        this.ownedBy = ownedBy;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("owned_by")
    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Integer getMaxContextTokens() {
        return maxContextTokens;
    }

    public void setMaxContextTokens(Integer maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(Integer maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public Boolean getVisionEnabled() {
        return visionEnabled;
    }

    public void setVisionEnabled(Boolean visionEnabled) {
        this.visionEnabled = visionEnabled;
    }

    public Boolean getFunctionCallingEnabled() {
        return functionCallingEnabled;
    }

    public void setFunctionCallingEnabled(Boolean functionCallingEnabled) {
        this.functionCallingEnabled = functionCallingEnabled;
    }

    public Boolean getJsonModeEnabled() {
        return jsonModeEnabled;
    }

    public void setJsonModeEnabled(Boolean jsonModeEnabled) {
        this.jsonModeEnabled = jsonModeEnabled;
    }

    public Boolean getSystemMessageSupport() {
        return systemMessageSupport;
    }    public void setSystemMessageSupport(Boolean systemMessageSupport) {
        this.systemMessageSupport = systemMessageSupport;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Checks if this model supports vision/image processing.
     * 
     * @return true if vision is enabled, false otherwise
     */
    public boolean isVisionCapable() {
        return Boolean.TRUE.equals(visionEnabled);
    }

    /**
     * Checks if this model supports function/tool calling.
     * 
     * @return true if function calling is enabled, false otherwise
     */
    public boolean isFunctionCallingCapable() {
        return Boolean.TRUE.equals(functionCallingEnabled);
    }

    /**
     * Checks if this model supports JSON structured output mode.
     * 
     * @return true if JSON mode is enabled, false otherwise
     */
    public boolean isJsonModeCapable() {
        return Boolean.TRUE.equals(jsonModeEnabled);
    }

    @Override
    public String toString() {
        return "ProviderModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ownedBy='" + ownedBy + '\'' +
                ", visionEnabled=" + visionEnabled +
                ", functionCallingEnabled=" + functionCallingEnabled +
                '}';
    }
}
