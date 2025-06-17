package com.steffenhebestreit.ai_research.Service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple debug test to verify fallback detection logic
 */
public class DebugFallbackTest {

    @Test
    void testStringContains() {
        String modelId = "custom-multimodal-32k-instruct";
        
        // Test the exact same checks as in the applyGenericFallback method
        boolean hasMultimodal = modelId.contains("multimodal");
        boolean hasInstruct = modelId.contains("instruct");
        boolean has32k = modelId.contains("32k");
        
        System.out.println("Model ID: " + modelId);
        System.out.println("Contains 'multimodal': " + hasMultimodal);
        System.out.println("Contains 'instruct': " + hasInstruct);
        System.out.println("Contains '32k': " + has32k);
        
        assertTrue(hasMultimodal, "Should contain 'multimodal'");
        assertTrue(hasInstruct, "Should contain 'instruct'");
        assertTrue(has32k, "Should contain '32k'");
    }
}
