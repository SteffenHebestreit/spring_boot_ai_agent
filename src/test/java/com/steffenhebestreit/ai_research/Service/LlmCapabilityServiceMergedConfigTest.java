package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Configuration.LlmConfigProperties;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Model.ProviderModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmCapabilityServiceMergedConfigTest {

    @Mock
    private LlmConfigProperties llmConfigProperties;

    @Mock
    private OpenAIService openAIService;

    @InjectMocks
    private LlmCapabilityService llmCapabilityService;

    private LlmConfiguration localConfig;
    private ProviderModel providerModel;

    @BeforeEach
    void setUp() {
        // Setup local configuration
        localConfig = new LlmConfiguration();
        localConfig.setId("test-model");
        localConfig.setName("Local Test Model");
        localConfig.setDescription("Local description");
        localConfig.setSupportsText(true);
        localConfig.setSupportsImage(false);
        localConfig.setSupportsPdf(true);
        localConfig.setMaxContextTokens(2048);

        // Setup provider model
        providerModel = new ProviderModel();
        providerModel.setId("provider-model");
        providerModel.setName("Provider Test Model");
        providerModel.setDescription("Provider description");
        providerModel.setMaxContextTokens(4096);
        providerModel.setVisionEnabled(true);
        providerModel.setJsonModeEnabled(true);
        providerModel.setFunctionCallingEnabled(true);
    }

    @Test
    void testGetMergedLlmConfigurations_WithLocalAndProviderModels() {
        // Arrange
        when(llmConfigProperties.getConfigurations()).thenReturn(Arrays.asList(localConfig));
        when(openAIService.getAvailableModels()).thenReturn(Arrays.asList(providerModel));

        // Act
        List<LlmConfiguration> result = llmCapabilityService.getMergedLlmConfigurations();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // One local, one provider
        
        // Check that both models are present
        assertTrue(result.stream().anyMatch(config -> "test-model".equals(config.getId())));
        assertTrue(result.stream().anyMatch(config -> "provider-model".equals(config.getId())));
    }

    @Test
    void testGetMergedLlmConfigurations_ProviderOverridesLocal() {
        // Arrange - same ID for both local and provider
        localConfig.setId("same-model");
        providerModel.setId("same-model");
        
        when(llmConfigProperties.getConfigurations()).thenReturn(Arrays.asList(localConfig));
        when(openAIService.getAvailableModels()).thenReturn(Arrays.asList(providerModel));

        // Act
        List<LlmConfiguration> result = llmCapabilityService.getMergedLlmConfigurations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size()); // Merged into one
        
        LlmConfiguration merged = result.get(0);
        assertEquals("same-model", merged.getId());
        // Should have local PDF support OR'd with provider capabilities
        assertTrue(merged.isSupportsPdf()); // Local had true
        assertTrue(merged.isSupportsImage()); // Provider had true
    }

    @Test
    void testGetMergedLlmConfigurations_FallbackToLocalOnProviderError() {
        // Arrange
        when(llmConfigProperties.getConfigurations()).thenReturn(Arrays.asList(localConfig));
        when(openAIService.getAvailableModels()).thenThrow(new RuntimeException("Provider unavailable"));

        // Act
        List<LlmConfiguration> result = llmCapabilityService.getMergedLlmConfigurations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size()); // Only local config
        assertEquals("test-model", result.get(0).getId());
    }

    @Test
    void testGetLlmConfigurationMerged_FindsLocalFirst() {
        // Arrange
        when(llmConfigProperties.findById("test-model")).thenReturn(localConfig);

        // Act
        LlmConfiguration result = llmCapabilityService.getLlmConfigurationMerged("test-model");

        // Assert
        assertNotNull(result);
        assertEquals("test-model", result.getId());
        assertEquals("Local Test Model", result.getName());
        
        // Should not call provider service if found locally
        verify(openAIService, never()).getAvailableModels();
    }

    @Test
    void testGetLlmConfigurationMerged_FallsBackToProvider() {
        // Arrange
        when(llmConfigProperties.findById("provider-model")).thenReturn(null);
        when(openAIService.getAvailableModels()).thenReturn(Arrays.asList(providerModel));

        // Act
        LlmConfiguration result = llmCapabilityService.getLlmConfigurationMerged("provider-model");

        // Assert
        assertNotNull(result);
        assertEquals("provider-model", result.getId());
        assertEquals("Provider Test Model", result.getName());
        assertTrue(result.isSupportsImage()); // From provider model
    }

    @Test
    void testGetLlmConfigurationMerged_ReturnsNullWhenNotFound() {
        // Arrange
        when(llmConfigProperties.findById("nonexistent")).thenReturn(null);
        when(openAIService.getAvailableModels()).thenReturn(Arrays.asList());

        // Act
        LlmConfiguration result = llmCapabilityService.getLlmConfigurationMerged("nonexistent");

        // Assert
        assertNull(result);
    }
}
