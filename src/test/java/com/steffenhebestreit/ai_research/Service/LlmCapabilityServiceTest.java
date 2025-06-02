package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Configuration.LlmConfigProperties;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LlmCapabilityServiceTest {

    @Mock
    private LlmConfigProperties llmConfigProperties;

    @InjectMocks
    private LlmCapabilityService llmCapabilityService;

    private LlmConfiguration textOnlyModel;
    private LlmConfiguration visionModel;
    private LlmConfiguration fullCapabilityModel;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test models
        textOnlyModel = LlmConfiguration.builder()
            .id("gpt-3.5-turbo")
            .name("GPT-3.5 Turbo")
            .supportsText(true)
            .supportsImage(false)
            .supportsPdf(false)
            .build();
            
        visionModel = LlmConfiguration.builder()
            .id("gpt-4-vision")
            .name("GPT-4 Vision")
            .supportsText(true)
            .supportsImage(true)
            .supportsPdf(false)
            .build();
            
        fullCapabilityModel = LlmConfiguration.builder()
            .id("claude-3-opus")
            .name("Claude 3 Opus")
            .supportsText(true)
            .supportsImage(true)
            .supportsPdf(true)
            .build();
            
        ReflectionTestUtils.setField(llmCapabilityService, "defaultModelId", "gpt-3.5-turbo");
    }

    @Test
    void getAllLlmConfigurations_ShouldReturnAllConfigurations() {
        // Given
        List<LlmConfiguration> configs = Arrays.asList(textOnlyModel, visionModel, fullCapabilityModel);
        when(llmConfigProperties.getConfigurations()).thenReturn(configs);
        
        // When
        List<LlmConfiguration> result = llmCapabilityService.getAllLlmConfigurations();
        
        // Then
        assertEquals(3, result.size());
        verify(llmConfigProperties).getConfigurations();
    }

    @Test
    void getLlmConfiguration_ShouldReturnCorrectConfiguration() {
        // Given
        when(llmConfigProperties.findById("gpt-4-vision")).thenReturn(visionModel);
        
        // When
        LlmConfiguration result = llmCapabilityService.getLlmConfiguration("gpt-4-vision");
        
        // Then
        assertNotNull(result);
        assertEquals("gpt-4-vision", result.getId());
        assertEquals("GPT-4 Vision", result.getName());
        assertTrue(result.isSupportsImage());
        assertFalse(result.isSupportsPdf());
        verify(llmConfigProperties).findById("gpt-4-vision");
    }

    @Test
    void supportsDataType_WithImageCapableModel_ShouldReturnTrue() {
        // Given
        when(llmConfigProperties.findById("gpt-4-vision")).thenReturn(visionModel);
        
        // When
        boolean result = llmCapabilityService.supportsDataType("gpt-4-vision", "image");
        
        // Then
        assertTrue(result);
    }    @Test
    void supportsDataType_WithIncompatibleModel_ShouldReturnFalse() {
        // Given
        when(llmConfigProperties.findById("gpt-3.5-turbo")).thenReturn(textOnlyModel);
        
        // When
        boolean result = llmCapabilityService.supportsDataType("gpt-3.5-turbo", "image");
        
        // Then
        assertFalse(result);
    }    @Test
    void defaultModelId_ShouldReturnConfiguredDefault() {
        // This test verifies that the default model ID is set
        assertNotNull(ReflectionTestUtils.getField(llmCapabilityService, "defaultModelId"));
        assertEquals("gpt-3.5-turbo", ReflectionTestUtils.getField(llmCapabilityService, "defaultModelId"));
    }

    @Test
    void getDefaultLlmConfiguration_ShouldReturnDefaultLlmConfig() {
        // Given
        when(llmConfigProperties.findById("gpt-3.5-turbo")).thenReturn(textOnlyModel);
        
        // When
        LlmConfiguration config = llmCapabilityService.getDefaultLlmConfiguration();
        
        // Then
        assertNotNull(config);
        assertEquals("gpt-3.5-turbo", config.getId());
    }
}
