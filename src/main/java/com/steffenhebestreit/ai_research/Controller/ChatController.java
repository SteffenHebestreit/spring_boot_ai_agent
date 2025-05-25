package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import org.slf4j.Logger; // Added import for Logger
import org.slf4j.LoggerFactory; // Added import for LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/research-agent/api/chats")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class); // Added logger
    private final ChatService chatService;
    private final OpenAIService openAIService;

    @Autowired
    public ChatController(ChatService chatService, OpenAIService openAIService) {
        this.chatService = chatService;
        this.openAIService = openAIService;
    }

    /**
     * Get all chats
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllChats() {
        List<Chat> chats = chatService.getAllChats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", chats);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific chat by ID
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<Map<String, Object>> getChatById(@PathVariable String chatId) {
        return chatService.getChatById(chatId)
                .map(chat -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", chat);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse(-32602, "Chat not found with ID: " + chatId)));
    }

    /**
     * Create a new chat with an initial message
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createChat(@RequestBody Message initialMessage) {
        try {
            Chat chat = chatService.createChat(initialMessage); // This saves the initial user message
            
            Map<String, Object> response = new HashMap<>();
            response.put("result", chat); // chat now contains only the initial user message
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating chat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(-32000, "Error creating chat: " + e.getMessage()));
        }
    }

    /**
     * Add a message to an existing chat (used by frontend to save user and completed AI messages)
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<Map<String, Object>> addMessageToChat(@PathVariable String chatId, @RequestBody Message message) {
        try {
            Chat chat = chatService.addMessageToChat(chatId, message);
            Map<String, Object> response = new HashMap<>();
            response.put("result", chat);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error adding message to chat {}: {}", chatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(-32602, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error adding message to chat {}: {}", chatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(-32000, "Unexpected error adding message: " + e.getMessage()));
        }
    }

    /**
     * Stream a response to a message in a chat (similar to TaskStreamingController)
     */
    @PostMapping(value = "/{chatId}/message/stream", consumes = MediaType.TEXT_PLAIN_VALUE, 
                produces = MediaType.APPLICATION_NDJSON_VALUE) // Changed to NDJSON for better stream handling
    public Flux<String> streamMessage(@PathVariable String chatId, @RequestBody String userMessageContent) {
        if (userMessageContent == null || userMessageContent.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("User message cannot be empty"));
        }

        try {
            // Fetch the existing conversation history
            List<ChatMessage> conversationHistory = chatService.getChatMessages(chatId);
            
            // The user's new message is already saved by the frontend before this call.
            // So, the conversationHistory should ideally contain it if fetched after save.
            // If not, we might need to adjust the frontend to ensure the message is saved first,
            // or add the current userMessageContent to the history list before sending to OpenAI.
            // For now, let's assume conversationHistory is up-to-date.
            // If the AI is not seeing the latest user message, this is the area to investigate.

            logger.info("Streaming message for chat ID: {}. History size: {}", chatId, conversationHistory.size());
            if (!conversationHistory.isEmpty()) {
                logger.debug("Last message in history for chat {}: Role: {}, Content: {}", 
                    chatId, 
                    conversationHistory.get(conversationHistory.size()-1).getRole(), 
                    conversationHistory.get(conversationHistory.size()-1).getContent().substring(0, Math.min(100, conversationHistory.get(conversationHistory.size()-1).getContent().length())));
            } else {
                // This case should ideally not happen if a chat is created with an initial message.
                // If it does, it means we are streaming for a chat that has no messages in DB yet.
                // We might need to construct a minimal history with the current userMessageContent.
                logger.warn("Conversation history for chat {} is empty. This might lead to unexpected AI behavior.", chatId);
                // Fallback: create a temporary history with just the current user message
                // This is a quick fix; ideally, the chat creation and message addition flow ensures history is present.
                ChatMessage currentMsg = new ChatMessage();
                currentMsg.setRole("user");
                currentMsg.setContent(userMessageContent);
                // currentMsg.setTimestamp(java.time.LocalDateTime.now()); // Timestamp not strictly needed for LLM call
                // currentMsg.setChat(chatService.getChatById(chatId).orElse(null)); // Chat association not strictly needed for LLM call
                conversationHistory = List.of(currentMsg);
            }

            return openAIService.getChatCompletionStream(conversationHistory)
                    .doOnError(e -> logger.error("Error during AI stream for chat {}: {}", chatId, e.getMessage(), e))
                    .onErrorResume(e -> Flux.just("{\"error\": \"Error processing your request: " + e.getMessage().replace("\"", "\\\"") + "\"}")); // Send error as JSON
        } catch (Exception e) {
            logger.error("Error initiating stream for chat {}: {}", chatId, e.getMessage(), e);
            return Flux.just("{\"error\": \"Error initiating stream: " + e.getMessage().replace("\"", "\\\"") + "\"}"); // Send error as JSON
        }
    }

    /**
     * Update chat title
     */
    @PutMapping("/{chatId}/title")
    public ResponseEntity<Map<String, Object>> updateChatTitle(@PathVariable String chatId, @RequestBody Map<String, String> request) {
        String newTitle = request.get("title");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(-32602, "Title cannot be empty"));
        }
        
        try {
            Chat updatedChat = chatService.updateChatTitle(chatId, newTitle);
            
            Map<String, Object> response = new HashMap<>();
            response.put("result", updatedChat);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(-32602, e.getMessage()));
        }
    }

    /**
     * Delete a chat
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Map<String, Object>> deleteChat(@PathVariable String chatId) {
        try {
            chatService.deleteChat(chatId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("result", Map.of("success", true, "message", "Chat deleted successfully"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(-32602, e.getMessage()));
        }
    }

    /**
     * Get messages for a specific chat
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Map<String, Object>> getChatMessages(@PathVariable String chatId) {
        try {
            List<ChatMessage> messages = chatService.getChatMessages(chatId);
            chatService.markChatAsRead(chatId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("result", messages);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(-32602, e.getMessage()));
        }
    }

    /**
     * Test endpoint for admin role
     */
    @GetMapping("/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminTestEndpoint() {
        return ResponseEntity.ok("Welcome, Admin! This is a restricted endpoint.");
    }

    /**
     * Test endpoint for user role
     */
    @GetMapping("/user/test")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> userTestEndpoint() {
        return ResponseEntity.ok("Welcome, User! This is an endpoint for users.");
    }

    private Map<String, Object> createErrorResponse(int code, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", Map.of(
            "code", code,
            "message", message
        ));
        return errorResponse;
    }
}
