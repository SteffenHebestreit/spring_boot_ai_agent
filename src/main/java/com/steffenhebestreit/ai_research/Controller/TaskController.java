package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Model.Task;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import com.steffenhebestreit.ai_research.Service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing AI research tasks and chat interactions.
 * 
 * <p>This controller provides comprehensive task management capabilities including task creation,
 * message handling, task cancellation, and both synchronous and streaming chat interactions.
 * It serves as the primary interface for task-related operations in the AI research system.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>Task Management:</strong> Creation, retrieval, cancellation of research tasks</li>
 * <li><strong>Message Processing:</strong> Adding messages to existing tasks and managing conversation flow</li>
 * <li><strong>Chat Integration:</strong> Simple chat endpoint for direct AI interactions</li>
 * <li><strong>Streaming Support:</strong> Real-time streaming chat responses for enhanced user experience</li>
 * </ul>
 * 
 * <h3>Integration Points:</h3>
 * <ul>
 * <li>{@link TaskService} - Core business logic for task operations</li>
 * <li>{@link OpenAIService} - AI model integration for generating responses</li>
 * <li>{@link ChatService} - Chat session management and message persistence</li>
 * </ul>
 * 
 * <h3>API Patterns:</h3>
 * <p>All endpoints follow JSON-RPC 2.0 style response format with either a "result" field
 * for successful operations or an "error" field containing error code and message for failures.</p>
 * 
 * <h3>Error Handling:</h3>
 * <p>Implements comprehensive error handling with appropriate HTTP status codes:
 * <ul>
 * <li>404 (NOT_FOUND) - When requested resources don't exist</li>
 * <li>400 (BAD_REQUEST) - For invalid input parameters</li>
 * <li>500 (INTERNAL_SERVER_ERROR) - For unexpected system errors</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see TaskService
 * @see ChatService
 * @see OpenAIService
 * @see MultimodalController
 */

/**
 * REST Controller for managing AI research tasks and chat interactions.
 * 
 * <p>This controller provides comprehensive task management capabilities including task creation,
 * message handling, task cancellation, and both synchronous and streaming chat interactions.
 * It serves as the primary interface for task-related operations in the AI research system.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>Task Management:</strong> Creation, retrieval, cancellation of research tasks</li>
 * <li><strong>Message Processing:</strong> Adding messages to existing tasks and managing conversation flow</li>
 * <li><strong>Chat Integration:</strong> Simple chat endpoint for direct AI interactions</li>
 * <li><strong>Streaming Support:</strong> Real-time streaming chat responses for enhanced user experience</li>
 * </ul>
 * 
 * <h3>Integration Points:</h3>
 * <ul>
 * <li>{@link TaskService} - Core business logic for task operations</li>
 * <li>{@link OpenAIService} - AI model integration for generating responses</li>
 * <li>{@link ChatService} - Chat session management and message persistence</li>
 * <li>{@link LlmCapabilityService} - LLM capability checking and validation</li>
 * </ul>
 * 
 * <h3>API Patterns:</h3>
 * <p>All endpoints follow JSON-RPC 2.0 style response format with either a "result" field
 * for successful operations or an "error" field containing error code and message for failures.</p>
 * 
 * <h3>Error Handling:</h3>
 * <p>Implements comprehensive error handling with appropriate HTTP status codes:
 * <ul>
 * <li>404 (NOT_FOUND) - When requested resources don't exist</li>
 * <li>400 (BAD_REQUEST) - For invalid input parameters</li>
 * <li>500 (INTERNAL_SERVER_ERROR) - For unexpected system errors</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see TaskService
 * @see ChatService
 * @see OpenAIService
 * @see MultimodalController
 */
@RestController
@RequestMapping("/research-agent/api")
public class TaskController {
      private final TaskService taskService;
    private final OpenAIService openAIService;
    private final ChatService chatService;
    
    @Value("${openai.api.model}")
    private String defaultLlmId;

