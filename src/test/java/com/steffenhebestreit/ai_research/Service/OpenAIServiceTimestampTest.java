package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify timestamp functionality in OpenAI service messages.
 */
@ExtendWith(MockitoExtension.class)
class OpenAIServiceTimestampTest {

    @Mock
    private OpenAIProperties openAIProperties;
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private DynamicIntegrationService dynamicIntegrationService;
    
    @Mock
    private LlmCapabilityService llmCapabilityService;
    
    private OpenAIService openAIService;
    private ObjectMapper objectMapper;    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Use lenient stubbing to avoid UnnecessaryStubbingException
        org.mockito.Mockito.lenient().when(webClientBuilder.baseUrl(org.mockito.ArgumentMatchers.anyString())).thenReturn(webClientBuilder);
        org.mockito.Mockito.lenient().when(webClientBuilder.defaultHeader(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(webClientBuilder);
        org.mockito.Mockito.lenient().when(webClientBuilder.build()).thenReturn(webClient);
        org.mockito.Mockito.lenient().when(openAIProperties.getBaseurl()).thenReturn("http://test-api.local");
        org.mockito.Mockito.lenient().when(openAIProperties.getKey()).thenReturn("test-key");
        org.mockito.Mockito.lenient().when(openAIProperties.getModel()).thenReturn("test-model");
        org.mockito.Mockito.lenient().when(openAIProperties.getSystemRole()).thenReturn("You are a helpful assistant.");
        
        openAIService = new OpenAIService(openAIProperties, webClientBuilder, objectMapper, dynamicIntegrationService, llmCapabilityService);
    }

    @Test
    void testSystemMessageIncludesTimestamp() throws Exception {
        // Test that the system message includes current date and time
        java.lang.reflect.Method createSystemMessageMethod = OpenAIService.class.getDeclaredMethod("createSystemMessageWithTime");
        createSystemMessageMethod.setAccessible(true);
        
        ChatMessage systemMessage = (ChatMessage) createSystemMessageMethod.invoke(openAIService);
        
        assertNotNull(systemMessage);
        assertEquals("system", systemMessage.getRole());
        assertTrue(systemMessage.getContent().contains("You are a helpful assistant."));
        assertTrue(systemMessage.getContent().contains("Current date and time:"));
        assertTrue(systemMessage.getContent().matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"));
    }

    @Test
    void testMessageTimestampPrefixGeneration() throws Exception {
        // Create a test message with a known timestamp
        ChatMessage testMessage = new ChatMessage("user", "Hello");
        testMessage.setCreatedAt(Instant.parse("2025-06-20T10:30:45Z"));
        
        // Access the private method
        java.lang.reflect.Method getTimestampPrefixMethod = OpenAIService.class.getDeclaredMethod("getTimestampPrefix", ChatMessage.class);
        getTimestampPrefixMethod.setAccessible(true);
        
        String timestampPrefix = (String) getTimestampPrefixMethod.invoke(openAIService, testMessage);
        
        assertNotNull(timestampPrefix);
        assertTrue(timestampPrefix.startsWith("["));
        assertTrue(timestampPrefix.endsWith("] "));
        // Should contain a date-time pattern
        assertTrue(timestampPrefix.matches("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] "));
    }

    @Test
    void testPrepareMessagesIncludesTimestamps() throws Exception {
        // Create test messages
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userMessage = new ChatMessage("user", "Hello");
        userMessage.setCreatedAt(Instant.parse("2025-06-20T10:30:45Z"));
        messages.add(userMessage);
        
        ChatMessage assistantMessage = new ChatMessage("assistant", "Hi there!");
        assistantMessage.setCreatedAt(Instant.parse("2025-06-20T10:31:00Z"));
        messages.add(assistantMessage);
        
        // Access the private method
        java.lang.reflect.Method prepareMessagesMethod = OpenAIService.class.getDeclaredMethod("prepareMessagesForLlm", List.class);
        prepareMessagesMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> preparedMessages = (List<Map<String, Object>>) prepareMessagesMethod.invoke(openAIService, messages);
        
        // Should have system message + user messages
        assertTrue(preparedMessages.size() >= 2);
        
        // Check that user message content includes timestamp
        Map<String, Object> preparedUserMessage = preparedMessages.stream()
            .filter(msg -> "user".equals(msg.get("role")))
            .findFirst()
            .orElse(null);
        
        assertNotNull(preparedUserMessage);
        String userContent = (String) preparedUserMessage.get("content");
        assertTrue(userContent.contains("["));
        assertTrue(userContent.contains("] Hello"));
        
        // Check that assistant message content includes timestamp
        Map<String, Object> preparedAssistantMessage = preparedMessages.stream()
            .filter(msg -> "assistant".equals(msg.get("role")))
            .findFirst()
            .orElse(null);
        
        assertNotNull(preparedAssistantMessage);
        String assistantContent = (String) preparedAssistantMessage.get("content");
        assertTrue(assistantContent.contains("["));
        assertTrue(assistantContent.contains("] Hi there!"));
    }

    @Test
    void testPrepareConversationWithSystemMessage() throws Exception {
        // Create test messages
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userMessage = new ChatMessage("user", "Hello");
        messages.add(userMessage);
        
        // Access the private method
        java.lang.reflect.Method prepareConversationMethod = OpenAIService.class.getDeclaredMethod("prepareConversationWithSystemMessage", List.class);
        prepareConversationMethod.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<ChatMessage> preparedMessages = (List<ChatMessage>) prepareConversationMethod.invoke(openAIService, messages);
        
        // Should have system message + user message
        assertEquals(2, preparedMessages.size());
        
        // First message should be system message with timestamp
        ChatMessage systemMessage = preparedMessages.get(0);
        assertEquals("system", systemMessage.getRole());
        assertTrue(systemMessage.getContent().contains("You are a helpful assistant."));
        assertTrue(systemMessage.getContent().contains("Current date and time:"));
        
        // Second message should be the original user message
        ChatMessage userMsg = preparedMessages.get(1);
        assertEquals("user", userMsg.getRole());
        assertEquals("Hello", userMsg.getContent());
    }

    @Test
    void testTimestampFormatIsReadable() throws Exception {
        // Test that timestamps are in a human-readable format (not ISO instant format)
        java.lang.reflect.Method createSystemMessageMethod = OpenAIService.class.getDeclaredMethod("createSystemMessageWithTime");
        createSystemMessageMethod.setAccessible(true);
        
        ChatMessage systemMessage = (ChatMessage) createSystemMessageMethod.invoke(openAIService);
        
        assertNotNull(systemMessage);
        String content = systemMessage.getContent();
        
        // Should not contain ISO format (like 2025-06-20T10:30:45.123Z)
        assertFalse(content.contains("T"));
        assertFalse(content.contains("Z"));
        
        // Should contain readable format (like 2025-06-20 10:30:45)
        assertTrue(content.matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"));
    }
}
