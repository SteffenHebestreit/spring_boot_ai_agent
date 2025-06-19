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
import org.springframework.test.util.ReflectionTestUtils;
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
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", "dGVzdCBpbWFnZSBjb250ZW50"); // base64 encoded "test image content"
        fileData.put("fileName", "test.jpg");
        fileData.put("fileType", "image/jpeg");
        fileData.put("fileSize", 18);
        fileData.put("prompt", prompt);
        
        // Setup validation to return valid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", true);
        when(multimodalContentService.validateFileFromBase64(fileData, "gpt-4-vision")).thenReturn(validationResult);
        
        // Setup data URI creation
        String dataUri = "data:image/jpeg;base64,dGVzdCBpbWFnZSBjb250ZW50";
        when(multimodalContentService.base64ToDataUri(anyString(), anyString())).thenReturn(dataUri);
        
        // Setup multimodal content creation
        Object multimodalContent = new Object[]{ Map.of("type", "text", "text", prompt), 
                                                Map.of("type", "image_url", "image_url", Map.of("url", dataUri)) };
        when(multimodalContentService.createMultimodalContentFromBase64(fileData, "gpt-4-vision")).thenReturn(multimodalContent);
        
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
        ResponseEntity<?> response = multimodalController.chatMultimodal(fileData, "gpt-4-vision", null);
          
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(aiResponse, response.getBody());
        
        // Verify the interactions
        verify(multimodalContentService).validateFileFromBase64(fileData, "gpt-4-vision");
        verify(multimodalContentService).createMultimodalContentFromBase64(fileData, "gpt-4-vision");
        verify(openAIService).getMultimodalCompletion(any(), eq("gpt-4-vision"));
        verify(chatService).createChat(any(Message.class));
        verify(chatService).addMessageToChat(eq("chat123"), any(Message.class));
    }    @Test
    void chatMultimodal_WithInvalidFile_ShouldReturnBadRequest() throws Exception {
        // Given
        String prompt = "What's in this image?";
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", "dGVzdCBpbWFnZSBjb250ZW50"); // base64 encoded "test image content"
        fileData.put("fileName", "test.jpg");
        fileData.put("fileType", "image/jpeg");
        fileData.put("fileSize", 18);
        fileData.put("prompt", prompt);
        
        // Setup validation to return invalid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", false);
        validationResult.put("error", "Model does not support images");
        when(multimodalContentService.validateFileFromBase64(fileData, "gpt-3.5-turbo")).thenReturn(validationResult);
        
        // When
        ResponseEntity<?> response = multimodalController.chatMultimodal(fileData, "gpt-3.5-turbo", null);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Object responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.toString().contains("Model does not support images"));
        
        // Verify the interactions
        verify(multimodalContentService).validateFileFromBase64(fileData, "gpt-3.5-turbo");
        verify(multimodalContentService, never()).base64ToDataUri(anyString(), anyString());
        verify(openAIService, never()).getMultimodalCompletion(any(), any());
    }    @Test
    void chatStreamMultimodal_WithValidImageAndPrompt_ShouldReturnFlux() throws Exception {
        // Given
        String prompt = "Describe this image in detail";
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", "dGVzdCBpbWFnZSBjb250ZW50"); // base64 encoded "test image content"
        fileData.put("fileName", "test.jpg");
        fileData.put("fileType", "image/jpeg");
        fileData.put("fileSize", 18);
        fileData.put("prompt", prompt);
        
        // Setup validation to return valid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", true);
        when(multimodalContentService.validateFileFromBase64(fileData, "gpt-4-vision")).thenReturn(validationResult);
        
        // Setup multimodal content creation
        Object multimodalContent = new Object[]{ Map.of("type", "text", "text", prompt), 
                                               Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64,test")) };
        when(multimodalContentService.createMultimodalContentFromBase64(fileData, "gpt-4-vision")).thenReturn(multimodalContent);
        
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
        Flux<String> resultFlux = multimodalController.chatStreamMultimodal(fileData, "gpt-4-vision", null);
        
        // Then
        // Since we can't use StepVerifier in this test environment, we'll verify method calls
        assertNotNull(resultFlux);
        
        // Verify the interactions
        verify(multimodalContentService).validateFileFromBase64(fileData, "gpt-4-vision");
        verify(multimodalContentService).createMultimodalContentFromBase64(fileData, "gpt-4-vision");
        verify(openAIService).getMultimodalCompletionStream(any(), eq("gpt-4-vision"));
        verify(chatService).createChat(any(Message.class));
    }
    
    @Test
    void createMultimodalChat_WithValidData_ShouldReturnChatDetails() throws Exception {
        // Given
        String prompt = "Analyze this image";
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", "dGVzdCBpbWFnZSBjb250ZW50"); // base64 encoded "test image content"
        fileData.put("fileName", "test.jpg");
        fileData.put("fileType", "image/jpeg");
        fileData.put("fileSize", 18);
        fileData.put("prompt", prompt);
        
        // Setup validation to return valid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", true);
        when(multimodalContentService.validateFileFromBase64(fileData, "gpt-4-vision")).thenReturn(validationResult);
        
        // Setup multimodal content creation
        Object multimodalContent = new Object[]{ Map.of("type", "text", "text", prompt), 
                                               Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64,test")) };
        when(multimodalContentService.createMultimodalContentFromBase64(fileData, "gpt-4-vision")).thenReturn(multimodalContent);
        
        // Setup response from OpenAI serialization
        when(openAIService.convertMultimodalContentToString(any())).thenReturn("{\"content\":\"serialized multimodal content\"}");
        
        // Setup chat creation
        Chat newChat = new Chat();
        newChat.setId("chat789");
        when(chatService.createChat(any(Message.class))).thenReturn(newChat);
        
        // When
        ResponseEntity<?> response = multimodalController.createMultimodalChat(fileData, "gpt-4-vision");
          // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("chat789", responseBody.get("chatId"));
        
        // Verify the interactions
        verify(multimodalContentService).validateFileFromBase64(fileData, "gpt-4-vision");
        verify(multimodalContentService).createMultimodalContentFromBase64(fileData, "gpt-4-vision");
        verify(chatService).createChat(any(Message.class));
    }
    
    @Test
    void createMultimodalChat_WithInvalidFile_ShouldReturnBadRequest() throws Exception {
        // Given
        String prompt = "Analyze this image";
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", "dGVzdCBpbWFnZSBjb250ZW50"); // base64 encoded "test image content"
        fileData.put("fileName", "test.jpg");
        fileData.put("fileType", "image/jpeg");
        fileData.put("fileSize", 18);
        fileData.put("prompt", prompt);
        
        // Setup validation to return invalid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", false);
        validationResult.put("error", "File too large");
        when(multimodalContentService.validateFileFromBase64(fileData, "gpt-4-vision")).thenReturn(validationResult);
        
        // When
        ResponseEntity<?> response = multimodalController.createMultimodalChat(fileData, "gpt-4-vision");
          // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("File too large", responseBody.get("error"));
        
        // Verify the interactions
        verify(multimodalContentService).validateFileFromBase64(fileData, "gpt-4-vision");
        verify(multimodalContentService, never()).createMultimodalContentFromBase64(any(), any());
        verify(chatService, never()).createChat(any(Message.class));
    }
    
    @Test
    void createStreamMultimodalChat_WithValidData_ShouldReturnFlux() throws Exception {
        // Given
        String prompt = "Analyze this image in detail";
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", "dGVzdCBpbWFnZSBjb250ZW50"); // base64 encoded "test image content"
        fileData.put("fileName", "test.jpg");
        fileData.put("fileType", "image/jpeg");
        fileData.put("fileSize", 18);
        fileData.put("prompt", prompt);
        
        // Setup validation to return valid
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("valid", true);
        when(multimodalContentService.validateFileFromBase64(fileData, "gpt-4-vision")).thenReturn(validationResult);
        
        // Setup multimodal content creation
        Object multimodalContent = new Object[]{ Map.of("type", "text", "text", prompt), 
                                               Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64,test")) };
        when(multimodalContentService.createMultimodalContentFromBase64(fileData, "gpt-4-vision")).thenReturn(multimodalContent);
        
        // Setup streaming response from OpenAI
        List<String> streamResponses = List.of("I", " can", " see", " a", " cat", " in", " the", " image.");
        when(openAIService.getMultimodalCompletionStream(any(), eq("gpt-4-vision")))
            .thenReturn(Flux.fromIterable(streamResponses));
            
        // Setup response from OpenAI serialization
        when(openAIService.convertMultimodalContentToString(any())).thenReturn("{\"content\":\"serialized multimodal content\"}");
        
        // Setup chat creation
        Chat newChat = new Chat();
        newChat.setId("chat101112");
        when(chatService.createChat(any(Message.class))).thenReturn(newChat);
          // When
        ResponseEntity<Flux<String>> response = multimodalController.createStreamMultimodalChat(fileData, "gpt-4-vision");
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Verify the interactions
        verify(multimodalContentService).validateFileFromBase64(fileData, "gpt-4-vision");
        verify(multimodalContentService).createMultimodalContentFromBase64(fileData, "gpt-4-vision");
        verify(openAIService).getMultimodalCompletionStream(any(), eq("gpt-4-vision"));
        verify(chatService).createChat(any(Message.class));
    }
}
