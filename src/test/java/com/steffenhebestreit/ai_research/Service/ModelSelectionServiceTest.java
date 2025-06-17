package com.steffenhebestreit.ai_research.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Model.ProviderModel;

/**
 * Unit tests for the ModelSelectionService class.
 */
@ExtendWith(MockitoExtension.class)
public class ModelSelectionServiceTest {

    @Mock
    private OpenAIService openAIService;
    
    @Mock
    private LlmCapabilityService llmCapabilityService;
    
    private ModelSelectionService modelSelectionService;    @BeforeEach
    void setUp() {
        modelSelectionService = new ModelSelectionService();
        
        // Use reflection to set private fields
        try {
            Field openAIServiceField = ModelSelectionService.class.getDeclaredField("openAIService");
            openAIServiceField.setAccessible(true);
            openAIServiceField.set(modelSelectionService, openAIService);
            
            Field llmCapabilityServiceField = ModelSelectionService.class.getDeclaredField("llmCapabilityService");
            llmCapabilityServiceField.setAccessible(true);
            llmCapabilityServiceField.set(modelSelectionService, llmCapabilityService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test dependencies", e);
        }
    }

    @Test
    void testGetModelAnalytics_Success() {
        // Given
        List<ProviderModel> providerModels = createMockProviderModels();
        List<LlmConfiguration> localConfigs = createMockLocalConfigurations();
        
        when(openAIService.getAvailableModels()).thenReturn(providerModels);
        when(llmCapabilityService.getAllLlmConfigurations()).thenReturn(localConfigs);

        // When
        Map<String, Object> analytics = modelSelectionService.getModelAnalytics();

        // Then
        assertNotNull(analytics);
        assertEquals(3, analytics.get("totalProviderModels"));
        assertEquals(2, analytics.get("totalLocalConfigurations"));
        assertEquals(2L, analytics.get("visionEnabledModels"));
        assertEquals(3L, analytics.get("functionCallingModels"));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> ownerDistribution = (Map<String, Long>) analytics.get("ownerDistribution");        assertEquals(2L, ownerDistribution.get("openai"));
        assertEquals(1L, ownerDistribution.get("anthropic"));
        
        assertNotNull(analytics.get("analysisTimestamp"));
    }

    @Test
    void testValidateModelCompatibility_ExistingModel_AllSupported() {
        // Given
        ProviderModel model = createVisionModel();
        when(openAIService.getAvailableModels()).thenReturn(List.of(model));
        when(llmCapabilityService.getLlmConfiguration("gpt-4o")).thenReturn(null);

        // When
        Map<String, Object> result = modelSelectionService.validateModelCompatibility(
            "gpt-4o", "image", List.of("function_calling", "json_mode"));

        // Then
        assertTrue((Boolean) result.get("compatible"));
        assertEquals("gpt-4o", result.get("modelId"));
        assertTrue((Boolean) result.get("contentTypeSupported"));
        
        @SuppressWarnings("unchecked")
        Map<String, Boolean> featureSupport = (Map<String, Boolean>) result.get("featureSupport");
        assertTrue(featureSupport.get("function_calling"));
        assertTrue(featureSupport.get("json_mode"));
    }

    @Test
    void testValidateModelCompatibility_ModelNotFound() {
        // Given
        when(openAIService.getAvailableModels()).thenReturn(List.of());
        when(llmCapabilityService.getLlmConfiguration("nonexistent")).thenReturn(null);

        // When
        Map<String, Object> result = modelSelectionService.validateModelCompatibility(
            "nonexistent", "text", List.of());

        // Then
        assertFalse((Boolean) result.get("compatible"));
        assertEquals("nonexistent", result.get("modelId"));
        assertTrue(result.get("error").toString().contains("Model not found"));
    }

    @Test
    void testValidateModelCompatibility_UnsupportedFeature() {
        // Given
        ProviderModel model = createTextOnlyModel();
        when(openAIService.getAvailableModels()).thenReturn(List.of(model));
        when(llmCapabilityService.getLlmConfiguration("text-model")).thenReturn(null);

        // When
        Map<String, Object> result = modelSelectionService.validateModelCompatibility(
            "text-model", "image", List.of("function_calling"));

        // Then
        assertFalse((Boolean) result.get("compatible"));
        assertFalse((Boolean) result.get("contentTypeSupported")); // No vision support
    }

    @Test
    void testGetUnknownModelsReport_WithInferredModels() {
        // Given
        List<ProviderModel> models = Arrays.asList(
            createKnownModel(),
            createInferredModel() // Uses new ID "custom-instruct-model"
        );
        when(openAIService.getAvailableModels()).thenReturn(models);
        when(llmCapabilityService.getAllLlmConfigurations()).thenReturn(List.of());

        // When
        Map<String, Object> report = modelSelectionService.getUnknownModelsReport();

        // Then
        assertNotNull(report);
        assertEquals(1, report.get("totalUnknown")); // Only the inferred model
        assertEquals(2, report.get("totalModels"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> unknownModels = 
            (List<Map<String, Object>>) report.get("unknownModels");
        assertEquals(1, unknownModels.size());
        assertEquals("custom-instruct-model", unknownModels.get(0).get("id")); // Assert with the new ID
        
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) report.get("configurationSuggestions");
        assertEquals(1, suggestions.size());
        assertTrue(suggestions.get(0).contains("custom-instruct-model")); // Assert with the new ID
    }

    @Test
    void testGetModelConfigurationHelp_ExistingModel() {
        // Given
        ProviderModel model = createInferredModel(); // Uses new ID "custom-instruct-model"
        model.setCapabilities("Text Generation (Inferred)");
        when(openAIService.getAvailableModels()).thenReturn(List.of(model));

        // When
        Map<String, Object> help = modelSelectionService.getModelConfigurationHelp("custom-instruct-model"); // Use the new ID

        // Then
        assertNotNull(help);
        assertEquals("custom-instruct-model", help.get("modelId"));
        assertTrue((Boolean) help.get("isInferred"));
        
        @SuppressWarnings("unchecked")
        List<String> patterns = (List<String>) help.get("detectedPatterns");
        assertFalse(patterns.isEmpty());
        
        @SuppressWarnings("unchecked")
        List<String> steps = (List<String>) help.get("configurationSteps");
        assertFalse(steps.isEmpty());
        
        assertNotNull(help.get("recommendedConfiguration"));
    }

    @Test
    void testGetModelConfigurationHelp_ModelNotFound() {
        // Given
        when(openAIService.getAvailableModels()).thenReturn(List.of());

        // When
        Map<String, Object> help = modelSelectionService.getModelConfigurationHelp("nonexistent");

        // Then
        assertNotNull(help);
        assertFalse((Boolean) help.get("available"));
        assertTrue(help.get("error").toString().contains("Model not found"));
    }

    @Test
    void testAnalyzeModelPatterns_VisionModel() {
        // Given - this would test private method through public interface
        ProviderModel model = new ProviderModel();
        model.setId("custom-vision-32k-instruct");
        model.setCapabilities("Vision, Instruct"); // Added this line
        when(openAIService.getAvailableModels()).thenReturn(List.of(model));

        // When
        Map<String, Object> help = modelSelectionService.getModelConfigurationHelp("custom-vision-32k-instruct");

        // Then
        @SuppressWarnings("unchecked")
        List<String> patterns = (List<String>) help.get("detectedPatterns");
        assertNotNull(patterns);
        assertTrue(patterns.stream().anyMatch(p -> p.contains("Vision capabilities likely")));
        assertTrue(patterns.stream().anyMatch(p -> p.contains("Instruction-following model")));
        assertTrue(patterns.stream().anyMatch(p -> p.contains("Extended context window")));
    }

    @Test
    void testFindSimilarModels() {
        // Given
        List<ProviderModel> models = Arrays.asList(
            createVisionModel(),
            createTextOnlyModel(),
            createAnotherVisionModel()
        );
        when(openAIService.getAvailableModels()).thenReturn(models);

        // When
        Map<String, Object> help = modelSelectionService.getModelConfigurationHelp("gpt-4o");

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> similarModels = (Map<String, Object>) help.get("similarModels");
        assertNotNull(similarModels);
        
        @SuppressWarnings("unchecked")
        List<String> modelsList = (List<String>) similarModels.get("models");
        assertNotNull(modelsList);
        // Should find the other vision model with similar capabilities
        assertTrue(modelsList.stream().anyMatch(m -> m.contains("claude-3-vision")));
    }

    // Helper methods to create mock objects
    private List<ProviderModel> createMockProviderModels() {
        ProviderModel gpt4o = new ProviderModel();
        gpt4o.setId("gpt-4o");        gpt4o.setOwnedBy("openai");
        gpt4o.setVisionEnabled(true);
        gpt4o.setFunctionCallingEnabled(true);

        ProviderModel gpt35 = new ProviderModel();
        gpt35.setId("gpt-3.5-turbo");
        gpt35.setOwnedBy("openai");        gpt35.setVisionEnabled(false);
        gpt35.setFunctionCallingEnabled(true);

        ProviderModel claude = new ProviderModel();
        claude.setId("claude-3-opus");
        claude.setOwnedBy("anthropic");        claude.setVisionEnabled(true);
        claude.setFunctionCallingEnabled(true);

        return Arrays.asList(gpt4o, gpt35, claude);
    }

    private List<LlmConfiguration> createMockLocalConfigurations() {
        LlmConfiguration config1 = new LlmConfiguration();
        config1.setId("gpt-4o");
        config1.setName("GPT-4o Enhanced");

        LlmConfiguration config2 = new LlmConfiguration();
        config2.setId("local-model");
        config2.setName("Local Model");

        return Arrays.asList(config1, config2);
    }

    private ProviderModel createVisionModel() {
        ProviderModel model = new ProviderModel();
        model.setId("gpt-4o");
        model.setName("GPT-4o");
        model.setOwnedBy("openai");        model.setVisionEnabled(true);
        model.setFunctionCallingEnabled(true);
        model.setJsonModeEnabled(true);
        model.setCapabilities("Vision, Function Calling, JSON Mode");
        return model;
    }

    private ProviderModel createTextOnlyModel() {
        ProviderModel model = new ProviderModel();
        model.setId("text-model");
        model.setName("Text Only Model");
        model.setOwnedBy("provider");        model.setVisionEnabled(false);
        model.setFunctionCallingEnabled(false);
        model.setJsonModeEnabled(false);
        model.setCapabilities("Text Generation");
        return model;
    }

    private ProviderModel createKnownModel() {
        ProviderModel model = new ProviderModel();
        model.setId("gpt-4");
        model.setName("GPT-4");
        model.setOwnedBy("openai");        model.setVisionEnabled(false);
        model.setFunctionCallingEnabled(true);
        model.setJsonModeEnabled(true);
        model.setCapabilities("Function Calling, JSON Mode");
        return model;
    }

    private ProviderModel createInferredModel() {
        ProviderModel model = new ProviderModel();
        model.setId("custom-instruct-model"); // Changed ID
        model.setName("Custom Instruct Model"); // Changed name
        model.setOwnedBy("custom-provider");        model.setVisionEnabled(false);
        model.setFunctionCallingEnabled(false);
        model.setJsonModeEnabled(false);
        model.setCapabilities("Text Generation (Inferred)");
        model.setDescription("Language model (capabilities detected using pattern matching)");
        return model;
    }

    private ProviderModel createAnotherVisionModel() {
        ProviderModel model = new ProviderModel();
        model.setId("claude-3-vision");
        model.setName("Claude 3 Vision");
        model.setOwnedBy("anthropic");        model.setVisionEnabled(true);
        model.setFunctionCallingEnabled(true);
        model.setJsonModeEnabled(true);
        model.setCapabilities("Vision, Function Calling, JSON Mode");
        return model;
    }
}
