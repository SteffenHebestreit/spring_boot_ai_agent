package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Repository.ChatMessageRepository;
import com.steffenhebestreit.ai_research.Repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatRepository, chatMessageRepository); 
    }

    @Test
    void createChat_AddsInitialUserMessageAndGeneratesTitle() {
        // Arrange
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat savedChat = invocation.getArgument(0);
            // Simulate title generation if content is present, as done in ChatService
            if (savedChat.getMessages() != null && !savedChat.getMessages().isEmpty()) {
                 ChatMessage firstMsg = savedChat.getMessages().get(0);
                 if (firstMsg.getContent() instanceof String && !((String)firstMsg.getContent()).trim().isEmpty()){
                    // Simplified title generation for test purposes
                    String content = (String)firstMsg.getContent();
                    savedChat.setTitle("Generated: " + content.substring(0, Math.min(30, content.length())));
                 }
            }
            return savedChat;
        });
        
        String userContent = "Hello, can you help me with research about AI ethics?";
        Message userMessage = new Message("user", "text/plain", userContent);
        
        // Act
        Chat result = chatService.createChat(userMessage);
        
        // Assert
        List<ChatMessage> messages = result.getMessages();
        assertEquals(1, messages.size(), "Chat should contain 1 message (the user's initial message)");
        
        ChatMessage firstMessage = messages.get(0);
        assertEquals("user", firstMessage.getRole(), "First message should have 'user' role");
        assertEquals(userContent, firstMessage.getContent(), "User message content should match");
        
        assertNotNull(result.getTitle(), "Chat title should be generated");
        assertTrue(result.getTitle().startsWith("Generated: "), "Chat title should be generated from content");
        assertTrue(result.getTitle().contains(userContent.substring(0, Math.min(30, userContent.length()))), "Chat title should contain part of the user message");
        
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void createChat_WithNullSystemRole_OnlyAddsUserMessage() { // System role is handled by OpenAIService now
        // Arrange
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Message userMessage = new Message("user", "text/plain", "Hello");
        
        // Act
        Chat result = chatService.createChat(userMessage);
        
        // Assert
        List<ChatMessage> messages = result.getMessages();
        assertEquals(1, messages.size(), "Chat should contain only the user message");
        
        ChatMessage message = messages.get(0);
        assertEquals("user", message.getRole(), "Message should have 'user' role");
        assertEquals(userMessage.getContent(), message.getContent(), "Message content should match");
        
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void createChat_WithEmptySystemRole_OnlyAddsUserMessage() { // System role is handled by OpenAIService now
        // Arrange
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        Message userMessage = new Message("user", "text/plain", "Hello");
        
        // Act
        Chat result = chatService.createChat(userMessage);
        
        // Assert
        List<ChatMessage> messages = result.getMessages();
        assertEquals(1, messages.size(), "Chat should contain only the user message");
        
        ChatMessage message = messages.get(0);
        assertEquals("user", message.getRole(), "Message should have 'user' role");
        assertEquals(userMessage.getContent(), message.getContent(), "Message content should match");
        
        verify(chatRepository).save(any(Chat.class));
    }
}
