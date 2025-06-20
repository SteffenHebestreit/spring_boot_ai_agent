package com.steffenhebestreit.ai_research.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.MultimodalContentProcessingService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import com.steffenhebestreit.ai_research.Util.ContentFilterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing chat sessions and conversations.
 * 
 * <p>This controller provides comprehensive chat management functionality including chat creation,
 * message handling, streaming responses, and chat lifecycle operations. It serves as the primary
 * interface for conversational AI interactions in the research system.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>Chat Session Management:</strong> Create, retrieve, update, and delete chat sessions</li>
 * <li><strong>Message Handling:</strong> Add messages to chats and retrieve conversation history</li>
 * <li><strong>Streaming Support:</strong> Real-time streaming AI responses for interactive conversations</li>
 * <li><strong>Security Integration:</strong> Role-based access control for administrative functions</li>
 * </ul>
 * 
 * <h3>Integration Points:</h3>
 * <ul>
 * <li>{@link ChatService} - Core business logic for chat operations and persistence</li>
 * <li>{@link OpenAIService} - AI model integration for generating conversational responses</li>
 * </ul>
 * 
 * <h3>API Design:</h3>
 * <p>Follows REST principles with JSON-RPC 2.0 style responses. All successful operations return
 * a "result" field, while errors return an "error" field with appropriate HTTP status codes.</p>
 * 
 * <h3>Stream Processing:</h3>
 * <p>Implements reactive streaming using Project Reactor for real-time chat responses.
 * Streaming endpoints produce NDJSON format for efficient client-side parsing.</p>
 * 
 * <h3>Error Handling:</h3>
 * <p>Comprehensive error handling with appropriate HTTP status codes:
 * <ul>
 * <li>200 (OK) - Successful operations</li>
 * <li>201 (CREATED) - Chat creation success</li>
 * <li>400 (BAD_REQUEST) - Invalid input parameters</li>
 * <li>404 (NOT_FOUND) - Chat or resource not found</li>
 * <li>500 (INTERNAL_SERVER_ERROR) - Unexpected system errors</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see ChatService
 * @see OpenAIService
 * @see TaskController
 */
