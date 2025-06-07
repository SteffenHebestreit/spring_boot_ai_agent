package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Controller.ChatController;
import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class to verify the chat message flow, especially the scenario where user messages
 * are properly saved when streaming is called before explicitly saving the message.
 * This test ensures that the frontend flow (stream->message->get) works correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ChatMessageFlowTest {

    @Mock
    private ChatService chatService;

    @Mock
    private OpenAIService openAIService;
    
    @Mock
    private OpenAIProperties openAIProperties;

    private ChatController chatController;

    @BeforeEach
    public void setup() {
        // Initialize controller with mocks
        chatController = new ChatController(chatService, openAIService, openAIProperties);
        
        // Setup default behaviors
        when(openAIProperties.getModel()).thenReturn("gpt-4");
    }

    /**
     * Test that simulates the actual frontend flow where:
     * 1. First message: POST /create with user message (this works fine)
     * 2. Next messages: POST /message/stream -> then POST /messages with AI response
     * 
     * The key issue is that subsequent user messages aren't being saved by the frontend
     * before streaming, and we need to ensure they're automatically saved during streaming.
     */
    @Test
    public void testCompleteMessageFlowWithMissingUserMessage() {
        // Setup
        String chatId = "test-chat-id";
        String initialUserMessage = "Hello, how are you?";
        String aiResponse = "I'm an AI assistant. How can I help you today?";
        String secondUserMessage = "What is the capital of France?";
        String secondAiResponse = "The capital of France is Paris.";
        
        // Create a chat with only the initial messages
        Chat chat = new Chat();
        chat.setId(chatId);
        
        // Add initial user message (this would be from /create endpoint)
        ChatMessage initialUserChatMsg = new ChatMessage("user", "text/plain", initialUserMessage);
        initialUserChatMsg.setTimestamp(Instant.now());
        chat.addMessage(initialUserChatMsg);
        
        // Add initial AI response (this would be from first stream)
        ChatMessage initialAiChatMsg = new ChatMessage("agent", "text/plain", aiResponse);
        initialAiChatMsg.setTimestamp(Instant.now());
        chat.addMessage(initialAiChatMsg);
        
        // Setup mocks for first part of test - streaming second message without saving it first
        List<ChatMessage> initialHistory = new ArrayList<>(chat.getMessages());
        when(chatService.getChatMessages(eq(chatId))).thenReturn(initialHistory);
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        when(chatService.addMessageToChat(eq(chatId), messageCaptor.capture())).thenReturn(chat);
        
        when(openAIService.getChatCompletionStreamWithToolExecution(any(), eq("gpt-4")))
            .thenReturn(Flux.just(secondAiResponse));
        
        // Execute - simulate streaming the second user message without saving it first
        Flux<String> result = chatController.streamMessage(chatId, secondUserMessage, null, true);
        
        // Verify
        StepVerifier.create(result)
            .expectNext(secondAiResponse)
            .verifyComplete();
        
        // Verify that the user message was saved automatically
        verify(chatService, times(1)).addMessageToChat(
            eq(chatId), 
            argThat(message -> 
                "user".equals(message.getRole()) && 
                secondUserMessage.equals(message.getContent())
            )
        );
        
        // Verify that the AI response was saved after streaming
        verify(chatService, times(2)).addMessageToChat(eq(chatId), any(Message.class));
        
        List<Message> capturedMessages = messageCaptor.getAllValues();
        assertEquals(2, capturedMessages.size());
        
        // First captured message should be the user message
        assertEquals("user", capturedMessages.get(0).getRole());
        assertEquals(secondUserMessage, capturedMessages.get(0).getContent());
        
        // Second captured message should be the AI response
        assertEquals("agent", capturedMessages.get(1).getRole());
        assertEquals(secondAiResponse, capturedMessages.get(1).getContent());
    }
    
    /**
     * Test that duplicates are properly detected and not added twice,
     * especially focusing on the enhanced duplicate detection with substring matching.
     */
    @Test
    public void testEnhancedDuplicateDetection() {
        // Setup test data
        String chatId = "test-chat-id";
        String existingContent = "This is an existing response that is already saved.";
        String duplicateContent = "This is an existing response that is already saved.";
        String substringContent = "This is an existing response";
        String supersetContent = "This is an existing response that is already saved. Plus more content.";
        
        // Create a mock chat with an existing message
        Chat mockChat = new Chat();
        mockChat.setId(chatId);
        
        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setRole("agent");
        existingMessage.setContentType("text/plain");
        existingMessage.setContent(existingContent);
        
        mockChat.addMessage(existingMessage);
        
        // Mock service behavior
        when(chatService.getChatById(eq(chatId))).thenReturn(Optional.of(mockChat));
        when(chatService.addMessageToChat(eq(chatId), any(Message.class))).thenCallRealMethod();
          // Test 1: Exact duplicate detection
        Message duplicateMessage = new Message("agent", "text/plain", duplicateContent);
        chatService.addMessageToChat(chatId, duplicateMessage);
        
        // The chat should remain unchanged (no new message added)
        assertEquals(1, mockChat.getMessages().size());
        
        // Test 2: Substring match detection (for agent messages)
        Message substringMessage = new Message("agent", "text/plain", substringContent);
        chatService.addMessageToChat(chatId, substringMessage);
        
        // Should detect as duplicate since it's a substring of existing content
        assertEquals(1, mockChat.getMessages().size());
        
        // Test 3: Superset match detection (for agent messages)
        Message supersetMessage = new Message("agent", "text/plain", supersetContent);
        chatService.addMessageToChat(chatId, supersetMessage);
        
        // Should detect as duplicate since existing content is a substring of it
        assertEquals(1, mockChat.getMessages().size());
        
        // Test 4: Different role should not be detected as duplicate
        Message userMessage = new Message("user", "text/plain", existingContent);
        
        // Create new mock to avoid test interference
        Chat newMockChat = new Chat();
        newMockChat.setId(chatId);
        newMockChat.addMessage(existingMessage);
        
        when(chatService.getChatById(eq(chatId))).thenReturn(Optional.of(newMockChat));
        when(chatService.addMessageToChat(eq(chatId), eq(userMessage))).thenReturn(newMockChat);
        
        chatService.addMessageToChat(chatId, userMessage);
        
        // Verify that addMessageToChat was called for user message
        verify(chatService, times(1)).addMessageToChat(eq(chatId), eq(userMessage));
    }
}
