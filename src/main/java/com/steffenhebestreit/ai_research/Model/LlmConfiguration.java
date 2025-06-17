package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration model representing the capabilities and metadata of a Large Language Model (LLM)
 * in the AI Research system. This class serves as a comprehensive descriptor for LLM instances,
 * enabling dynamic capability detection and intelligent routing of requests based on input types.
 * 
 * <h3>Core Configuration Components:</h3>
 * <ul>
 *   <li><strong>Model Identity</strong> - Unique ID and human-readable name for model identification</li>
 *   <li><strong>Capability Matrix</strong> - Precise mapping of supported input modalities</li>
 *   <li><strong>Metadata Support</strong> - Additional configuration notes and limitations</li>
 *   <li><strong>JSON Serialization</strong> - Optimized API response format with capabilities object</li>
 * </ul>
 * 
 * <h3>Supported Input Modalities:</h3>
 * <ul>
 *   <li><strong>Text Processing</strong> - Standard text input support (enabled by default)</li>
 *   <li><strong>Vision Capabilities</strong> - Image analysis and multimodal understanding</li>
 *   <li><strong>Document Processing</strong> - PDF parsing and document comprehension</li>
 *   <li><strong>Function Calling</strong> - Ability to use tools or functions</li>
 *   <li><strong>JSON Mode</strong> - Ability to output JSON</li>
 * </ul>
 * 
 * <h3>Builder Pattern Support:</h3>
 * The class provides a fluent builder pattern for convenient construction with optional
 * parameters and sensible defaults. Text support is enabled by default while image
 * and PDF capabilities are opt-in based on model specifications.
 * 
 * <h3>API Integration:</h3>
 * Designed for seamless integration with AI Research APIs, providing structured
 * capability information through the {@link #getCapabilities()} method that returns
 * a nested capabilities object optimized for JSON responses.
 * 
 * @author Steffen Hebestreit
 * @version 1.1
 * @since 1.0
 * @see com.steffenhebestreit.ai_research.Model.Message
 * @see com.steffenhebestreit.ai_research.Model.ChatMessage
 */
public class LlmConfiguration {
      /**
     * Unique identifier for the Large Language Model used in API requests and internal routing.
     * This ID must be consistent with the model provider's naming convention and should be
     * URL-safe for REST API usage.
     * 
     * <p>Examples: "gpt-4", "claude-3-opus", "gemini-pro"</p>
     */
    private String id;
      /**
     * Human-readable display name for the LLM presented in user interfaces.
     * This name should be descriptive and user-friendly, potentially including
     * version information or provider details.
     * 
     * <p>Examples: "GPT-4", "Claude 3 Opus", "Gemini Pro"</p>
     */
    private String name;
      /**
     * Indicates whether the model supports standard text input processing.
     * Nearly all modern LLMs support text input, making this the baseline capability.
     * This flag is set to {@code true} by default.
     * 
     * <p>When {@code false}, the model is typically specialized for non-text modalities only.</p>
     */
    private boolean supportsText = true;
      /**
     * Indicates whether the model supports image input and vision-based processing.
     * Vision-enabled models can analyze images, describe visual content, read text from images,
     * and perform multimodal reasoning combining text and visual information.
     * This capability is {@code false} by default and must be explicitly enabled.
     * 
     * <p>Supported image formats typically include: JPEG, PNG, WebP, and GIF</p>
     */
    private boolean supportsImage = false;
      /**
     * Indicates whether the model supports PDF document processing.
     * This capability enables direct PDF analysis, content extraction, and document comprehension.
     * Models with PDF support can parse document structure, extract text, and understand
     * document context without requiring pre-processing. This capability is {@code false} by default.
     * 
     * <p>PDF support may include limitations on file size, page count, or complex formatting.</p>
     */
    private boolean supportsPdf = false;
      /**
     * Indicates whether the model supports function calling (tool use).
     * This capability is {@code false} by default and must be explicitly enabled.
     */
    private boolean supportsFunctionCalling = false;

    /**
     * Indicates whether the model supports JSON mode for output.
     * This capability is {@code false} by default and must be explicitly enabled.
     */
    private boolean supportsJsonMode = false;

    /**
     * Maximum number of context tokens the model can handle.
     * Default is 0, indicating unknown or not applicable.
     */
    private int maxContextTokens = 0;

    /**
     * Maximum number of output tokens the model can generate.
     * Default is 0, indicating unknown or not applicable.
     */
    private int maxOutputTokens = 0;    /**
     * A brief description of the model.
     * Default is null.
     */
    private String description;
      /**
     * Additional configuration notes, limitations, or special requirements for the model.
     * This field provides flexible metadata storage for model-specific information such as
     * maximum file sizes, token limits, rate limiting details, or usage restrictions.
     * 
     * <p>Examples: "Max image size: 20MB", "Rate limit: 10 requests/minute", "Beta version"</p>
     */
    private String notes;    /**
     * Default constructor creating an LlmConfiguration instance with default capability settings.
     * Initializes the configuration with text support enabled and all other capabilities disabled.
     * 
     * <p><strong>Initial State:</strong></p>
     * <ul>
     *   <li>Text support: {@code true} (enabled by default)</li>
     *   <li>Image support: {@code false} (disabled by default)</li>
     *   <li>PDF support: {@code false} (disabled by default)</li>
     *   <li>Function Calling support: {@code false} (disabled by default)</li>
     *   <li>JSON Mode support: {@code false} (disabled by default)</li>
     *   <li>Token limits: {@code 0}</li>
     *   <li>Pricing Tier: {@code null}</li>
     *   <li>Description: {@code null}</li>
     *   <li>All other fields: {@code null}</li>
     * </ul>
     */
    public LlmConfiguration() {
    }    /**
     * Comprehensive constructor for creating a fully configured LlmConfiguration instance.
     * This constructor allows explicit specification of all model capabilities and metadata,
     * providing complete control over the configuration.
     * 
     * @param id the unique identifier for the LLM (must be non-null and URL-safe)
     * @param name the human-readable display name for the model
     * @param supportsText whether the model supports text input processing
     * @param supportsImage whether the model supports image input and vision capabilities
     * @param supportsPdf whether the model supports PDF document processing
     * @param supportsFunctionCalling whether the model supports function calling
     * @param supportsJsonMode whether the model supports JSON output mode     * @param maxContextTokens maximum context tokens
     * @param maxOutputTokens maximum output tokens
     * @param description a brief description of the model
     * @param notes additional configuration notes, limitations, or requirements
     * 
     * <p><strong>Usage Example:</strong></p>
     * <pre>
     * LlmConfiguration config = new LlmConfiguration(
     *     "gpt-4-vision",
     *     "GPT-4 Vision",
     *     true,  // supports text
     *     true,  // supports images
     *     false, // no PDF support
     *     true,  // supports function calling
     *     true,  // supports JSON mode     *     128000, // max context tokens
     *     4096,  // max output tokens
     *     "Latest GPT-4 model with vision capabilities.", // description
     *     "Max image size: 20MB"
     * );
     * </pre>
     */    public LlmConfiguration(String id, String name, boolean supportsText, boolean supportsImage, boolean supportsPdf, 
                            boolean supportsFunctionCalling, boolean supportsJsonMode, 
                            int maxContextTokens, int maxOutputTokens, String description, String notes) {
        this.id = id;
        this.name = name;
        this.supportsText = supportsText;
        this.supportsImage = supportsImage;
        this.supportsPdf = supportsPdf;
        this.supportsFunctionCalling = supportsFunctionCalling;
        this.supportsJsonMode = supportsJsonMode;        this.maxContextTokens = maxContextTokens;
        this.maxOutputTokens = maxOutputTokens;
        this.description = description;
        this.notes = notes;
    }
      /**
     * Creates a new builder instance for constructing LlmConfiguration objects using the builder pattern.
     * The builder provides a fluent interface for setting configuration parameters with sensible defaults.
     * 
     * @return a new LlmConfigurationBuilder instance with default settings
     * 
     * <p><strong>Usage Example:</strong></p>
     * <pre>     * LlmConfiguration config = LlmConfiguration.builder()
     *     .id("claude-3-opus")
     *     .name("Claude 3 Opus")
     *     .supportsText(true)
     *     .supportsImage(true)
     *     .supportsFunctionCalling(true)
     *     .maxContextTokens(200000)
     *     .description("Anthropic's most powerful model.")
     *     .notes("Premium model with advanced reasoning")
     *     .build();
     * </pre>
     */
    public static LlmConfigurationBuilder builder() {
        return new LlmConfigurationBuilder();
    }
      /**
     * Creates a structured capabilities object optimized for JSON serialization in API responses.
     * This method transforms the individual capability flags into a nested object structure
     * that provides clear capability information for client applications.
     * 
     * @return a Capabilities object containing all supported input modalities
     * 
     * <p><strong>JSON Structure:</strong></p>
     * <pre>
     * {
     *   "capabilities": {
     *     "text": true,
     *     "image": false,
     *     "pdf": true
     *   }
     * }
     * </pre>
     */
    @JsonProperty("capabilities")
    public Capabilities getCapabilities() {
        return new Capabilities(supportsText, supportsImage, supportsPdf, supportsFunctionCalling, supportsJsonMode);
    }
      /**
     * Returns the unique identifier for this LLM configuration.
     * 
     * @return the model ID used for API requests and internal routing
     */
    public String getId() {
        return id;
    }
      /**
     * Sets the unique identifier for this LLM configuration.
     * 
     * @param id the model ID to set (should be non-null and URL-safe)
     */
    public void setId(String id) {
        this.id = id;
    }
      /**
     * Returns the human-readable display name for this LLM.
     * 
     * @return the user-friendly model name for UI display
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the human-readable display name for this LLM.
     * 
     * @param name the display name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns whether this model supports text input processing.
     * 
     * @return {@code true} if text input is supported, {@code false} otherwise
     */
    public boolean isSupportsText() {
        return supportsText;
    }
    
    /**
     * Sets whether this model supports text input processing.
     * 
     * @param supportsText {@code true} to enable text support, {@code false} to disable
     */
    public void setSupportsText(boolean supportsText) {
        this.supportsText = supportsText;
    }
    
    /**
     * Returns whether this model supports image input and vision capabilities.
     * 
     * @return {@code true} if image processing is supported, {@code false} otherwise
     */
    public boolean isSupportsImage() {
        return supportsImage;
    }
    
    /**
     * Sets whether this model supports image input and vision capabilities.
     * 
     * @param supportsImage {@code true} to enable image support, {@code false} to disable
     */
    public void setSupportsImage(boolean supportsImage) {
        this.supportsImage = supportsImage;
    }
    
    /**
     * Returns whether this model supports PDF document processing.
     * 
     * @return {@code true} if PDF processing is supported, {@code false} otherwise
     */
    public boolean isSupportsPdf() {
        return supportsPdf;
    }
    
    /**
     * Sets whether this model supports PDF document processing.
     * 
     * @param supportsPdf {@code true} to enable PDF support, {@code false} to disable
     */
    public void setSupportsPdf(boolean supportsPdf) {
        this.supportsPdf = supportsPdf;
    }

    public boolean isSupportsFunctionCalling() {
        return supportsFunctionCalling;
    }

    public void setSupportsFunctionCalling(boolean supportsFunctionCalling) {
        this.supportsFunctionCalling = supportsFunctionCalling;
    }

    public boolean isSupportsJsonMode() {
        return supportsJsonMode;
    }

    public void setSupportsJsonMode(boolean supportsJsonMode) {
        this.supportsJsonMode = supportsJsonMode;
    }

    public int getMaxContextTokens() {
        return maxContextTokens;
    }

    public void setMaxContextTokens(int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {        this.maxOutputTokens = maxOutputTokens;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Returns additional configuration notes or limitations for this model.
     * 
     * @return the configuration notes, or {@code null} if no notes are available
     */
    public String getNotes() {
        return notes;
    }
    
    /**
     * Sets additional configuration notes or limitations for this model.
     * 
     * @param notes the configuration notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
      /**
     * Immutable nested class representing the structured capabilities of an LLM for JSON serialization.
     * This class provides a clean, organized view of model capabilities specifically designed for
     * API responses and client consumption. All capability flags are final and set during construction.
     * 
     * <p>This class is automatically serialized as a nested "capabilities" object in JSON responses,
     * providing clear capability information without exposing internal implementation details.</p>
     * 
     * @since 1.0
     */
    public static class Capabilities {
        /**
         * Indicates text processing capability - immutable after construction.
         */
        private final boolean text;
        
        /**
         * Indicates image processing capability - immutable after construction.
         */
        private final boolean image;
        
        /**
         * Indicates PDF processing capability - immutable after construction.
         */
        private final boolean pdf;

        /**
         * Indicates function calling capability - immutable after construction.
         */
        private final boolean functionCalling;

        /**
         * Indicates JSON mode capability - immutable after construction.
         */
        private final boolean jsonMode;
        
        /**
         * Constructs a new Capabilities instance with the specified capability flags.
         * All parameters are stored as immutable final fields.
         * 
         * @param text whether text processing is supported
         * @param image whether image processing is supported
         * @param pdf whether PDF processing is supported
         * @param functionCalling whether function calling is supported
         * @param jsonMode whether JSON mode is supported
         */
        public Capabilities(boolean text, boolean image, boolean pdf, boolean functionCalling, boolean jsonMode) {
            this.text = text;
            this.image = image;
            this.pdf = pdf;
            this.functionCalling = functionCalling;
            this.jsonMode = jsonMode;
        }
        
        /**
         * Returns whether text processing is supported.
         * 
         * @return {@code true} if text input is supported
         */
        public boolean isText() {
            return text;
        }
        
        /**
         * Returns whether image processing is supported.
         * 
         * @return {@code true} if image input is supported
         */
        public boolean isImage() {
            return image;
        }
        
        /**
         * Returns whether PDF processing is supported.
         * 
         * @return {@code true} if PDF input is supported
         */
        public boolean isPdf() {
            return pdf;
        }

        public boolean isFunctionCalling() {
            return functionCalling;
        }

        public boolean isJsonMode() {
            return jsonMode;
        }
    }
      /**
     * Builder class implementing the Builder pattern for constructing LlmConfiguration instances.
     * Provides a fluent interface for setting configuration parameters with sensible defaults
     * and optional parameter handling.
     * 
     * <h3>Default Values:</h3>
     * <ul>
     *   <li>Text support: {@code true} (enabled by default)</li>
     *   <li>Image support: {@code false} (disabled by default)</li>
     *   <li>PDF support: {@code false} (disabled by default)</li>
     *   <li>Function Calling support: {@code false} (disabled by default)</li>
     *   <li>JSON Mode support: {@code false} (disabled by default)</li>
     *   <li>Token limits: {@code 0}</li>
     *   <li>Pricing Tier: {@code null}</li>
     *   <li>Description: {@code null}</li>
     *   <li>ID and name: {@code null} (must be explicitly set)</li>
     *   <li>Notes: {@code null} (optional)</li>
     * </ul>
     * 
     * <p><strong>Usage Pattern:</strong></p>
     * <pre>
     * LlmConfiguration config = LlmConfiguration.builder()
     *     .id("model-id")
     *     .name("Model Name")
     *     .supportsImage(true)
     *     .notes("Additional info")
     *     .build();
     * </pre>
     * 
     * @since 1.0
     */
    public static class LlmConfigurationBuilder {
        private String id;
        private String name;
        private boolean supportsText = true;
        private boolean supportsImage = false;
        private boolean supportsPdf = false;
        private boolean supportsFunctionCalling = false;
        private boolean supportsJsonMode = false;        private int maxContextTokens = 0;
        private int maxOutputTokens = 0;
        private String description;
        private String notes;
        
        /**
         * Package-private constructor to enforce usage through the static builder() method.
         */
        LlmConfigurationBuilder() {
        }
        
        /**
         * Sets the unique identifier for the LLM configuration.
         * 
         * @param id the model ID (should be non-null and URL-safe)
         * @return this builder instance for method chaining
         */
        public LlmConfigurationBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the human-readable display name for the LLM.
         * 
         * @param name the display name for the model
         * @return this builder instance for method chaining
         */
        public LlmConfigurationBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets whether the model supports text input processing.
         * 
         * @param supportsText {@code true} to enable text support
         * @return this builder instance for method chaining
         */
        public LlmConfigurationBuilder supportsText(boolean supportsText) {
            this.supportsText = supportsText;
            return this;
        }
        
        /**
         * Sets whether the model supports image input and vision capabilities.
         * 
         * @param supportsImage {@code true} to enable image support
         * @return this builder instance for method chaining
         */
        public LlmConfigurationBuilder supportsImage(boolean supportsImage) {
            this.supportsImage = supportsImage;
            return this;
        }
        
        /**
         * Sets whether the model supports PDF document processing.
         * 
         * @param supportsPdf {@code true} to enable PDF support
         * @return this builder instance for method chaining
         */
        public LlmConfigurationBuilder supportsPdf(boolean supportsPdf) {
            this.supportsPdf = supportsPdf;
            return this;
        }

        public LlmConfigurationBuilder supportsFunctionCalling(boolean supportsFunctionCalling) {
            this.supportsFunctionCalling = supportsFunctionCalling;
            return this;
        }

        public LlmConfigurationBuilder supportsJsonMode(boolean supportsJsonMode) {
            this.supportsJsonMode = supportsJsonMode;
            return this;
        }

        public LlmConfigurationBuilder maxContextTokens(int maxContextTokens) {
            this.maxContextTokens = maxContextTokens;
            return this;
        }

        public LlmConfigurationBuilder maxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;        }

        public LlmConfigurationBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets additional configuration notes or limitations for the model.
         * 
         * @param notes the configuration notes (can be null)
         * @return this builder instance for method chaining
         */
        public LlmConfigurationBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        /**
         * Constructs the LlmConfiguration instance with the current builder settings.
         * This method creates a new instance using the all-args constructor with
         * the values set through the builder methods.
         * 
         * @return a new LlmConfiguration instance with the configured values
         */        public LlmConfiguration build() {
            return new LlmConfiguration(id, name, supportsText, supportsImage, supportsPdf, 
                                        supportsFunctionCalling, supportsJsonMode, 
                                        maxContextTokens, maxOutputTokens, description, notes);
        }
    }
}
