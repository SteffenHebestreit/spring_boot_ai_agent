package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Service.LlmCapabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Large Language Model (LLM) capability management and discovery.
 * 
 * <p>This controller provides endpoints for discovering LLM configurations, capabilities,
 * and data type support. It enables dynamic frontend adaptation based on available
 * LLM features and allows clients to verify compatibility before attempting operations.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>LLM Discovery:</strong> Retrieves all configured LLM models and their capabilities</li>
 * <li><strong>Default Configuration:</strong> Provides access to the default LLM configuration</li>
 * <li><strong>Capability Checking:</strong> Validates LLM support for specific data types</li>
 * <li><strong>Dynamic UI Support:</strong> Enables frontend adaptation based on LLM features</li>
 * </ul>
 * 
 * <h3>LLM Capability Management:</h3>
 * <ul>
 * <li><strong>Configuration Retrieval:</strong> Access to complete LLM configurations</li>
 * <li><strong>Data Type Support:</strong> Verification of text, image, PDF, and other format support</li>
 * <li><strong>Model Information:</strong> Names, descriptions, and usage notes for each LLM</li>
 * <li><strong>Default Selection:</strong> Automatic fallback to configured default model</li>
 * </ul>
 * 
 * <h3>API Endpoints:</h3>
 * <ul>
 * <li><code>GET /capabilities</code> - Lists all available LLM configurations</li>
 * <li><code>GET /default</code> - Retrieves the default LLM configuration</li>
 * <li><code>GET /{llmId}/supports/{dataType}</code> - Checks data type support</li>
 * </ul>
 * 
 * <h3>Frontend Integration:</h3>
 * <p>Enables frontend applications to dynamically adjust their user interfaces based on
 * available LLM capabilities, such as showing/hiding file upload options or adjusting
 * conversation features based on model-specific limitations.</p>
 * 
 * <h3>Configuration Management:</h3>
 * <p>Works with LlmCapabilityService to manage LLM configurations defined in application
 * properties, providing a centralized way to configure and discover model capabilities.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see LlmCapabilityService
 * @see LlmConfiguration
 */
@RestController
@RequestMapping("/research-agent/api/llms")
public class LlmController {
    
