package com.steffenhebestreit.ai_research.Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class MultimodalContentServiceGemmaTest {

    @Mock
    private LlmCapabilityService llmCapabilityService;

    @InjectMocks
    private MultimodalContentService multimodalContentService;

    @Test
    void testValidateFile_GemmaModelSupportsImages() {
        // Arrange
        String gemmaModelId = "google/gemma-3-27b";
        MockMultipartFile imageFile = new MockMultipartFile(
            "test", 
            "test.jpg", 
            "image/jpeg", 
            "fake image content".getBytes()
        );

        // Mock that the model is not found in local/provider configs (fallback scenario)
        when(llmCapabilityService.getLlmConfigurationMerged(gemmaModelId)).thenReturn(null);

        // Act
        Map<String, Object> result = multimodalContentService.validateFile(imageFile, gemmaModelId);

        // Assert
        assertNotNull(result);
        assertTrue((Boolean) result.get("valid"), "Gemma model should be allowed for image inputs");
        assertTrue((Boolean) result.get("modelLoading"), "Should indicate model is loading");
        
        String message = (String) result.get("message");
        assertNotNull(message);
        assertTrue(message.contains("appears to be initializing"), "Should have initialization message");
    }

    @Test
    void testIsLikelyVisionCapableModel_DetectsGemma() throws Exception {
        // Use reflection to test the private method
        java.lang.reflect.Method method = MultimodalContentService.class
            .getDeclaredMethod("isLikelyVisionCapableModel", String.class);
        method.setAccessible(true);        // Test various Gemma model IDs including problematic format from error message
        String[] gemmaModels = {
            "google/gemma-3-27b",
            "Google/gemma 3 27b",    // The exact format from the error message
            "gemma-2b-it",
            "gemma-7b-it", 
            "gemma-3-2b",
            "Gemma-3-27B",          // Test case insensitive
            "google/gemma3-27b",     // Without hyphen
            "Google Gemma 3 27B"     // With spaces and capitals
        };

        for (String modelId : gemmaModels) {
            Boolean result = (Boolean) method.invoke(multimodalContentService, modelId);
            assertTrue(result, "Model " + modelId + " should be detected as vision-capable");
        }
    }

    @Test
    void testIsLikelyVisionCapableModel_RejectsNonVisionModels() throws Exception {
        // Use reflection to test the private method
        java.lang.reflect.Method method = MultimodalContentService.class
            .getDeclaredMethod("isLikelyVisionCapableModel", String.class);
        method.setAccessible(true);

        // Test non-vision models
        String[] nonVisionModels = {
            "text-davinci-003",
            "gpt-3.5-turbo",
            "llama-2-7b",
            "mistral-7b"
        };

        for (String modelId : nonVisionModels) {
            Boolean result = (Boolean) method.invoke(multimodalContentService, modelId);
            assertFalse(result, "Model " + modelId + " should NOT be detected as vision-capable");
        }
    }
}
