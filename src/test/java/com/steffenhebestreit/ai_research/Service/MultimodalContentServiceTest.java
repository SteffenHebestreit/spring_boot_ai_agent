package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MultimodalContentServiceTest {

    @Mock
    private LlmCapabilityService llmCapabilityService;

    @InjectMocks
    private MultimodalContentService multimodalContentService;

    private LlmConfiguration visionModel;
    private LlmConfiguration textOnlyModel;
    private LlmConfiguration fullCapabilityModel;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
          // Set the @Value field values using ReflectionTestUtils
        ReflectionTestUtils.setField(multimodalContentService, "maxImageSize", DataSize.ofMegabytes(5));
        ReflectionTestUtils.setField(multimodalContentService, "maxPdfSize", DataSize.ofMegabytes(10));
        
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
    }

    @Test
    void validateFile_WithImageAndVisionModel_ShouldReturnValid() {
        // Given
        byte[] imageContent = new byte[1024]; // 1KB sample image
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", imageContent
        );
        
        when(llmCapabilityService.getLlmConfiguration("gpt-4-vision")).thenReturn(visionModel);
        
        // When
        Map<String, Object> result = multimodalContentService.validateFile(imageFile, "gpt-4-vision");
        
        // Then
        assertTrue((Boolean) result.get("valid"));
        assertNull(result.get("error"));
    }    @Test
    void validateFile_WithImageAndTextOnlyModel_ShouldReturnInvalid() {
        // Given
        byte[] imageContent = new byte[1024]; // 1KB sample image
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", imageContent
        );
        
        // Use a model name that won't match any pattern in isLikelyVisionCapableModel
        textOnlyModel.setId("text-only-model");
        textOnlyModel.setName("Text Only Model");
        
        when(llmCapabilityService.getLlmConfigurationMerged("text-only-model")).thenReturn(textOnlyModel);
        
        // When
        Map<String, Object> result = multimodalContentService.validateFile(imageFile, "text-only-model");
        
        // Then
        assertFalse((Boolean) result.get("valid"));
        assertNotNull(result.get("error"));
        assertTrue(((String) result.get("error")).contains("not support image"));
    }@Test
    void validateFile_WithPdfAndFullCapabilityModel_ShouldReturnValid() {
        // Given
        byte[] pdfContent = new byte[1024]; // 1KB sample PDF
        MultipartFile pdfFile = new MockMultipartFile(
            "document", "test.pdf", "application/pdf", pdfContent
        );
        
        when(llmCapabilityService.getLlmConfiguration("claude-3-opus")).thenReturn(fullCapabilityModel);
        
        // When
        Map<String, Object> result = multimodalContentService.validateFile(pdfFile, "claude-3-opus");
        
        // Then
        assertTrue((Boolean) result.get("valid"));
        assertNull(result.get("error"));
    }

    @Test
    void validateFile_WithEmptyFile_ShouldReturnInvalid() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
            "image", "empty.jpg", "image/jpeg", new byte[0]
        );
        
        // When
        Map<String, Object> result = multimodalContentService.validateFile(emptyFile, "gpt-4-vision");
        
        // Then
        assertFalse((Boolean) result.get("valid"));
        assertEquals("File is empty or not provided", result.get("error"));
    }

    @Test
    void fileToDataUri_WithJpegImage_ShouldCreateCorrectUri() throws IOException {
        // Given
        byte[] imageContent = "test image content".getBytes();
        MultipartFile imageFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", imageContent
        );
        
        // When
        String dataUri = multimodalContentService.fileToDataUri(imageFile);
        
        // Then
        assertTrue(dataUri.startsWith("data:image/jpeg;base64,"));
        // Verify the base64 content is included
        String base64Content = dataUri.substring("data:image/jpeg;base64,".length());
        assertFalse(base64Content.isEmpty());
    }

    @Test
    void fileToDataUri_WithPdfDocument_ShouldCreateCorrectUri() throws IOException {
        // Given
        byte[] pdfContent = "test pdf content".getBytes();
        MultipartFile pdfFile = new MockMultipartFile(
            "document", "test.pdf", "application/pdf", pdfContent
        );
        
        // When
        String dataUri = multimodalContentService.fileToDataUri(pdfFile);
        
        // Then
        assertTrue(dataUri.startsWith("data:application/pdf;base64,"));
        // Verify the base64 content is included
        String base64Content = dataUri.substring("data:application/pdf;base64,".length());
        assertFalse(base64Content.isEmpty());
    }
}
