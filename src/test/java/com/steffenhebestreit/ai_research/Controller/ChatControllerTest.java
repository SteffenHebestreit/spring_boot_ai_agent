package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.MultimodalContentProcessingService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatControllerTest {

    @Mock
    private OpenAIService openAIService;

    @Mock
    private ChatService chatService;

    @Mock 
    private OpenAIProperties openAIProperties;
    
    @Mock
    private MultimodalContentProcessingService multimodalContentProcessingService;

    @InjectMocks
    private ChatController chatController;

    @Test
    void streamMessage_whenAiResponseIsOnlyToolContent_shouldNotSaveMessage() {
        // Arrange
        String chatId = "testChatId";
        String userMessageContent = "Hello";
        
        ChatMessage userChatMessage = new ChatMessage();
        userChatMessage.setRole("user");
        userChatMessage.setContent(userMessageContent);
        List<ChatMessage> conversationHistory = Collections.singletonList(userChatMessage);

        String rawAiResponse = "[Calling tool MyTool with args: {}]";
        // Correctly escaped JSON string for the expected error
        String expectedErrorJson = "{\"error\": \"AI response was empty after filtering tool-related content.\"}";

        when(chatService.getChatMessages(chatId)).thenReturn(conversationHistory); 
        when(openAIProperties.getModel()).thenReturn("gpt-4-test"); 
        when(openAIService.getChatCompletionStreamWithToolExecution(anyList(), eq("gpt-4-test")))
                .thenReturn(Flux.just(rawAiResponse)); 

        // Act
        Flux<String> result = chatController.streamMessage(chatId, userMessageContent, "gpt-4-test", true);

        // Assert
        StepVerifier.create(result)
                .expectNext(rawAiResponse) // The raw tool call is still sent
                .expectNext(expectedErrorJson) // Expect the error JSON from onErrorResume
                .verifyComplete(); // onErrorResume completes the stream

        verify(chatService, never()).addMessageToChat(eq(chatId), argThat(m -> 
            m.getRole().equals("agent") && 
            m.getContent() instanceof String && 
            ((String)m.getContent()).isEmpty()
        ));
    }

    @Test
    void streamMessage_whenAiResponseIsMixedContent_shouldSaveFilteredMessage() {
        // Arrange
        String chatId = "testChatId2";
        String userMessageContent = "Describe something.";

        ChatMessage userChatMessage = new ChatMessage();
        userChatMessage.setRole("user");
        userChatMessage.setContent(userMessageContent);
        List<ChatMessage> conversationHistory = Collections.singletonList(userChatMessage);

        String rawAiResponseChunk1 = "[Calling tool MyTool with args: {}]";
        String rawAiResponseChunk2 = " This is the actual response.";
        String expectedFilteredResponse = "This is the actual response.";

        when(chatService.getChatMessages(chatId)).thenReturn(conversationHistory); 
        when(openAIProperties.getModel()).thenReturn("gpt-4-test"); 
        when(openAIService.getChatCompletionStreamWithToolExecution(anyList(), eq("gpt-4-test")))
                .thenReturn(Flux.just(rawAiResponseChunk1, rawAiResponseChunk2));

        // Act
        Flux<String> result = chatController.streamMessage(chatId, userMessageContent, "gpt-4-test", true);

        // Assert
        StepVerifier.create(result)
                .expectNext(rawAiResponseChunk1)
                .expectNext(rawAiResponseChunk2)
                .verifyComplete();

        verify(chatService).addMessageToChat(eq(chatId), argThat(message ->
                message.getRole().equals("agent") &&
                message.getContent().equals(expectedFilteredResponse)
        ));
    }
}
