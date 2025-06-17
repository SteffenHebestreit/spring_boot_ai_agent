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
 * Unit tests for the simplified OpenAIService model capability detection.
 * Tests the core capabilities (vision and function calling) without additional metadata.
 */
@ExtendWith(MockitoExtension.class)
public class OpenAIServiceSimplifiedTest {

    @Mock
    private OpenAIProperties openAIProperties;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private DynamicIntegrationService dynamicIntegrationService;
    
    @Mock
    private LlmCapabilityService llmCapabilityService;
    
    private OpenAIService openAIService;
    private ObjectMapper objectMapper;

    @BeforeEach
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
        when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        when(openAIProperties.getKey()).thenReturn("test-key");
    }

    @Test
    void testGetAvailableModels_WithConfiguration() {
        // Given
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
        setupWebClientMocks(mockResponse);

        LlmConfiguration llmConfig = new LlmConfiguration();
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
        assertNotNull(gpt4o);
        assertEquals(modelId, gpt4o.getId());
        assertEquals("openai", gpt4o.getOwnedBy());
        assertEquals("GPT-4 Omni (Configured)", gpt4o.getName());
        assertTrue(gpt4o.getVisionEnabled());
        assertTrue(gpt4o.getFunctionCallingEnabled());
        assertTrue(gpt4o.getDescription().contains("Configured via LlmConfiguration."));
        assertTrue(gpt4o.getCapabilities().contains("(Configured)"));
    }

    @Test
    void testGetAvailableModels_FallbackCapabilities() {
        // Given
        String modelId = "unknown-model";
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
        assertNotNull(model);
        assertEquals(modelId, model.getId());
        assertEquals("custom-provider", model.getOwnedBy());
        assertEquals("Unknown Model", model.getName()); // From generateDisplayName
        assertFalse(model.getVisionEnabled()); // Fallback default
        assertTrue(model.getFunctionCallingEnabled()); // Fallback default
        assertTrue(model.getCapabilities().contains("(Fallback)"));
    }

    @Test
    void testGetAvailableModels_ErrorHandling() {
        // Given
        String invalidResponse = "{ invalid json }";
        setupWebClientMocks(invalidResponse);

        // When
        List<ProviderModel> models = openAIService.getAvailableModels();

        // Then
        assertTrue(models.isEmpty(), "Models list should be empty for invalid JSON response.");
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
