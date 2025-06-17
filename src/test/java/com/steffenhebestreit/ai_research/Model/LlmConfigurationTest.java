package com.steffenhebestreit.ai_research.Model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LlmConfiguration model class
 */
public class LlmConfigurationTest {    @Test
    void defaultConstructor_ShouldSetDefaultValues() {
        // When
        LlmConfiguration config = new LlmConfiguration();
        
        // Then
        assertNull(config.getId());
        assertNull(config.getName());
        assertTrue(config.isSupportsText(), "Text support should be true by default");
        assertFalse(config.isSupportsImage(), "Image support should be false by default");
        assertFalse(config.isSupportsPdf(), "PDF support should be false by default");
        assertFalse(config.isSupportsFunctionCalling(), "Function calling support should be false by default");        assertFalse(config.isSupportsJsonMode(), "JSON mode support should be false by default");
        assertEquals(0, config.getMaxContextTokens(), "Max context tokens should be 0 by default");
        assertEquals(0, config.getMaxOutputTokens(), "Max output tokens should be 0 by default");
        assertNull(config.getDescription());
        assertNull(config.getNotes());
    }    @Test
    void parameterizedConstructor_ShouldInitializeAllFields() {
        // Given
        String id = "gpt-4-vision";
        String name = "GPT-4 Vision";
        boolean supportsText = true;
        boolean supportsImage = true;
        boolean supportsPdf = false;
        boolean supportsFunctionCalling = true;
        boolean supportsJsonMode = false;
        int maxContextTokens = 0;
        int maxOutputTokens = 0;
        String description = null;
        String notes = "Max image size: 20MB";
        
        // When
        LlmConfiguration config = new LlmConfiguration(id, name, supportsText, supportsImage, supportsPdf, 
                                                      supportsFunctionCalling, supportsJsonMode,
                                                      maxContextTokens, maxOutputTokens, description, notes);
        
        // Then
        assertEquals(id, config.getId());
        assertEquals(name, config.getName());
        assertEquals(supportsText, config.isSupportsText());
        assertEquals(supportsImage, config.isSupportsImage());
        assertEquals(supportsPdf, config.isSupportsPdf());
        assertEquals(supportsFunctionCalling, config.isSupportsFunctionCalling());
        assertEquals(supportsJsonMode, config.isSupportsJsonMode());
        assertEquals(maxContextTokens, config.getMaxContextTokens());
        assertEquals(maxOutputTokens, config.getMaxOutputTokens());
        assertEquals(description, config.getDescription());
        assertEquals(notes, config.getNotes());
    }@Test
    void builder_ShouldCreateInstanceWithCorrectValues() {
        // Given
        String id = "claude-3-opus";
        String name = "Claude 3 Opus";
          // When
        LlmConfiguration config = LlmConfiguration.builder()
            .id(id)
            .name(name)
            .supportsImage(true)
            .supportsPdf(true)
            .supportsFunctionCalling(true)
            .supportsJsonMode(true)
            .maxContextTokens(200000)
            .maxOutputTokens(4096)
            .description("Anthropic's most powerful model")
            .notes("Handles both images and PDFs")
            .build();
        
        // Then
        assertEquals(id, config.getId());
        assertEquals(name, config.getName());
        assertTrue(config.isSupportsText());
        assertTrue(config.isSupportsImage());
        assertTrue(config.isSupportsPdf());
        assertTrue(config.isSupportsFunctionCalling());
        assertTrue(config.isSupportsJsonMode());
        assertEquals(200000, config.getMaxContextTokens());
        assertEquals(4096, config.getMaxOutputTokens());
        assertEquals("Anthropic's most powerful model", config.getDescription());
        assertEquals("Handles both images and PDFs", config.getNotes());
    }@Test
    void getCapabilities_ShouldReturnCorrectCapabilitiesObject() {
        // Given
        LlmConfiguration config = new LlmConfiguration();
        config.setSupportsText(true);
        config.setSupportsImage(true);
        config.setSupportsPdf(false);
        config.setSupportsFunctionCalling(true);
        config.setSupportsJsonMode(false);
        
        // When
        LlmConfiguration.Capabilities capabilities = config.getCapabilities();
        
        // Then
        assertTrue(capabilities.isText());
        assertTrue(capabilities.isImage());
        assertFalse(capabilities.isPdf());
        assertTrue(capabilities.isFunctionCalling());
        assertFalse(capabilities.isJsonMode());
    }    @Test
    void setters_ShouldUpdateFields() {
        // Given
        LlmConfiguration config = new LlmConfiguration();
        
        // When
        config.setId("gpt-4o");
        config.setName("GPT-4o");
        config.setSupportsText(true);
        config.setSupportsImage(true);
        config.setSupportsPdf(true);
        config.setSupportsFunctionCalling(true);        config.setSupportsJsonMode(true);
        config.setMaxContextTokens(128000);
        config.setMaxOutputTokens(4096);
        config.setDescription("Latest GPT-4 model");
        config.setNotes("Latest model with all capabilities");
        
        // Then
        assertEquals("gpt-4o", config.getId());
        assertEquals("GPT-4o", config.getName());
        assertTrue(config.isSupportsText());
        assertTrue(config.isSupportsImage());
        assertTrue(config.isSupportsPdf());
        assertTrue(config.isSupportsFunctionCalling());
        assertTrue(config.isSupportsJsonMode());
        assertEquals(128000, config.getMaxContextTokens());
        assertEquals(4096, config.getMaxOutputTokens());
        assertEquals("Latest GPT-4 model", config.getDescription());
        assertEquals("Latest model with all capabilities", config.getNotes());
    }
}
