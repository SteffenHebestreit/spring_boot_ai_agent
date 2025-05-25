package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Model.Task;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import com.steffenhebestreit.ai_research.Service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/research-agent/api")
public class TaskController {

    private final TaskService taskService;
    private final OpenAIService openAIService;
    private final ChatService chatService;

    public TaskController(TaskService taskService, OpenAIService openAIService, ChatService chatService) {
        this.taskService = taskService;
        this.openAIService = openAIService;
        this.chatService = chatService;
    }    @PostMapping(value = "/chat", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
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
    }    @PostMapping(value = "/chat-stream", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
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
    }

    @PostMapping("/tasks/create")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Message initialMessage) {
        Task task = taskService.createTask(initialMessage);
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tasks/{taskId}/get")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        Task task = taskService.getTask(taskId);
        
        if (task == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", Map.of(
                "code", -32602,
                "message", "Task not found with ID: " + taskId
            ));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.ok(response);
    }

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
            errorResponse.put("error", Map.of(
                "code", -32602,
                "message", "Task not found with ID: " + taskId
            ));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        Task task = taskService.cancelTask(taskId);
        
        if (task == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", Map.of(
                "code", -32602,
                "message", "Task not found with ID: " + taskId
            ));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", task);
        
        return ResponseEntity.ok(response);
    }
}
