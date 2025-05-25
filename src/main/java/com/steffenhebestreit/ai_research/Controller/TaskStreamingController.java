package com.steffenhebestreit.ai_research.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Model.Task;
import com.steffenhebestreit.ai_research.Service.TaskService;
import com.steffenhebestreit.ai_research.Service.TaskUpdateListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class TaskStreamingController implements TaskUpdateListener {

    private final TaskService taskService;
    private final ObjectMapper objectMapper;
    private final Map<String, SseEmitter> taskEmitters = new ConcurrentHashMap<>();
    private final Map<String, String> requestIds = new ConcurrentHashMap<>();

    public TaskStreamingController(TaskService taskService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
        // Register this controller as a listener with the task service
        this.taskService.registerTaskUpdateListener(this);
    }

    @PostMapping("/tasks/resubscribe")
    public SseEmitter resubscribe(@RequestBody Map<String, Object> payload) {
        String taskId = (String) payload.get("taskId");
        String requestId = (String) payload.get("id"); // Extract JSON-RPC request ID

        if (taskId == null) {
            throw new IllegalArgumentException("taskId is required");
        }

        // Check if task exists
        Task task = taskService.getTask(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        // Store the request ID for this task to use in responses
        if (requestId != null) {
            requestIds.put(taskId, requestId);
        }

        // Create a new emitter for this task
        SseEmitter emitter = createEmitterForTask(taskId);

        // Immediately send the current state to the client as a JSON-RPC response
        try {
            Map<String, Object> response = createJsonRpcResponse(requestId, createTaskStatusUpdateEvent(task));
            emitter.send(SseEmitter.event()
                    .name("task_status_update")
                    .data(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }    @PostMapping("/message/stream")
    public SseEmitter streamMessages(@RequestBody Map<String, Object> request) {
        // Correctly extract parameters from the nested structure
        Object paramsObj = request.get("params");
        if (!(paramsObj instanceof Map)) {
            throw new IllegalArgumentException("Missing or invalid params in request");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) paramsObj;
        
        String taskId = (String) params.get("taskId");
        Object messageObj = params.get("message");
        String requestId = (String) request.get("id"); // Extract JSON-RPC request ID

        if (taskId == null) {
            throw new IllegalArgumentException("taskId is required");
        }
        
        if (!(messageObj instanceof Map)) {
            throw new IllegalArgumentException("message is required and must be an object");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) messageObj;

        // Store the request ID for this task to use in responses
        if (requestId != null) {
            requestIds.put(taskId, requestId);
        }

        // Process the message normally
        taskService.processStreamingMessage(taskId, messageMap);

        // Return an emitter for streaming updates
        return createEmitterForTask(taskId);
    }    // Helper method to create and register an emitter
    private SseEmitter createEmitterForTask(String taskId) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout

        // Remove completed or timed out emitters
        emitter.onCompletion(() -> taskEmitters.remove(taskId));
        emitter.onTimeout(() -> taskEmitters.remove(taskId));
        emitter.onError(e -> {
            // Log error if needed
            System.err.println("SSE error for task " + taskId + ": " + e.getMessage());
            taskEmitters.remove(taskId);
        });

        // Store the emitter for this task
        taskEmitters.put(taskId, emitter);

        return emitter;
    }    // Implementation of TaskUpdateListener interface
    @Override
    public void sendTaskUpdate(String taskId, Object update, String eventType) {
        SseEmitter emitter = taskEmitters.get(taskId);
        if (emitter != null) {
            try {
                // Get the stored request ID for this task
                String requestId = requestIds.getOrDefault(taskId, "1");

                // Create a proper JSON-RPC 2.0 response
                Map<String, Object> jsonRpcResponse;

                if (eventType.equals("task_status_update")) {
                    jsonRpcResponse = createJsonRpcResponse(requestId, createTaskStatusUpdateEvent(update));
                } else if (eventType.equals("artifact_update")) {
                    jsonRpcResponse = createJsonRpcResponse(requestId, createArtifactUpdateEvent(taskId, update));
                } else {
                    jsonRpcResponse = createJsonRpcResponse(requestId, update);
                }

                // Send as SSE event
                String responseJson = objectMapper.writeValueAsString(jsonRpcResponse);
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(responseJson, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitter.completeWithError(e);
                taskEmitters.remove(taskId);
            }
        }
    }

    // Helper method to create a JSON-RPC 2.0 response object
    private Map<String, Object> createJsonRpcResponse(String id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id != null ? id : "1");
        response.put("result", result);
        return response;
    }    // Helper method to create a TaskStatusUpdateEvent
    private Map<String, Object> createTaskStatusUpdateEvent(Object taskOrStatus) {
        Map<String, Object> event = new HashMap<>();
        Task task;

        if (taskOrStatus instanceof Task) {
            task = (Task) taskOrStatus;
            event.put("taskId", task.getId());
            event.put("contextId", task.getContextId());  // Assuming Task has contextId
            event.put("status", task.getStatus());
        } else if (taskOrStatus instanceof Map) {
            // Safely cast and copy the map data
            @SuppressWarnings("unchecked")
            Map<String, Object> statusMap = (Map<String, Object>) taskOrStatus;
            event.putAll(statusMap);
        } else {
            // Handle other cases or null values
            event.put("status", "UNKNOWN");
        }

        event.put("kind", "status-update");
        return event;
    }

    // Helper method to create an ArtifactUpdateEvent
    private Map<String, Object> createArtifactUpdateEvent(String taskId, Object artifact) {
        Map<String, Object> event = new HashMap<>();
        event.put("taskId", taskId);
        event.put("artifact", artifact);
        event.put("kind", "artifact-update");
        event.put("append", false);  // Default values, can be overridden
        event.put("lastChunk", false);
        return event;
    }
}
