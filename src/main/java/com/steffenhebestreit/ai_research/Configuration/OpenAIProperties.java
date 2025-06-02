package com.steffenhebestreit.ai_research.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for OpenAI-compatible API connections in the AI Research system.
 * 
 * <p>This configuration class manages connection settings for OpenAI-compatible Language Learning
 * Model (LLM) APIs, including OpenAI's official API, local LLM servers, and other compatible
 * services. It provides secure credential management, endpoint configuration, and model
 * selection for AI-powered research and conversation capabilities.</p>
 * 
 * <h3>Supported API Types:</h3>
 * <ul>
 * <li><strong>OpenAI Official API:</strong> Direct connection to OpenAI's cloud services</li>
 * <li><strong>Azure OpenAI:</strong> Microsoft Azure-hosted OpenAI models</li>
 * <li><strong>Local LLM Servers:</strong> Self-hosted models using OpenAI-compatible APIs</li>
 * <li><strong>Third-party Services:</strong> Other providers with OpenAI-compatible interfaces</li>
 * </ul>
 * 
 * <h3>Configuration Elements:</h3>
 * <ul>
 * <li><strong>API Endpoint:</strong> Base URL for API communication</li>
 * <li><strong>Authentication:</strong> API key for secure access</li>
 * <li><strong>Model Selection:</strong> Specific model identifier for requests</li>
 * <li><strong>Validation:</strong> Required field validation for proper configuration</li>
 * </ul>
 * 
 * <h3>Security Features:</h3>
 * <ul>
 * <li><strong>Credential Protection:</strong> Secure API key storage and handling</li>
 * <li><strong>Environment Variables:</strong> Support for environment-based configuration</li>
 * <li><strong>Configuration Validation:</strong> Automatic validation of required parameters</li>
 * <li><strong>Runtime Safety:</strong> Prevents incomplete configuration at startup</li>
 * </ul>
 * 
 * <h3>Configuration Examples:</h3>
 * <ul>
 * <li><strong>OpenAI:</strong> baseurl: "https://api.openai.com/v1", model: "gpt-4"</li>
 * <li><strong>Local Server:</strong> baseurl: "http://localhost:1234", model: "llama-3.2"</li>
 * <li><strong>Azure OpenAI:</strong> baseurl: "https://your-resource.openai.azure.com", model: "gpt-4"</li>
 * </ul>
 * 
 * <h3>Model Compatibility:</h3>
 * <p>Supports any model that implements the OpenAI Chat Completions API specification,
 * including streaming responses, message history, and multimodal content processing.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see OpenAIService
 * @see ChatService
 */
@Configuration
@ConfigurationProperties(prefix = "openai.api")
@Validated
public class OpenAIProperties {

    /**
     * Base URL for the OpenAI-compatible API endpoint.
     * 
     * <p>Specifies the root URL for API requests to the OpenAI-compatible service.
     * This URL serves as the base for constructing specific API endpoints like
     * /chat/completions for text generation and other OpenAI API operations.</p>
     * 
     * <h3>URL Requirements:</h3>
     * <ul>
     * <li>Must be a valid HTTP or HTTPS URL</li>
     * <li>Should be accessible from the application's network</li>
     * <li>Must support OpenAI Chat Completions API specification</li>
     * <li>Should use HTTPS for production environments</li>
     * </ul>
     * 
     * <h3>Common Base URLs:</h3>
     * <ul>
     * <li><strong>OpenAI Official:</strong> https://api.openai.com/v1</li>
     * <li><strong>Local Development:</strong> http://localhost:1234</li>
     * <li><strong>Azure OpenAI:</strong> https://{resource}.openai.azure.com</li>
     * <li><strong>Custom Services:</strong> https://your-llm-service.com/v1</li>
     * </ul>
     * 
     * <h3>Endpoint Construction:</h3>
     * <p>The service automatically appends specific paths like "/chat/completions"
     * to this base URL for different API operations.</p>
     */
    @NotBlank
    private String baseurl;

    /**
     * API key for authentication with the OpenAI-compatible service.
     * 
     * <p>Provides the authentication credential required to access the LLM API.
     * This key is included in the Authorization header as a Bearer token for
     * all API requests to authenticate the client and authorize access to
     * the language model services.</p>
     * 
     * <h3>Security Guidelines:</h3>
     * <ul>
     * <li>Store securely using environment variables or secure configuration</li>
     * <li>Never commit API keys to version control systems</li>
     * <li>Use different keys for development, staging, and production</li>
     * <li>Monitor key usage and implement rotation policies</li>
     * <li>Restrict key permissions to minimum required scope</li>
     * </ul>
     * 
     * <h3>Key Sources:</h3>
     * <ul>
     * <li><strong>OpenAI:</strong> Generated from OpenAI platform dashboard</li>
     * <li><strong>Azure OpenAI:</strong> Access keys from Azure portal</li>
     * <li><strong>Local Services:</strong> May not require a key or use custom keys</li>
     * <li><strong>Third-party:</strong> Provider-specific API keys</li>
     * </ul>
     * 
     * <h3>Configuration Example:</h3>
     * <pre>
     * # Environment variable approach (recommended)
     * export OPENAI_API_KEY="sk-..."
     * 
     * # Application properties
     * openai.api.key=${OPENAI_API_KEY}
     * </pre>
     */
    @NotBlank
    private String key;

    /**
     * Model identifier for the specific LLM to use for requests.
     * 
     * <p>Specifies which language model to use when making API requests to the
     * OpenAI-compatible service. The model determines the capabilities, performance
     * characteristics, and cost of the AI interactions.</p>
     * 
     * <h3>Model Selection Criteria:</h3>
     * <ul>
     * <li><strong>Capability Requirements:</strong> Text-only vs. multimodal models</li>
     * <li><strong>Performance Needs:</strong> Response speed vs. quality trade-offs</li>
     * <li><strong>Cost Considerations:</strong> Different models have different pricing</li>
     * <li><strong>Context Length:</strong> Maximum conversation history supported</li>
     * </ul>
     * 
     * <h3>Common Model Identifiers:</h3>
     * <ul>
     * <li><strong>OpenAI GPT-4:</strong> "gpt-4", "gpt-4-turbo", "gpt-4o"</li>
     * <li><strong>OpenAI GPT-3.5:</strong> "gpt-3.5-turbo"</li>
     * <li><strong>Local Models:</strong> "llama-3.2", "mistral-7b", "codellama"</li>
     * <li><strong>Specialized Models:</strong> "gpt-4-vision-preview" for multimodal</li>
     * </ul>
     * 
     * <h3>Model Capabilities:</h3>
     * <p>Different models support different features:</p>
     * <ul>
     * <li><strong>Text Generation:</strong> All models support basic text completion</li>
     * <li><strong>Vision Processing:</strong> Only certain models support image analysis</li>
     * <li><strong>Code Generation:</strong> Some models are optimized for programming tasks</li>
     * <li><strong>Reasoning:</strong> Advanced models provide better logical reasoning</li>
     * </ul>
     */
    @NotBlank
    private String model;

    public String getBaseurl() {
        return baseurl;
    }

    public void setBaseurl(String baseurl) {
        this.baseurl = baseurl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
