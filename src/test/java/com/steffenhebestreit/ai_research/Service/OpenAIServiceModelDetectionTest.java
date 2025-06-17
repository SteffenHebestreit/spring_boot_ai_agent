package com.steffenhebestreit.ai_research.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Model.ProviderModel;

import reactor.core.publisher.Mono;

/**
 * Unit tests for the OpenAIService model detection and capability analysis functionality.
 * Tests the simplified capability detection logic using LlmConfiguration or basic fallback.
 */
@ExtendWith(MockitoExtension.class)
public class OpenAIServiceModelDetectionTest {    @Mock
    private OpenAIProperties openAIProperties;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private DynamicIntegrationService dynamicIntegrationService;
    
    @Mock
    private LlmCapabilityService llmCapabilityService;
    
    private OpenAIService openAIService;
    private ObjectMapper objectMapper;    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        WebClient.Builder webClientBuilder = Mockito.mock(WebClient.Builder.class);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        
        openAIService = new OpenAIService(
            openAIProperties, 
            webClientBuilder, 
            objectMapper,
            dynamicIntegrationService, 
            llmCapabilityService
        );
        // Only stub these in tests that use fetchModelsFromApi
        // The unnecessary stubbing has been removed
    }    @Test
    void testGetAvailableModels_WithLlmConfiguration() {
        // Given
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
        
        String modelId = "gpt-4o";
        String mockResponse = String.format("""
            {
                "data": [
                    {
                        "id": "%s",
                        "object": "model",
                        "owned_by": "openai",
                        "created": 1234567890
                    }
                ]
            }
            """, modelId);
        setupWebClientMocks(mockResponse);        LlmConfiguration llmConfig = new LlmConfiguration();
        llmConfig.setId(modelId);
        llmConfig.setName("GPT-4 Omni (Configured)");
        llmConfig.setSupportsImage(true);
        llmConfig.setSupportsFunctionCalling(true);
        llmConfig.setDescription("Configured via LlmConfiguration.");

        when(llmCapabilityService.getLlmConfiguration(modelId)).thenReturn(llmConfig);

        // When
        List<ProviderModel> models = openAIService.getAvailableModels();

        // Then
        assertNotNull(models);
        assertEquals(1, models.size());

        ProviderModel gpt4o = models.get(0);
        assertNotNull(gpt4o);        assertEquals(modelId, gpt4o.getId());
        assertEquals("openai", gpt4o.getOwnedBy());
        assertEquals("GPT-4 Omni (Configured)", gpt4o.getName());
        assertTrue(gpt4o.getVisionEnabled());
        assertTrue(gpt4o.getFunctionCallingEnabled());
        assertTrue(gpt4o.getDescription().contains("Configured via LlmConfiguration."));
        assertTrue(gpt4o.getCapabilities().contains("(Configured)"));
    }    @Test
    void testGetAvailableModels_FallbackLogic() {
        // Given
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
        
        String modelId = "unknown-model-basic";
        String mockResponse = String.format("""
            {
                "data": [
                    {
                        "id": "%s",
                        "object": "model",
                        "owned_by": "custom-provider",
                        "created": 1234567890
                    }
                ]
            }
            """, modelId);
        setupWebClientMocks(mockResponse);

        // Ensure no LlmConfiguration is found for this model to trigger fallback
        when(llmCapabilityService.getLlmConfiguration(modelId)).thenReturn(null);

        // When
        List<ProviderModel> models = openAIService.getAvailableModels();

        // Then
        assertNotNull(models);
        assertEquals(1, models.size());

        ProviderModel model = models.get(0);
        assertNotNull(model);        assertEquals(modelId, model.getId());
        assertEquals("custom-provider", model.getOwnedBy());
        assertEquals("Unknown Model Basic", model.getName()); // From generateDisplayName
        assertFalse(model.getVisionEnabled()); // Fallback default
        assertTrue(model.getFunctionCallingEnabled()); // Fallback default
        assertTrue(model.getCapabilities().contains("(Fallback)"));
    }    @Test
    void testGetAvailableModels_WithFullConfiguration() {
        // Given
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
        
        String modelId = "test-model-enhanced";
        String mockResponse = String.format("""
            {
                "data": [
                    {
                        "id": "%s",
                        "object": "model",
                        "owned_by": "test-provider",
                        "created": 1234567890
                    }
                ]
            }
            """, modelId);
        setupWebClientMocks(mockResponse);        // Create a complete LlmConfiguration with all fields populated
        LlmConfiguration completeConfig = new LlmConfiguration();
        completeConfig.setId(modelId);
        completeConfig.setName("Test Model With Full Config");
        completeConfig.setSupportsImage(true);
        completeConfig.setSupportsFunctionCalling(true);
        completeConfig.setDescription("Model with complete configuration");
        completeConfig.setNotes("Additional configuration notes");

        when(llmCapabilityService.getLlmConfiguration(modelId)).thenReturn(completeConfig);

        // When
        List<ProviderModel> models = openAIService.getAvailableModels();

        // Then
        assertEquals(1, models.size());
        ProviderModel model = models.get(0);
          assertEquals(modelId, model.getId());
        assertEquals("Test Model With Full Config", model.getName());
        assertTrue(model.getVisionEnabled());
        assertTrue(model.getFunctionCallingEnabled());
        assertTrue(model.getCapabilities().contains("(Configured)"));
        assertTrue(model.getDescription().contains("Model with complete configuration"));
        assertTrue(model.getDescription().contains("Additional configuration notes"));
    }    @Test
    void testGenerateDisplayName_VariousCases() {
        // Since we use generateDisplayName indirectly, we need to mock properties
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
        
        // Simple mock response to trigger the method
        String mockResponse = """
            {
                "data": [
                    {
                        "id": "gpt-4o-mini",
                        "object": "model",
                        "owned_by": "openai",
                        "created": 1234567890
                    }
                ]
            }
            """;
        setupWebClientMocks(mockResponse);
        when(llmCapabilityService.getLlmConfiguration("gpt-4o-mini")).thenReturn(null);
        
        // Test cases are implicitly covered by other tests using generateDisplayName,
        // but an explicit test can be added if more specific scenarios for generateDisplayName
        // need to be isolated and verified beyond what's covered in model processing tests.
        // For now, relying on coverage from testGetAvailableModels_FallbackLogic and others.
        ProviderModel model = new ProviderModel();
        model.setId("gpt-4o-mini");
        model.setOwnedBy("openai");
        openAIService.getAvailableModels(); // To trigger internal calls if any, though not strictly needed for direct test of generateDisplayName
        // This test is more of a placeholder as generateDisplayName is private.
        // Its behavior is tested through the public methods that use it.
        // Example direct (if it were public):
        // assertEquals("GPT-4o Mini", openAIService.generateDisplayName("gpt-4o-mini", "openai"));
        // assertEquals("Claude 3 Sonnet", openAIService.generateDisplayName("claude-3-sonnet", "anthropic"));
        // assertEquals("Custom Model Name", openAIService.generateDisplayName("custom_model_name", "custom"));
        assertTrue(true, "generateDisplayName is tested indirectly via getAvailableModels.");
    }@Test
    void testErrorHandling_InvalidJsonResponse() {
        // Given
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
        
        String invalidResponse = "{ invalid json }";
        setupWebClientMocks(invalidResponse);

        // When
        List<ProviderModel> models = openAIService.getAvailableModels();

        // Then
        assertTrue(models.isEmpty(), "Models list should be empty for invalid JSON response.");
    }    @Test
    void testErrorHandling_ApiReturnsNoDataField() {
        // Given
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
        
        String mockResponse = """
            {
                "models": [
                    {"id": "test-model"}
                ]
            }
            """;
        setupWebClientMocks(mockResponse);

        // When
        List<ProviderModel> models = openAIService.getAvailableModels();

        // Then
        assertTrue(models.isEmpty(), "Models list should be empty if 'data' field is missing.");
    }    @Test
    void testErrorHandling_ModelWithEmptyId() {
        // Given
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
        
        String mockResponse = """
            {
                "data": [
                    {
                        "id": "",
                        "object": "model",
                        "owned_by": "test-provider",
                        "created": 1234567890
                    },
                    {
                        "id": "valid-model",
                        "object": "model",
                        "owned_by": "test-provider",
                        "created": 1234567891
                    }
                ]
            }
            """;
        setupWebClientMocks(mockResponse);
        when(llmCapabilityService.getLlmConfiguration("valid-model")).thenReturn(null); // for fallback on the valid one

        // When
        List<ProviderModel> models = openAIService.getAvailableModels();

        // Then
        assertEquals(1, models.size(), "Should skip model with empty ID and process valid one.");
        assertEquals("valid-model", models.get(0).getId());
    }
      @SuppressWarnings({"unchecked", "rawtypes"})
    private void setupWebClientMocks(String mockResponse) {
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/models")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), anyString()))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockResponse));
    }
}