@RestController
@RequestMapping("/research-agent/api/chats")
public class ChatController {    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final OpenAIService openAIService;
    private final OpenAIProperties openAIProperties;
    private final MultimodalContentProcessingService multimodalContentProcessingService;
    private final ObjectMapper objectMapper;    /**
     * Constructs a ChatController with required service dependencies.
     * 
     * @param chatService Service for chat session management and persistence
     * @param openAIService Service for AI model interactions and response generation
     * @param openAIProperties Configuration properties for the OpenAI API
     * @param multimodalContentProcessingService Service for processing multimodal content and token optimization
     * @param objectMapper Jackson ObjectMapper for JSON processing
     */
    public ChatController(ChatService chatService, OpenAIService openAIService, OpenAIProperties openAIProperties, MultimodalContentProcessingService multimodalContentProcessingService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.openAIService = openAIService;
        this.openAIProperties = openAIProperties;
        this.multimodalContentProcessingService = multimodalContentProcessingService;
        this.objectMapper = objectMapper;
    }/**
     * Retrieves all chat sessions in the system.
     * 
     * <p>Returns a list of all chat sessions with their basic information including
     * chat ID, title, creation timestamp, and last activity. This endpoint is useful
     * for displaying chat lists in user interfaces.</p>
     * 
     * <h3>Response Format:</h3>
     * <p>JSON-RPC response with "result" field containing an array of Chat objects.</p>
     * 
     * @return ResponseEntity with JSON-RPC response containing list of all chats
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllChats() {
        List<Chat> chats = chatService.getAllChats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", chats);
        
        return ResponseEntity.ok(response);
    }    /**
     * Retrieves a specific chat session by its unique identifier.
     * 
     * <p>Fetches complete chat details including metadata and basic information.
     * For message history, use the dedicated messages endpoint to avoid loading
     * large conversation data unnecessarily.</p>
     * 
     * <h3>Response Scenarios:</h3>
     * <ul>
     * <li><strong>Success:</strong> Returns chat object with metadata</li>
     * <li><strong>Not Found:</strong> Returns 404 with error details</li>
     * </ul>
     * 
     * @param chatId The unique identifier of the chat to retrieve
     * @return ResponseEntity with JSON-RPC response containing chat details or error
     * @see #getChatMessages(String) for retrieving conversation history
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
    }    /**
     * Creates a new chat session with an initial message.
     * 
     * <p>Initializes a new chat session with the provided message as the first entry
     * in the conversation. The chat is assigned a unique identifier and can be extended
     * with additional messages through other endpoints.</p>
     * 
     * <h3>Chat Creation Process:</h3>
     * <ol>
     * <li>Validates the initial message content and format</li>
     * <li>Creates new chat entity with generated UUID</li>
     * <li>Associates the initial message with the chat</li>
     * <li>Sets creation timestamp and initial metadata</li>
     * <li>Persists chat and message to database</li>
     * </ol>
     * 
     * <h3>Initial Message Requirements:</h3>
     * <ul>
     * <li>Must have valid role (typically "user")</li>
     * <li>Must have content type (e.g., "text/plain")</li>
     * <li>Must have non-empty content</li>
     * </ul>
     * 
     * @param initialMessage The initial message to start the chat with
     * @return ResponseEntity with JSON-RPC response containing the created chat (HTTP 201)
     * @throws IllegalArgumentException if initialMessage is invalid
     */    @PostMapping("/create")    public ResponseEntity<Map<String, Object>> createChat(@RequestBody Message initialMessage) {          try {
            String originalContent = null;
            // Filter the initial message content to remove thinking tags and ensure it fits database constraints
            if (initialMessage.getContent() != null && initialMessage.getContent() instanceof String) {
                originalContent = (String) initialMessage.getContent();
                String filteredContent = ContentFilterUtil.filterForDatabase(originalContent);
                initialMessage.setContent(filteredContent);
                logger.debug("Filtered initial message content, original length: {}, filtered length: {}", 
                    originalContent.length(), filteredContent.length());
            } else if (initialMessage.getContent() != null && "multipart/mixed".equals(initialMessage.getContentType())) {
                // Handle multimodal content - convert to proper JSON string representation
                try {
                    originalContent = openAIService.convertMultimodalContentToString(initialMessage.getContent());
                    initialMessage.setContent(originalContent);
                    logger.debug("Converted multimodal content to JSON string for chat creation, length: {}", 
                        originalContent.length());
                } catch (Exception e) {
                    logger.error("Failed to convert multimodal content to string: {}", e.getMessage(), e);
                    // Leave content as-is if conversion fails
                }
            }
            
            // Check if a recent chat exists with a similar user message to avoid duplicates
            // This is especially helpful for multimodal requests where the frontend might create multiple chats
            List<Chat> recentChats = chatService.getRecentChats(10); // Get recent chats to check
            Chat existingChat = null;
            
            for (Chat chat : recentChats) {
                if (!chat.getMessages().isEmpty()) {
                    ChatMessage firstMessage = chat.getMessages().get(0);
                    // Only compare user messages
                    if ("user".equals(firstMessage.getRole())) {
                        boolean contentTypesMatch = (firstMessage.getContentType() == null && initialMessage.getContentType() == null) ||
                                                   (firstMessage.getContentType() != null && 
                                                    firstMessage.getContentType().equals(initialMessage.getContentType()));
                          // Enhanced duplicate detection to handle cross-content-type scenarios
                        if (contentTypesMatch && "multipart/mixed".equals(initialMessage.getContentType())) {
                            // Special handling for multimodal content - high chance it's a duplicate request
                            // from frontend if we see a recent multimodal chat
                            if (System.currentTimeMillis() - chat.getCreatedAt().toEpochMilli() < 10000) { // within 10 seconds
                                logger.info("Found recent multimodal chat with same content type. Reusing existing chat: {}", chat.getId());
                                existingChat = chat;
                                break;
                            }
                        }
                        // For text content, do a more detailed comparison
                        else if (contentTypesMatch && initialMessage.getContent() instanceof String && 
                                 firstMessage.getContent() instanceof String) {
                            String existingContent = (String) firstMessage.getContent();
                            String newContent = (String) initialMessage.getContent();
                            
                            if (existingContent.equals(newContent)) {
                                logger.info("Found existing chat with identical user message. Reusing existing chat: {}", chat.getId());
                                existingChat = chat;
                                break;
                            }
                        }                        // CROSS-CONTENT-TYPE MATCHING: Check if incoming text message is already part of existing multimodal message
                        else if ("text/plain".equals(initialMessage.getContentType()) && 
                                 "multipart/mixed".equals(firstMessage.getContentType()) &&
                                 initialMessage.getContent() instanceof String &&
                                 firstMessage.getContent() instanceof String) {
                            
                            String incomingTextContent = (String) initialMessage.getContent();
                            String existingMultimodalContent = (String) firstMessage.getContent();
                            
                            // Use ObjectMapper to properly parse and extract text from multimodal JSON
                            boolean textFoundInMultimodal = extractTextFromMultimodalJson(existingMultimodalContent, incomingTextContent);
                            
                            if (textFoundInMultimodal) {
                                // Only consider recent chats to avoid false positives
                                if (System.currentTimeMillis() - chat.getCreatedAt().toEpochMilli() < 30000) { // within 30 seconds
                                    logger.info("Found existing multimodal chat containing the same text content. Reusing existing chat: {}", chat.getId());
                                    existingChat = chat;
                                    break;
                                }
                            }
                        }
                    }
                }
            }            // If we found an existing similar chat, return that instead of creating a new one
            if (existingChat != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("result", existingChat);
                return ResponseEntity.ok(response); // Return 200 OK instead of 201 Created
            }
            
            // No existing chat found, create a new one
            Chat chat = chatService.createChat(initialMessage); // This saves the initial user message
            
            // Save raw content if filtering occurred
            if (originalContent != null && !originalContent.equals(initialMessage.getContent())) {
                List<ChatMessage> messages = chat.getMessages();
                if (!messages.isEmpty()) {
                    ChatMessage lastMessage = messages.get(messages.size() - 1);
                    chatService.updateMessageRawContent(lastMessage.getId(), originalContent);
                    logger.debug("Saved raw content for message {} in chat creation", lastMessage.getId());
                }
            }            Map<String, Object> response = new HashMap<>();
            response.put("result", chat); // chat now contains only the initial user message
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating chat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(-32000, "Error creating chat: " + e.getMessage()));
        }
    }    /**
     * Adds a message to an existing chat session.
     * 
     * <p>Appends a new message to the conversation history of an existing chat.
     * This endpoint is primarily used by frontend applications to save both user
     * messages and completed AI responses to maintain conversation continuity.</p>
     * 
     * <h3>Use Cases:</h3>
     * <ul>
     * <li>Save user messages before requesting AI responses</li>
     * <li>Store completed AI responses after streaming finishes</li>
     * <li>Add system messages or metadata entries</li>
     * </ul>
     * 
     * <h3>Message Processing:</h3>
     * <ol>
     * <li>Validates chat existence</li>
     * <li>Validates message format and content</li>
     * <li>Associates message with chat session</li>
     * <li>Updates chat's last activity timestamp</li>
     * <li>Persists message to conversation history</li>
     * </ol>
     * 
     * @param chatId The unique identifier of the target chat
     * @param message The message to add to the chat
     * @return ResponseEntity with JSON-RPC response containing updated chat
     * @throws RuntimeException if chat is not found
     * @throws IllegalArgumentException if message format is invalid
     */    @PostMapping("/{chatId}/messages")    public ResponseEntity<Map<String, Object>> addMessageToChat(@PathVariable String chatId, @RequestBody Message message) {        try {
            String originalContent = null;
            // Filter the message content to remove thinking tags and ensure it fits database constraints
            if (message.getContent() != null && message.getContent() instanceof String) {
                originalContent = (String) message.getContent();
                String filteredContent = ContentFilterUtil.filterForDatabase(originalContent);
                message.setContent(filteredContent);
                logger.debug("Filtered message content for chat {}, original length: {}, filtered length: {}", 
                    chatId, 
                    originalContent.length(), 
                    filteredContent.length());
            }
            
            Chat chat = chatService.addMessageToChat(chatId, message);
            
            // Save raw content if filtering occurred
            if (originalContent != null && !originalContent.equals(message.getContent())) {
                List<ChatMessage> messages = chat.getMessages();
                if (!messages.isEmpty()) {
                    ChatMessage lastMessage = messages.get(messages.size() - 1);
                    chatService.updateMessageRawContent(lastMessage.getId(), originalContent);
                    logger.debug("Saved raw content for message {} in chat {}", lastMessage.getId(), chatId);
                }
            }
            
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
    }/**
     * Streams AI responses for a message in an existing chat session.
     * 
     * <p>Provides real-time streaming AI responses within the context of an existing chat.
     * This endpoint fetches the conversation history, processes it with the AI model,
     * and streams the response back in real-time using reactive streams.</p>
     * 
     * <h3>Technical Implementation:</h3>
     * <ul>
     * <li>Uses Reactor Flux for reactive streaming capabilities</li>
     * <li>Produces NDJSON (Newline Delimited JSON) for efficient parsing</li>
     * <li>Maintains conversation context from chat history</li>
     * <li>Implements comprehensive error handling with stream-safe responses</li>
     * </ul>
     * 
     * <h3>Process Flow:</h3>
     * <ol>
     * <li>Validates input message content</li>
     * <li>Retrieves existing conversation history from chat</li>
     * <li>Handles edge case of empty conversation history</li>
     * <li>Processes conversation through OpenAI service</li>
     * <li>Streams response chunks as they arrive</li>
     * <li>Provides error recovery for stream failures</li>
     * </ol>
     * 
     * <h3>Error Handling Strategy:</h3>
     * <ul>
     * <li>Input validation errors return error flux immediately</li>
     * <li>Empty history triggers fallback message construction</li>
     * <li>Stream processing errors are caught and replaced with JSON error messages</li>
     * <li>All errors are logged with appropriate context</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> This endpoint expects the user message to already be saved
     * to the chat history by the frontend before calling. If history is empty, it creates
     * a temporary message for AI processing.</p>
     *     * @param chatId The unique identifier of the chat session
     * @param userMessageContent The user's message content as plain text
     * @param llmId Optional ID of the language model to use
     * @param autoSaveResponse Whether to automatically save the AI response after streaming completes (default: true)
     * @return Flux&lt;String&gt; streaming AI response chunks in NDJSON format
     * @throws IllegalArgumentException if userMessageContent is null or empty
     * @see #addMessageToChat(String, Message) for saving messages to chat
     */    @PostMapping(value = "/{chatId}/message/stream", consumes = MediaType.TEXT_PLAIN_VALUE, 
                produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> streamMessage(
            @PathVariable String chatId, 
            @RequestBody String userMessageContent,
            @RequestParam(value = "llmId", required = false) String llmId,
            @RequestParam(value = "autoSaveResponse", defaultValue = "true") boolean autoSaveResponse) {
        
        if (userMessageContent == null || userMessageContent.trim().isEmpty()) {
            // Return a Flux error that will be caught by onErrorResume
            return Flux.error(new IllegalArgumentException("User message cannot be empty"));
        }

        try {
            String modelToUse = (llmId != null && !llmId.isEmpty()) ? 
                llmId : openAIProperties.getModel();
            
            logger.info("Using LLM model: {} for chat: {}", modelToUse, chatId);
            
            List<ChatMessage> conversationHistory = chatService.getChatMessages(chatId);
              boolean foundCurrentUserMessage = false;
            if (!conversationHistory.isEmpty()) {
                ChatMessage lastMessage = conversationHistory.get(conversationHistory.size() - 1);
                if ("user".equals(lastMessage.getRole()) && lastMessage.getContent() != null) {
                    // Check for exact text match (for plain text messages)
                    if (lastMessage.getContent().equals(userMessageContent)) {
                        foundCurrentUserMessage = true;
                    }
                    // Check for multimodal content that contains the text prompt
                    else if ("multipart/mixed".equals(lastMessage.getContentType()) && 
                             lastMessage.getContent() instanceof String) {
                        String multimodalContent = (String) lastMessage.getContent();
                        // Check if the multimodal content contains the text prompt
                        if (multimodalContent.contains("\"text\":\"" + userMessageContent + "\"") ||
                            multimodalContent.contains("\"text\": \"" + userMessageContent + "\"")) {
                            foundCurrentUserMessage = true;
                            logger.debug("Found current user message as part of multimodal content in chat {}", chatId);
                        }
                    }
                }
            }
            if (!foundCurrentUserMessage) {
                logger.warn("Current user message not found in conversation history. Saving it to database and adding to LLM context for chat {}.", chatId);
                try {
                    Message userMessage = new Message("user", "text/plain", userMessageContent);
                    chatService.addMessageToChat(chatId, userMessage);
                    logger.info("Saved missing user message to chat {} database", chatId);
                    
                    // Initialize with default constructor then set properties individually
                    ChatMessage tempUserMessage = new ChatMessage();
                    tempUserMessage.setRole("user");
                    tempUserMessage.setContentType("text/plain");
                    tempUserMessage.setContent(userMessageContent);
                    tempUserMessage.setTimestamp(Instant.now());
                    conversationHistory.add(tempUserMessage);
                } catch (Exception e) {
                    logger.error("Failed to save user message to database for chat {}: {}", chatId, e.getMessage(), e);
                    // Initialize with default constructor then set properties individually
                    ChatMessage tempUserMessage = new ChatMessage();
                    tempUserMessage.setRole("user");
                    tempUserMessage.setContentType("text/plain");
                    tempUserMessage.setContent(userMessageContent);
                    tempUserMessage.setTimestamp(Instant.now());
                    conversationHistory.add(tempUserMessage); // Add to history for LLM even if DB save fails
                }
            }
              logger.info("Streaming message for chat ID: {}. History size: {}", chatId, conversationHistory.size());
            if (logger.isDebugEnabled()) {
                for (int i = 0; i < conversationHistory.size(); i++) {
                    ChatMessage msg = conversationHistory.get(i);
                    Object content = msg.getContent();
                    String contentPreview = "null";
                    if (content instanceof String) {
                        contentPreview = ((String)content).substring(0, Math.min(50, ((String)content).length())) + "...";
                    } else if (content != null) {
                        contentPreview = "Non-string content: " + content.getClass().getSimpleName();
                    }
                    logger.debug("History [{} for chat {}] - Role: {}, Content: {}", i, chatId, msg.getRole(), contentPreview);
                }
            }

            if (conversationHistory.isEmpty()) {
                logger.error("Conversation history for chat {} is empty after attempting to prepare it. This should not happen.", chatId);
                return Flux.just("{\"error\": \"No conversation history found or could be prepared. Please ensure the user message is saved before streaming.\"}");
            }
            
            // Optimize token usage by converting multimodal content to text-only
            // This is crucial to prevent sending base64 images repeatedly in the chat context
            boolean hasMultimodalContent = conversationHistory.stream()
                .anyMatch(msg -> "multipart/mixed".equals(msg.getContentType()));
                
            if (hasMultimodalContent) {
                logger.info("Detected multimodal content in chat history - optimizing context by converting to text-only");
                List<Message> optimizedHistory = multimodalContentProcessingService.convertToTextOnlyHistory(
                    conversationHistory, 
                    -1  // Convert ALL messages to text-only for streaming
                );
                
                // Replace the conversation history with optimized version
                conversationHistory = optimizedHistory.stream()
                    .map(msg -> {
                        String content = (msg.getContent() instanceof String) 
                            ? (String) msg.getContent() 
                            : msg.getContent().toString();
                        return new ChatMessage(msg.getRole(), content);
                    })
                    .toList();
                
                logger.info("Using optimized text-only history with {} messages for streaming", conversationHistory.size());
            }

            StringBuilder responseAggregator = new StringBuilder();
            
            Flux<String> openAiFlux = openAIService.getChatCompletionStreamWithToolExecution(conversationHistory, modelToUse)
                    .doOnNext(responseAggregator::append)
                    .doOnError(e -> logger.error("Error during AI stream for chat {}: {}", chatId, e.getMessage(), e)); // Log OpenAI specific errors

            return Flux.concat(openAiFlux, Flux.defer(() -> {
                String fullResponse = responseAggregator.toString();
                String filteredResponse = ContentFilterUtil.filterForDatabase(fullResponse); // Changed to filterForDatabase
                boolean effectivelyEmpty = filteredResponse.isEmpty();
                boolean wasOriginallyContent = !fullResponse.isEmpty(); // Check if there was any content before filtering

                if (autoSaveResponse) {
                    if (effectivelyEmpty && wasOriginallyContent) { // Only error if there was content and it all got filtered out
                        logger.info("AI response for chat {} was empty after filtering tool-related content. Not saving. Signaling error to frontend.", chatId);
                        return Flux.error(new RuntimeException("AI response was empty after filtering tool-related content."));                    } else if (!effectivelyEmpty) { // Save only if there's something to save
                        try {
                            Message agentMessage = new Message("agent", "text/plain", filteredResponse);
                            agentMessage.setLlmId(modelToUse); // Set the LLM ID
                            
                            // Save both filtered and raw content
                            Chat updatedChat = chatService.addMessageToChat(chatId, agentMessage);
                            
                            // Now update the last message with raw content
                            if (updatedChat != null && !updatedChat.getMessages().isEmpty()) {
                                ChatMessage lastMessage = updatedChat.getMessages().get(updatedChat.getMessages().size() - 1);
                                if (lastMessage != null && "agent".equals(lastMessage.getRole())) {
                                    chatService.updateMessageRawContent(lastMessage.getId(), fullResponse);
                                    logger.info("Saved agent response for chat {} with LLM: {} (filtered: {} chars, raw: {} chars)", 
                                              chatId, modelToUse, filteredResponse.length(), fullResponse.length());
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error saving streamed response to chat history for chat ID {}: {}", chatId, e.getMessage(), e);
                            // Optionally, convert this to a stream error if critical
                            // return Flux.error(new RuntimeException("Failed to save AI response: " + e.getMessage()));
                        }
                    } else {
                        // If effectivelyEmpty is true AND wasOriginallyContent is false, it means the AI produced no output at all.
                        // This is different from producing only tool calls. We might not want to error here, or handle it differently.
                        logger.info("AI response for chat {} was completely empty (no tool calls, no text). Not saving.", chatId);
                    }
                } else { // autoSaveResponse is false
                    logger.info("Auto-save disabled for chat {}. Response will not be saved by backend.", chatId);
                    if (effectivelyEmpty && wasOriginallyContent) {
                        logger.info("AI response for chat {} (auto-save off) consisted only of tool-related content, which was filtered out. Signaling error.", chatId);
                        return Flux.error(new RuntimeException("AI response consisted only of tool-related content, which was filtered out."));
                    }
                }
                return Flux.empty(); // Normal completion if no error condition met
            }))
            .onErrorResume(e -> {
                logger.error("Error in stream processing for chat {}: {}", chatId, e.getMessage(), e);
                String errorMessageValue;
                if (e.getMessage() != null &&
                    (e.getMessage().contains("AI response was empty after filtering tool-related content.") ||
                     e.getMessage().contains("AI response consisted only of tool-related content, which was filtered out."))) {
                    errorMessageValue = e.getMessage().replace("\"", "\\\"");
                } else {
                    errorMessageValue = "Error processing your request: " + (e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error");
                }
                return Flux.just("{\"error\": \"" + errorMessageValue + "\"}");
            });

        } catch (Exception e) {
            logger.error("Error initiating stream for chat {}: {}", chatId, e.getMessage(), e);
            return Flux.just("{\"error\": \"Error initiating stream: " + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }    /**
     * Updates the title of an existing chat session.
     * 
     * <p>Allows modification of the chat's display title for better organization
     * and identification in user interfaces. This is typically used when users
     * want to rename conversations for easier reference.</p>
     * 
     * <h3>Request Format:</h3>
     * <p>Expects JSON object with "title" field containing the new title string.</p>
     * 
     * <h3>Validation Rules:</h3>
     * <ul>
     * <li>Title cannot be null or empty</li>
     * <li>Title should be trimmed of whitespace</li>
     * <li>Chat must exist in the system</li>
     * </ul>
     * 
     * @param chatId The unique identifier of the chat to update
     * @param request Map containing the new title in "title" field
     * @return ResponseEntity with JSON-RPC response containing updated chat or error
     * @throws IllegalArgumentException if title is invalid
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
    }    /**
     * Deletes a chat session and all associated data.
     * 
     * <p>Permanently removes a chat session including all messages, metadata,
     * and conversation history. This operation is irreversible and should be
     * used with caution.</p>
     * 
     * <h3>Deletion Process:</h3>
     * <ol>
     * <li>Validates chat existence</li>
     * <li>Removes all associated chat messages</li>
     * <li>Deletes chat metadata and session data</li>
     * <li>Returns success confirmation</li>
     * </ol>
     * 
     * <h3>Data Impact:</h3>
     * <ul>
     * <li>All conversation history is permanently lost</li>
     * <li>Chat metadata and timestamps are removed</li>
     * <li>Any references to the chat become invalid</li>
     * </ul>
     * 
     * @param chatId The unique identifier of the chat to delete
     * @return ResponseEntity with JSON-RPC response containing success confirmation or error
     * @throws RuntimeException if chat is not found
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
    }    /**
     * Retrieves all messages for a specific chat session.
     * 
     * <p>Fetches the complete conversation history for a chat, including all user
     * and AI messages in chronological order. Additionally marks the chat as read
     * to update the unread status for UI purposes.</p>
     * 
     * <h3>Response Content:</h3>
     * <ul>
     * <li>Complete message history in chronological order</li>
     * <li>Message metadata including timestamps and roles</li>
     * <li>Content in original format and type</li>
     * </ul>
     * 
     * <h3>Side Effects:</h3>
     * <ul>
     * <li>Marks the chat as read (updates unread status)</li>
     * <li>Updates last accessed timestamp</li>
     * </ul>
     * 
     * @param chatId The unique identifier of the chat
     * @return ResponseEntity with JSON-RPC response containing message list or error
     * @throws RuntimeException if chat is not found
     * @see #addMessageToChat(String, Message) for adding new messages
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
    }    /**
     * Retrieves the raw content of a specific message.
     * 
     * <p>Fetches the unfiltered LLM output for a message, including think tags
     * and other internal reasoning content that was filtered out before database storage.
     * This is useful for debugging, transparency, and understanding the full LLM response.</p>
     * 
     * <h3>Response Content:</h3>
     * <ul>
     * <li>Raw LLM output with think tags</li>
     * <li>Tool execution messages</li>
     * <li>Internal reasoning content</li>
     * </ul>
     * 
     * @param chatId The unique identifier of the chat
     * @param messageId The unique identifier of the message
     * @return ResponseEntity with JSON-RPC response containing raw content or error
     */
    @GetMapping("/{chatId}/messages/{messageId}/raw")
    public ResponseEntity<Map<String, Object>> getMessageRawContent(
            @PathVariable String chatId, 
            @PathVariable String messageId) {
        try {
            Optional<ChatMessage> messageOpt = chatService.getChatMessages(chatId)
                    .stream()
                    .filter(msg -> msg.getId().equals(messageId))
                    .findFirst();
            
            if (messageOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse(-32602, "Message not found with ID: " + messageId + " in chat: " + chatId));
            }
            
            ChatMessage message = messageOpt.get();
            String rawContent = message.getRawContent();
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", messageId);
            result.put("rawContent", rawContent != null ? rawContent : message.getContent());
            result.put("filteredContent", message.getContent());
            result.put("hasRawContent", rawContent != null);
            response.put("result", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving raw content for message {} in chat {}: {}", messageId, chatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(-32000, "Error retrieving raw content: " + e.getMessage()));
        }
    }

    /**
     * Test endpoint for administrative users.
     * 
     * <p>Security-protected endpoint that requires ADMIN role authentication.
     * Used for testing role-based access control and administrative functionality.</p>
     * 
     * <h3>Security Requirements:</h3>
     * <ul>
     * <li>User must be authenticated</li>
     * <li>User must have 'ADMIN' role</li>
     * <li>Access is enforced via Spring Security's @PreAuthorize</li>
     * </ul>
     * 
     * @return ResponseEntity with welcome message for administrators
     * @throws AccessDeniedException if user lacks ADMIN role
     */
    @GetMapping("/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminTestEndpoint() {
        return ResponseEntity.ok("Welcome, Admin! This is a restricted endpoint.");
    }    /**
     * Test endpoint for regular users.
     * 
     * <p>Security-protected endpoint that requires USER role authentication.
     * Used for testing role-based access control and user-level functionality.</p>
     * 
     * <h3>Security Requirements:</h3>
     * <ul>
     * <li>User must be authenticated</li>
     * <li>User must have 'USER' role</li>
     * <li>Access is enforced via Spring Security's @PreAuthorize</li>
     * </ul>
     * 
     * @return ResponseEntity with welcome message for users
     * @throws AccessDeniedException if user lacks USER role
     */
    @GetMapping("/user/test")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> userTestEndpoint() {
        return ResponseEntity.ok("Welcome, User! This is an endpoint for users.");
    }

    /**
     * Creates standardized JSON-RPC error response objects.
     * 
     * <p>Utility method for generating consistent error responses following
     * JSON-RPC 2.0 specification. All error responses include a code and
     * descriptive message for client-side error handling.</p>
     * 
     * @param code Error code (typically negative integers following JSON-RPC conventions)
     * @param message Descriptive error message for debugging and user feedback
     * @return Map containing structured error response with "error" field
     */
    private Map<String, Object> createErrorResponse(int code, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", Map.of(
            "code", code,
            "message", message        ));
        return errorResponse;
    }
    
    /**
     * Helper method to properly extract and compare text content from multimodal JSON.
     * Uses ObjectMapper for robust JSON parsing instead of string matching.
     */
    private boolean extractTextFromMultimodalJson(String multimodalJson, String targetText) {
        try {
            // Parse the multimodal JSON using ObjectMapper
            Object[] contentArray = objectMapper.readValue(multimodalJson, Object[].class);
            
            for (Object item : contentArray) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    String type = (String) itemMap.get("type");
                    
                    if ("text".equals(type)) {
                        String text = (String) itemMap.get("text");
                        if (text != null && text.equals(targetText)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("Error parsing multimodal JSON for text extraction: {}", e.getMessage());
            // Fallback to simple string contains check
            return multimodalJson.contains("\"text\":\"" + targetText + "\"") ||
                   multimodalJson.contains("\"text\": \"" + targetText + "\"");
        }
    }
}
