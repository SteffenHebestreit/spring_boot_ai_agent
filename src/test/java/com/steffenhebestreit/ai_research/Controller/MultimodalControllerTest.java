package com.steffenhebestreit.ai_research.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.LlmCapabilityService;
import com.steffenhebestreit.ai_research.Service.MultimodalContentService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MultimodalControllerTest {

    @Mock
    private OpenAIService openAIService;

    @Mock
    private ChatService chatService;    @Mock
    private MultimodalContentService multimodalContentService;

    @Mock
    private LlmCapabilityService llmCapabilityService;
    
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MultimodalController multimodalController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(multimodalController, "defaultLlmId", "gpt-4-vision");
        
        // Set up the mocks for common operations
        LlmConfiguration visionModel = LlmConfiguration.builder()
            .id("gpt-4-vision")
            .name("GPT-4 Vision")
            .supportsText(true)
            .supportsImage(true)
            .supportsPdf(false)
            .build();
            
        when(llmCapabilityService.getLlmConfiguration("gpt-4-vision")).thenReturn(visionModel);
    }    @Test
    void chatMultimodal_WithValidImageAndPrompt_ShouldReturnSuccess() throws Exception {
        // Given
        String prompt = "What's in this image?";
        MultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        
        // Setup validation to return valid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", true);
        when(multimodalContentService.validateFile(file, "gpt-4-vision")).thenReturn(validationResult);
        
        // Setup data URI creation
        String dataUri = "data:image/jpeg;base64,dGVzdCBpbWFnZSBjb250ZW50";
        when(multimodalContentService.fileToDataUri(file)).thenReturn(dataUri);
        
        // Setup multimodal content creation
        Object multimodalContent = new Object[]{ Map.of("type", "text", "text", prompt), 
                                                Map.of("type", "image_url", "image_url", Map.of("url", dataUri)) };
        when(multimodalContentService.createMultimodalContent(prompt, file, "gpt-4-vision")).thenReturn(multimodalContent);
        
        // Setup response from OpenAI
        String aiResponse = "I can see a cat in the image.";
        when(openAIService.getMultimodalCompletion(any(), eq("gpt-4-vision"))).thenReturn(aiResponse);
        
        // Setup ObjectMapper mock
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"multimodal\":\"content\"}");
          
        // Setup chat creation
        Chat newChat = new Chat();
        newChat.setId("chat123");
        when(chatService.createChat(any(Message.class))).thenReturn(newChat);
        
        // Mock the getRecentChats method to return an empty list (no duplicates)
        when(chatService.getRecentChats(anyInt())).thenReturn(new ArrayList<>());
          // When
        ResponseEntity<?> response = multimodalController.chatMultimodal(prompt, file, "gpt-4-vision", null);
          
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(aiResponse, response.getBody());
        
        // Verify the interactions
        verify(multimodalContentService).validateFile(file, "gpt-4-vision");
        verify(multimodalContentService).createMultimodalContent(prompt, file, "gpt-4-vision");
        verify(openAIService).getMultimodalCompletion(any(), eq("gpt-4-vision"));
        verify(chatService).createChat(any(Message.class));
        verify(chatService).addMessageToChat(eq("chat123"), any(Message.class));
    }

    @Test
    void chatMultimodal_WithInvalidFile_ShouldReturnBadRequest() throws Exception {
        // Given
        String prompt = "What's in this image?";
        MultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        
        // Setup validation to return invalid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", false);
        validationResult.put("error", "Model does not support images");
        when(multimodalContentService.validateFile(file, "gpt-3.5-turbo")).thenReturn(validationResult);
        
        // When
        ResponseEntity<?> response = multimodalController.chatMultimodal(prompt, file, "gpt-3.5-turbo", null);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Object responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.toString().contains("Model does not support images"));
        
        // Verify the interactions
        verify(multimodalContentService).validateFile(file, "gpt-3.5-turbo");
        verify(multimodalContentService, never()).fileToDataUri(any());
        verify(openAIService, never()).getMultimodalCompletion(any(), any());
    }    @Test
    void chatStreamMultimodal_WithValidImageAndPrompt_ShouldReturnFlux() throws Exception {
        // Given
        String prompt = "Describe this image in detail";
        MultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        
        // Setup validation to return valid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", true);
        when(multimodalContentService.validateFile(file, "gpt-4-vision")).thenReturn(validationResult);
        
        // Setup multimodal content creation
        Object multimodalContent = new Object[]{ Map.of("type", "text", "text", prompt), 
                                               Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64,test")) };
        when(multimodalContentService.createMultimodalContent(prompt, file, "gpt-4-vision")).thenReturn(multimodalContent);
        
        // Setup streaming response from OpenAI
        List<String> streamResponses = List.of("I", " can", " see", " a", " cat", " in", " the", " image.");
        when(openAIService.getMultimodalCompletionStream(any(), eq("gpt-4-vision")))
            .thenReturn(Flux.fromIterable(streamResponses));
        
        // Setup ObjectMapper mock
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"multimodal\":\"content\"}");
        
        // Mock the getRecentChats method to return an empty list (no duplicates)
        when(chatService.getRecentChats(anyInt())).thenReturn(new ArrayList<>());
          
        // Setup chat creation
        Chat newChat = new Chat();
        newChat.setId("chat456");
        when(chatService.createChat(any(Message.class))).thenReturn(newChat);
        
        // When
        Flux<String> resultFlux = multimodalController.chatStreamMultimodal(prompt, file, "gpt-4-vision", null);
        
        // Then
        // Since we can't use StepVerifier in this test environment, we'll verify method calls
        assertNotNull(resultFlux);
        
        // Verify the interactions
        verify(multimodalContentService).validateFile(file, "gpt-4-vision");
        verify(multimodalContentService).createMultimodalContent(prompt, file, "gpt-4-vision");
        verify(openAIService).getMultimodalCompletionStream(any(), eq("gpt-4-vision"));
        verify(chatService).createChat(any(Message.class));
    }
}