    @Autowired
    private LlmCapabilityService llmCapabilityService;
      /**
     * Retrieves comprehensive list of all configured LLM capabilities and configurations.
     * 
     * <p>Returns complete information about all available Large Language Models including
     * their capabilities, supported data types, configuration parameters, and usage notes.
     * This endpoint enables frontend applications to dynamically adapt their interfaces
     * based on available model features.</p>
     * 
     * <h3>Response Content:</h3>
     * <ul>
     * <li><strong>Model Information:</strong> Name, ID, description, and provider details</li>
     * <li><strong>Capability Matrix:</strong> Supported data types (text, image, PDF, etc.)</li>
     * <li><strong>Configuration Parameters:</strong> Model-specific settings and limitations</li>
     * <li><strong>Usage Notes:</strong> Important information about model behavior and restrictions</li>
     * </ul>
     * 
     * <h3>Frontend Integration:</h3>
     * <p>Frontend applications can use this data to:</p>
     * <ul>
     * <li>Show/hide file upload options based on multimodal support</li>
     * <li>Display model selection dropdowns with capability information</li>
     * <li>Validate user inputs against model limitations</li>
     * <li>Provide contextual help about model-specific features</li>
     * </ul>
     * 
     * <h3>Response Format:</h3>
     * <p>Returns JSON array of LlmConfiguration objects containing complete
     * capability information for each configured model.</p>
     * 
     * @return ResponseEntity containing list of LLM configurations with capability details
     * @see LlmConfiguration
     * @see LlmCapabilityService#getAllLlmConfigurations()
     */
    @GetMapping("/capabilities")
    public ResponseEntity<List<LlmConfiguration>> getLlmCapabilities() {
        List<LlmConfiguration> configurations = llmCapabilityService.getAllLlmConfigurations();
        return ResponseEntity.ok(configurations);
    }
      /**
     * Retrieves the default LLM configuration as specified in application properties.
     * 
     * <p>Returns the default Large Language Model configuration that serves as the
     * fallback option when no specific model is requested. The default model is
     * configured via the openai.api.model property and provides a reliable baseline
     * for system operations.</p>
     * 
     * <h3>Configuration Source:</h3>
     * <p>The default LLM is determined by the <code>openai.api.model</code> property
     * in application configuration, ensuring consistent behavior across the system.</p>
     * 
     * <h3>Response Scenarios:</h3>
     * <ul>
     * <li><strong>Success:</strong> Returns complete LlmConfiguration object for default model</li>
     * <li><strong>No Default:</strong> Returns error object if no default LLM is configured</li>
     * </ul>
     * 
     * <h3>Error Handling:</h3>
     * <p>When no default LLM is configured, returns a JSON object with error message
     * instead of throwing an exception, allowing graceful handling by frontend applications.</p>
     * 
     * <h3>Usage:</h3>
     * <p>Used by frontend applications for initial model selection and as fallback
     * when user preferences are not available or invalid.</p>
     * 
     * @return ResponseEntity containing default LLM configuration or error message
     * @see LlmCapabilityService#getDefaultLlmConfiguration()
     */
    @GetMapping("/default")
    public ResponseEntity<Object> getDefaultLlm() {
        LlmConfiguration defaultConfig = llmCapabilityService.getDefaultLlmConfiguration();
        if (defaultConfig == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No default LLM configured");
            return ResponseEntity.ok(error);
        }
        return ResponseEntity.ok(defaultConfig);
    }
      /**
     * Validates whether a specific LLM supports a given data type.
     * 
     * <p>Performs capability checking to determine if a specific Large Language Model
     * can process a particular data type. This validation prevents upload attempts
     * and API calls with unsupported content types, improving user experience and
     * reducing unnecessary API costs.</p>
     * 
     * <h3>Supported Data Types:</h3>
     * <ul>
     * <li><code>text</code> - Plain text and markdown content</li>
     * <li><code>image</code> - Image files (PNG, JPEG, WebP, etc.)</li>
     * <li><code>pdf</code> - PDF documents</li>
     * <li><code>audio</code> - Audio files for transcription</li>
     * <li><code>video</code> - Video content analysis</li>
     * </ul>
     * 
     * <h3>Response Information:</h3>
     * <ul>
     * <li><strong>Support Status:</strong> Boolean flag indicating capability</li>
     * <li><strong>Model Details:</strong> LLM name and identification</li>
     * <li><strong>Usage Notes:</strong> Important limitations or requirements</li>
     * <li><strong>Context Information:</strong> Request parameters for reference</li>
     * </ul>
     * 
     * <h3>Frontend Usage:</h3>
     * <p>Frontend applications should check support before:</p>
     * <ul>
     * <li>Enabling file upload controls</li>
     * <li>Processing multimodal content</li>
     * <li>Displaying capability-dependent features</li>
     * <li>Validating user input types</li>
     * </ul>
     * 
     * <h3>Response Format:</h3>
     * <p>Returns JSON object containing support status, model information,
     * and contextual details for comprehensive capability assessment.</p>
     * 
     * @param llmId The unique identifier of the LLM to check capabilities for
     * @param dataType The data type to validate (text, image, pdf, audio, video)
     * @return ResponseEntity containing detailed support information and model context
     * @see LlmCapabilityService#supportsDataType(String, String)
     * @see LlmCapabilityService#getLlmConfiguration(String)
     */
    @GetMapping("/{llmId}/supports/{dataType}")
    public ResponseEntity<Map<String, Object>> checkDataTypeSupport(
            @PathVariable String llmId,
            @PathVariable String dataType) {
        
        boolean supported = llmCapabilityService.supportsDataType(llmId, dataType);
        Map<String, Object> response = new HashMap<>();
        response.put("llmId", llmId);
        response.put("dataType", dataType);
        response.put("supported", supported);
        
        LlmConfiguration config = llmCapabilityService.getLlmConfiguration(llmId);
        if (config != null) {
            response.put("llmName", config.getName());
            response.put("notes", config.getNotes());
        }
        
        return ResponseEntity.ok(response);
    }
}