    /**
     * Constructs a TaskController with required service dependencies.
     * 
     * @param taskService Service for task management operations
     * @param openAIService Service for AI model interactions
     * @param chatService Service for chat session management
     */
    public TaskController(TaskService taskService, OpenAIService openAIService, ChatService chatService) {
        this.taskService = taskService;
        this.openAIService = openAIService;
        this.chatService = chatService;
    }    /**
     * Simple synchronous chat endpoint for direct AI interactions.
     * 
     * <p>Creates a new chat session with the provided user message, generates an AI response,
     * and saves both messages to the chat history. This endpoint provides immediate response
     * without streaming capabilities.</p>
     * 
     * <h3>Process Flow:</h3>
     * <ol>
     * <li>Creates new chat with user message</li>
     * <li>Generates AI response via OpenAI service</li>
     * <li>Saves AI response to chat history</li>
     * <li>Returns complete AI response</li>
     * </ol>
     * 
     * @param userMessage The user's input message as plain text
     * @return ResponseEntity containing the AI's response text
     * @throws IllegalArgumentException if userMessage is null or empty
     * @see #chatStream(String) for streaming alternative
     */
    @PostMapping(value = "/chat", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> chat(@RequestBody String userMessage) {
        try {
            // Create a new chat with the user message
            Message userMsg = new Message("user", "text/plain", userMessage);
            Chat chat = chatService.createChat(userMsg);
            
            // Get AI response
            String aiResponse = openAIService.getChatCompletion(userMessage);
            
            // Add the AI response to the chat history
            Message agentMsg = new Message("agent", "text/plain", aiResponse);
            chatService.addMessageToChat(chat.getId(), agentMsg);
            
            return ResponseEntity.ok(aiResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing your request.");
        }
    }    
    /**
     * Streaming chat endpoint for real-time AI interactions.
     * 
     * <p>Provides real-time streaming responses from the AI model using reactive streams.
     * This endpoint creates a new chat session and streams the AI response as it's generated,
     * providing a more interactive user experience compared to the synchronous chat endpoint.</p>
     * 
     * <h3>Technical Implementation:</h3>
     * <ul>
     * <li>Uses Reactor Flux for reactive streaming</li>
     * <li>Produces NDJSON (Newline Delimited JSON) for stream parsing</li>
     * <li>Aggregates response chunks for persistence</li>
     * <li>Automatically saves complete response to chat history on completion</li>
     * </ul>
     * 
     * <h3>Error Handling:</h3>
     * <p>Implements comprehensive error handling with stream-safe error responses:
     * <ul>
     * <li>Input validation errors return error flux</li>
     * <li>Stream processing errors are logged and replaced with user-friendly messages</li>
     * <li>Persistence errors during completion are logged but don't interrupt the stream</li>
     * </ul>
     * 
     * @param userMessage The user's input message as plain text
     * @return Flux&lt;String&gt; streaming AI response chunks
     * @throws IllegalArgumentException if userMessage is null or empty
     * @see #chat(String) for synchronous alternative
     */
    @PostMapping(value = "/chat-stream", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStream(@RequestBody String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("User message cannot be empty"));
        }
        try {
            // Create a new chat with the user message
            Message userMsg = new Message("user", "text/plain", userMessage);
            Chat chat = chatService.createChat(userMsg);
            
            // Get the conversation history (which will have the user message we just added)
            List<ChatMessage> conversationHistory = chatService.getChatMessages(chat.getId());
            
            // StringBuilder to accumulate the response
            StringBuilder responseAggregator = new StringBuilder();

            // Get and return the streaming response
            return openAIService.getChatCompletionStream(conversationHistory)
                    .doOnNext(responseAggregator::append) // Append each chunk to the aggregator
                    .doOnComplete(() -> {
                        // Save the completed response to chat history
                        try {
                            String fullResponse = responseAggregator.toString();
                            if (chat != null && !fullResponse.isEmpty()) { // Check chat != null
                                Message agentMsg = new Message("agent", "text/plain", fullResponse);
                                chatService.addMessageToChat(chat.getId(), agentMsg);
                            }
                        } catch (Exception e) {
                            // Log error more visibly or handle appropriately
                            System.err.println("Error saving streamed response to chat history for chat ID " + (chat != null ? chat.getId() : "unknown") + ": " + e.getMessage());
                            e.printStackTrace(); // For more detailed logging
                        }
                    })
                    .onErrorResume(e -> {
                        // Log error and return a user-friendly message in the stream
                        System.err.println("Error during chat stream processing: " + e.getMessage());
                        e.printStackTrace(); // For more detailed logging
                        return Flux.just("Error processing your request: An internal error occurred.");
                    });
        } catch (Exception e) {
            // Log error and return a user-friendly message in the stream
            System.err.println("Error initiating chat stream: " + e.getMessage());
            e.printStackTrace(); // For more detailed logging
            return Flux.just("Error initiating stream: An internal error occurred.");
        }
    }    /**
     * Creates a new research task with an initial message.
     * 
     * <p>Initializes a new task in the system with the provided message as the starting point.
     * The task is created in PENDING status and can be further developed through additional
     * message operations.</p>
     * 
     * <h3>Task Creation Process:</h3>
     * <ol>
     * <li>Validates the initial message content</li>
     * <li>Creates new task entity with generated UUID</li>
     * <li>Sets initial task status to PENDING</li>
     * <li>Associates the initial message with the task</li>
     * <li>Persists task to database</li>
     * </ol>
     * 
     * @param initialMessage The initial message to start the task with
     * @return ResponseEntity with JSON-RPC response containing the created task
     * @throws IllegalArgumentException if initialMessage is invalid
     */
    @PostMapping("/tasks/create")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Message initialMessage) {
        Task task = taskService.createTask(initialMessage);
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }    /**
     * Retrieves a specific task by its unique identifier.
     * 
     * <p>Fetches complete task details including all associated messages and current status.
     * Returns a 404 error if the task doesn't exist in the system.</p>
     * 
     * <h3>Response Format:</h3>
     * <p>Success: JSON-RPC response with "result" containing the complete task object</p>
     * <p>Failure: JSON-RPC error response with code -32602 and descriptive message</p>
     * 
     * @param taskId The unique identifier of the task to retrieve
     * @return ResponseEntity with JSON-RPC response containing the task or error details
     * @throws IllegalArgumentException if taskId is invalid format
     */
    @GetMapping("/tasks/{taskId}/get")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        Task task = taskService.getTask(taskId);
        
