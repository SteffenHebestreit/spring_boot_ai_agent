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

/**
 * REST Controller for real-time task streaming and Server-Sent Events (SSE).
 * 
 * <p>This controller implements real-time communication for task updates using Server-Sent Events
 * and JSON-RPC 2.0 protocol. It provides streaming capabilities for task status updates, message
 * processing, and artifact notifications, enabling responsive real-time user interfaces.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>SSE Management:</strong> Creates and manages persistent Server-Sent Event connections</li>
 * <li><strong>Task Streaming:</strong> Real-time streaming of task status and progress updates</li>
 * <li><strong>Message Processing:</strong> Handles streaming message operations with live feedback</li>
 * <li><strong>Event Broadcasting:</strong> Distributes task updates to connected clients</li>
 * </ul>
 * 
 * <h3>Technical Architecture:</h3>
 * <ul>
 * <li><strong>SSE Emitters:</strong> Manages concurrent map of active streaming connections</li>
 * <li><strong>JSON-RPC 2.0:</strong> Implements standardized request/response protocol</li>
 * <li><strong>Event Listener:</strong> Implements TaskUpdateListener for service integration</li>
 * <li><strong>Connection Management:</strong> Handles emitter lifecycle and cleanup</li>
 * </ul>
 * 
 * <h3>Supported Event Types:</h3>
 * <ul>
 * <li><code>task_status_update</code> - Task status changes and progress updates</li>
 * <li><code>artifact_update</code> - Task artifact creation and modification events</li>
 * <li><code>message_stream</code> - Real-time message processing events</li>
 * </ul>
 * 
 * <h3>Connection Management:</h3>
 * <p>The controller maintains persistent SSE connections using SseEmitter with automatic cleanup
 * on completion, timeout, or error. Each task can have one active emitter at a time, with
 * resubscription capabilities for connection recovery.</p>
 * 
 * <h3>Error Handling:</h3>
 * <ul>
 * <li>Connection errors trigger automatic cleanup and emitter removal</li>
 * <li>Invalid requests return appropriate error responses</li>
 * <li>Stream processing errors are logged and communicated to clients</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see TaskService
 * @see TaskUpdateListener
 * @see SseEmitter
 */
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
    }    /**
     * Reestablishes Server-Sent Event connection for task streaming.
     * 
     * <p>Creates a new SSE emitter for an existing task and immediately sends the current
     * task status as the initial event. This endpoint is used for connection recovery,
     * page refreshes, or initial subscription to task updates.</p>
     * 
     * <h3>Process Flow:</h3>
     * <ul>
     * <li>Validates the provided taskId parameter</li>
     * <li>Verifies task existence in the system</li>
     * <li>Stores JSON-RPC request ID for response correlation</li>
     * <li>Creates new SSE emitter with automatic lifecycle management</li>
     * <li>Sends immediate task status update as JSON-RPC 2.0 response</li>
     * <li>Returns emitter for continued streaming</li>
     * </ul>
     * 
     * <h3>Request Format:</h3>
     * <p>Expects JSON payload with taskId and optional JSON-RPC id:</p>
     * <pre>
     * {
     *   "taskId": "task-uuid",
     *   "id": "request-id"
     * }
     * </pre>
     * 
     * <h3>Response Events:</h3>
     * <p>Immediately sends task_status_update event with current task state,
     * followed by real-time updates as task progresses.</p>
     * 
     * @param payload Request payload containing taskId and optional request ID
     * @return SseEmitter for streaming task updates
     * @throws IllegalArgumentException if taskId is missing or task doesn't exist
     * @see #createEmitterForTask(String)
     * @see #sendTaskUpdate(String, Object, String)
     */
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
    }    /**
     * Processes streaming message requests and establishes SSE connection.
     * 
     * <p>This endpoint handles message processing requests with real-time streaming
     * capabilities. It extracts message data from JSON-RPC formatted requests,
     * initiates message processing through TaskService, and returns an SSE emitter
     * for receiving live updates during processing.</p>
     * 
     * <h3>Request Structure:</h3>
     * <p>Expects JSON-RPC 2.0 formatted request with nested parameters:</p>
     * <pre>
     * {
     *   "id": "request-id",
     *   "params": {
     *     "taskId": "task-uuid",
     *     "message": {
     *       "content": "message content",
     *       "role": "user"
     *     }
     *   }
     * }
     * </pre>
     * 
     * <h3>Processing Flow:</h3>
     * <ul>
     * <li>Validates request structure and extracts nested parameters</li>
     * <li>Validates required taskId and message object</li>
     * <li>Stores JSON-RPC request ID for response correlation</li>
     * <li>Initiates streaming message processing via TaskService</li>
     * <li>Creates SSE emitter for real-time update delivery</li>
     * <li>Returns emitter for client to receive processing updates</li>
     * </ul>
     * 
     * <h3>Streaming Events:</h3>
     * <p>During message processing, clients receive various event types including
     * message progress, intermediate results, and completion notifications.</p>
     * 
     * @param request JSON-RPC formatted request with taskId and message parameters
     * @return SseEmitter for streaming message processing updates
     * @throws IllegalArgumentException if required parameters are missing or invalid
     * @see TaskService#processStreamingMessage(String, Map)
     * @see #createEmitterForTask(String)
     */
    @PostMapping("/message/stream")
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
    }    /**
     * Creates and configures a new SSE emitter for task-specific streaming.
     * 
     * <p>This helper method creates a properly configured SseEmitter with automatic
     * lifecycle management, event handlers, and connection cleanup. Each task can
     * have one active emitter at a time, with previous emitters being replaced.</p>
     * 
     * <h3>Emitter Configuration:</h3>
     * <ul>
     * <li><strong>Timeout:</strong> No timeout (0L) for persistent connections</li>
     * <li><strong>Completion Handler:</strong> Automatic cleanup from taskEmitters map</li>
     * <li><strong>Timeout Handler:</strong> Removes emitter from active connections</li>
     * <li><strong>Error Handler:</strong> Logs errors and performs cleanup</li>
     * </ul>
     * 
     * <h3>Connection Management:</h3>
     * <ul>
     * <li>Replaces any existing emitter for the same taskId</li>
     * <li>Stores emitter in concurrent map for thread-safe access</li>
     * <li>Automatic removal on completion, timeout, or error</li>
     * <li>Error logging for debugging connection issues</li>
     * </ul>
     * 
     * <h3>Thread Safety:</h3>
     * <p>Uses ConcurrentHashMap for thread-safe emitter storage and removal,
     * ensuring proper handling of concurrent connections and updates.</p>
     * 
     * @param taskId The unique identifier of the task for emitter association
     * @return Configured SseEmitter ready for event streaming
     * @see ConcurrentHashMap
     * @see SseEmitter
     */
    // Helper method to create and register an emitter
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
    }    /**
     * Broadcasts task updates to connected SSE clients.
     * 
     * <p>Implementation of TaskUpdateListener interface method that handles real-time
     * distribution of task updates to connected clients. Formats updates according to
     * JSON-RPC 2.0 protocol and sends them as Server-Sent Events with appropriate
     * event types.</p>
     * 
     * <h3>Update Processing:</h3>
     * <ul>
     * <li>Retrieves active SSE emitter for the specified task</li>
     * <li>Formats update data according to event type requirements</li>
     * <li>Wraps updates in JSON-RPC 2.0 response structure</li>
     * <li>Sends formatted events via SSE with proper content type</li>
     * <li>Handles connection errors and emitter cleanup</li>
     * </ul>
     * 
     * <h3>Supported Event Types:</h3>
     * <ul>
     * <li><code>task_status_update</code> - Task status and progress changes</li>
     * <li><code>artifact_update</code> - Task artifact creation and modifications</li>
     * <li><code>custom events</code> - Other update types passed through directly</li>
     * </ul>
     * 
     * <h3>Error Handling:</h3>
     * <p>Connection errors trigger automatic emitter cleanup and removal from
     * active connections. Failed sends complete the emitter with error status.</p>
     * 
     * <h3>JSON-RPC Integration:</h3>
     * <p>Uses stored request IDs to correlate responses with original requests,
     * maintaining proper JSON-RPC 2.0 protocol compliance.</p>
     * 
     * @param taskId The unique identifier of the task being updated
     * @param update The update data to broadcast to connected clients
     * @param eventType The type of event for proper client-side handling
     * @see TaskUpdateListener
     * @see #createJsonRpcResponse(String, Object)
     * @see #createTaskStatusUpdateEvent(Object)
     * @see #createArtifactUpdateEvent(String, Object)
     */
    // Implementation of TaskUpdateListener interface
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
    }    /**
     * Creates a JSON-RPC 2.0 compliant response object.
     * 
     * <p>Utility method for constructing properly formatted JSON-RPC 2.0 response
     * objects that include protocol version, request correlation ID, and result data.
     * Used for maintaining protocol compliance in SSE communications.</p>
     * 
     * <h3>Response Structure:</h3>
     * <pre>
     * {
     *   "jsonrpc": "2.0",
     *   "id": "request-id",
     *   "result": { ... }
     * }
     * </pre>
     * 
     * <h3>Protocol Compliance:</h3>
     * <ul>
     * <li>Includes required "jsonrpc": "2.0" field</li>
     * <li>Correlates responses with original requests via ID</li>
     * <li>Defaults to "1" if no request ID provided</li>
     * <li>Wraps arbitrary result data in standard format</li>
     * </ul>
     * 
     * @param id The request ID for response correlation, defaults to "1" if null
     * @param result The result data to include in the response
     * @return Map containing JSON-RPC 2.0 formatted response object
     * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
     */
    // Helper method to create a JSON-RPC 2.0 response object
    private Map<String, Object> createJsonRpcResponse(String id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id != null ? id : "1");
        response.put("result", result);
        return response;
    }    /**
     * Creates a standardized task status update event object.
     * 
     * <p>Constructs event objects for task status updates that can be sent via SSE.
     * Handles both Task domain objects and status Map objects, extracting relevant
     * information and formatting it according to client expectations.</p>
     * 
     * <h3>Input Types:</h3>
     * <ul>
     * <li><strong>Task Object:</strong> Extracts id, contextId, and status fields</li>
     * <li><strong>Map Object:</strong> Copies all key-value pairs from status map</li>
     * <li><strong>Other/Null:</strong> Creates event with "UNKNOWN" status</li>
     * </ul>
     * 
     * <h3>Event Structure:</h3>
     * <pre>
     * {
     *   "taskId": "task-uuid",
     *   "contextId": "context-uuid",
     *   "status": "RUNNING|COMPLETED|FAILED|...",
     *   "kind": "status-update"
     * }
     * </pre>
     * 
     * <h3>Usage:</h3>
     * <p>Used by sendTaskUpdate method to format task status changes for
     * client consumption via Server-Sent Events.</p>
     * 
     * @param taskOrStatus Task object or status map to convert to event format
     * @return Map containing standardized task status update event data
     * @see Task
     * @see #sendTaskUpdate(String, Object, String)
     */
    // Helper method to create a TaskStatusUpdateEvent
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
    }    /**
     * Creates a standardized artifact update event object.
     * 
     * <p>Constructs event objects for task artifact updates that can be sent via SSE.
     * Formats artifact data with metadata fields required for proper client-side
     * handling of artifact creation, modification, and streaming updates.</p>
     * 
     * <h3>Event Structure:</h3>
     * <pre>
     * {
     *   "taskId": "task-uuid",
     *   "artifact": { ... },
     *   "kind": "artifact-update",
     *   "append": false,
     *   "lastChunk": false
     * }
     * </pre>
     * 
     * <h3>Metadata Fields:</h3>
     * <ul>
     * <li><strong>taskId:</strong> Associates artifact with specific task</li>
     * <li><strong>artifact:</strong> The actual artifact data or content</li>
     * <li><strong>kind:</strong> Event type identifier for client routing</li>
     * <li><strong>append:</strong> Whether to append to existing content (default: false)</li>
     * <li><strong>lastChunk:</strong> Indicates final chunk in streaming sequence (default: false)</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Used by sendTaskUpdate method when processing artifact_update events,
     * providing consistent structure for artifact notifications across the system.</p>
     * 
     * @param taskId The unique identifier of the task that owns the artifact
     * @param artifact The artifact data to include in the update event
     * @return Map containing standardized artifact update event data
     * @see #sendTaskUpdate(String, Object, String)
     */
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