        if (task == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("code", -32602);
            errorDetails.put("message", "Task not found with ID: " + taskId);
            errorResponse.put("error", errorDetails);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.ok(response);
    }    /**
     * Adds a new message to an existing task.
     * 
     * <p>Appends a message to the conversation history of an existing task. This is used to
     * continue the conversation flow within a task context, allowing for multi-turn interactions.</p>
     * 
     * <h3>Request Format:</h3>
     * <p>Expects a JSON object with:
     * <ul>
     * <li><code>taskId</code> - String identifier of the target task</li>
     * <li><code>message</code> - Message object with role, contentType, and content fields</li>
     * </ul>
     * 
     * <h3>Message Processing:</h3>
     * <ol>
     * <li>Validates taskId and message structure</li>
     * <li>Constructs Message object from request data</li>
     * <li>Associates message with existing task</li>
     * <li>Updates task's message history</li>
     * <li>Returns updated task object</li>
     * </ol>
     * 
     * @param request Map containing taskId and message data
     * @return ResponseEntity with JSON-RPC response containing updated task or error
     * @throws IllegalArgumentException if request format is invalid
     */
    @PostMapping("/message/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        String taskId = (String) request.get("taskId");
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) request.get("message");
        
        Message message = new Message();
        message.setRole((String) messageMap.get("role"));
        message.setContentType((String) messageMap.get("contentType"));
        message.setContent((String) messageMap.get("content"));
        
        Task task = taskService.addMessageToTask(taskId, message);
        
        if (task == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("code", -32602);
            errorDetails.put("message", "Task not found with ID: " + taskId);
            errorResponse.put("error", errorDetails);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.ok(response);
    }    
    /**
     * Cancels an existing task by updating its status.
     * 
     * <p>Marks a task as cancelled, preventing further processing. The task and its message
     * history are preserved but the task status is updated to indicate cancellation.</p>
     * 
     * <h3>Cancellation Process:</h3>
     * <ol>
     * <li>Validates task existence</li>
     * <li>Updates task status to CANCELLED</li>
     * <li>Sets cancellation reason and timestamp</li>
     * <li>Persists status change</li>
     * <li>Returns updated task object</li>
     * </ol>
     * 
     * <h3>Business Rules:</h3>
     * <ul>
     * <li>Only active tasks can be cancelled</li>
     * <li>Cancelled tasks preserve all existing data</li>
     * <li>Cancellation is irreversible through this API</li>
     * </ul>
     * 
     * @param taskId The unique identifier of the task to cancel
     * @return ResponseEntity with JSON-RPC response containing cancelled task or error
     * @throws IllegalArgumentException if taskId is invalid format
     */
    @PostMapping("/tasks/{taskId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        Task task = taskService.cancelTask(taskId);
        
        if (task == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("code", -32602);
            errorDetails.put("message", "Task not found with ID: " + taskId);
            errorResponse.put("error", errorDetails);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.ok(response);
    }
      // The multimodal endpoints have been moved to MultimodalController
    // to avoid ambiguous mapping conflicts and better organize the application
}
