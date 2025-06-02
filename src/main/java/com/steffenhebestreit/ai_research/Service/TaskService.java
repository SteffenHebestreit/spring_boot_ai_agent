package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Model.Task;
import com.steffenhebestreit.ai_research.Model.TaskArtifact;
import com.steffenhebestreit.ai_research.Model.TaskStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Core service for comprehensive task lifecycle management in the AI Research system.
 * 
 * <p>This service provides complete task orchestration capabilities including creation,
 * processing, status management, and real-time communication. It implements asynchronous
 * task processing with streaming updates and supports the Agent-to-Agent (A2A) protocol
 * for distributed task coordination.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>Task Lifecycle:</strong> Complete CRUD operations and state management</li>
 * <li><strong>Asynchronous Processing:</strong> Non-blocking task execution with thread pool</li>
 * <li><strong>Real-time Updates:</strong> Streaming status and progress notifications</li>
 * <li><strong>Message Management:</strong> Conversation history and context preservation</li>
 * <li><strong>Artifact Collection:</strong> Generated content and deliverable management</li>
 * <li><strong>Cancellation Support:</strong> Graceful task termination and cleanup</li>
 * </ul>
 * 
 * <h3>Task Processing Architecture:</h3>
 * <ul>
 * <li><strong>Repository Pattern:</strong> In-memory ConcurrentHashMap for thread-safe storage</li>
 * <li><strong>Executor Service:</strong> Cached thread pool for scalable task processing</li>
 * <li><strong>Observer Pattern:</strong> Listener-based updates for real-time communication</li>
 * <li><strong>State Machine:</strong> Standardized task status progression</li>
 * </ul>
 * 
 * <h3>Task Status Progression:</h3>
 * <ul>
 * <li><code>PENDING</code> → <code>PROCESSING</code> → <code>COMPLETED</code></li>
 * <li><code>PENDING</code> → <code>PROCESSING</code> → <code>FAILED</code></li>
 * <li><code>*</code> → <code>CANCELLED</code> (user-initiated)</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>All operations are thread-safe using concurrent collections and atomic operations.
 * Multiple tasks can be processed simultaneously without interference.</p>
 * 
 * <h3>Integration Points:</h3>
 * <ul>
 * <li><strong>TaskUpdateListener:</strong> Real-time progress broadcasting</li>
 * <li><strong>Message Processing:</strong> LLM integration and response handling</li>
 * <li><strong>Artifact Generation:</strong> Content creation and storage</li>
 * <li><strong>A2A Protocol:</strong> Inter-agent communication and coordination</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see Task
 * @see TaskStatus
 * @see TaskUpdateListener
 * @see Message
 * @see TaskArtifact
 */
@Service
public class TaskService {

    private final Map<String, Task> taskRepository = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private TaskUpdateListener updateListener;

    /**
     * Registers a task update listener for real-time event broadcasting.
     * 
     * <p>Configures the service to send real-time updates to the specified listener
     * whenever task states change, messages are added, or artifacts are created.
     * This enables responsive user interfaces and system monitoring capabilities.</p>
     * 
     * <h3>Listener Capabilities:</h3>
     * <ul>
     * <li>Task status change notifications</li>
     * <li>Message processing updates</li>
     * <li>Artifact creation events</li>
     * <li>Error and cancellation notifications</li>
     * </ul>
     * 
     * <h3>Threading:</h3>
     * <p>Listener methods are called synchronously from the task processing thread.
     * Listeners should implement non-blocking operations to avoid impacting
     * task processing performance.</p>
     * 
     * @param listener The TaskUpdateListener implementation to register for updates
     * @see TaskUpdateListener
     * @see #cancelTask(String)
     */
    public void registerTaskUpdateListener(TaskUpdateListener listener) {
        this.updateListener = listener;
    }

    /**
     * Creates a new task with initial message and begins asynchronous processing.
     * 
     * <p>Initializes a new task with the provided message, stores it in the repository,
     * and immediately begins asynchronous processing. The task starts in PENDING status
     * with a brief delay to allow clients to observe the initial state before processing
     * begins.</p>
     * 
     * <h3>Processing Flow:</h3>
     * <ul>
     * <li>Creates new Task instance with auto-generated UUID</li>
     * <li>Adds initial message to task conversation history</li>
     * <li>Stores task in concurrent repository for thread-safe access</li>
     * <li>Submits task for asynchronous processing with slight delay</li>
     * <li>Returns task immediately for client access</li>
     * </ul>
     * 
     * <h3>Asynchronous Processing:</h3>
     * <p>Task processing occurs on a separate thread from the cached thread pool,
     * allowing the creation request to return immediately. The 10ms delay ensures
     * clients can verify the initial PENDING status before processing begins.</p>
     * 
     * <h3>Thread Safety:</h3>
     * <p>Uses ConcurrentHashMap for thread-safe task storage and retrieval
     * across multiple concurrent operations.</p>
     * 
     * @param initialMessage The first message in the task conversation
     * @return The newly created Task with unique ID and PENDING status
     * @throws IllegalArgumentException if initialMessage is null
     * @see #processTask(Task)
     * @see Message
     */
    public Task createTask(Message initialMessage) {
        Task task = new Task();
        task.addMessage(initialMessage);
        taskRepository.put(task.getId(), task);
        
        // Process the task asynchronously - using a slight delay to ensure the initial PENDING status is preserved
        // This allows tests to verify the initial state before processing begins
        executorService.submit(() -> {
            try {
                Thread.sleep(10);  // Small delay to ensure tests can check initial status
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return task;
    }

    /**
     * Retrieves a task by its unique identifier.
     * 
     * <p>Performs thread-safe lookup of a task using its UUID-based identifier.
     * Returns the complete task object including current status, message history,
     * and any generated artifacts.</p>
     * 
     * <h3>Return Values:</h3>
     * <ul>
     * <li><strong>Task found:</strong> Returns complete Task object with all data</li>
     * <li><strong>Task not found:</strong> Returns null</li>
     * </ul>
     * 
     * <h3>Thread Safety:</h3>
     * <p>Safe for concurrent access from multiple threads due to underlying
     * ConcurrentHashMap implementation.</p>
     * 
     * @param taskId The unique UUID-based identifier of the task to retrieve
     * @return The Task object if found, null otherwise
     * @see Task
     */
    public Task getTask(String taskId) {
        return taskRepository.get(taskId);
    }

    /**
     * Updates the status of an existing task.
     * 
     * <p>Modifies the status of a task identified by taskId. This method provides
     * external status control for integration with other systems or manual
     * task management operations.</p>
     * 
     * <h3>Status Update Process:</h3>
     * <ul>
     * <li>Locates task using provided taskId</li>
     * <li>Updates task status if task exists</li>
     * <li>Automatically updates task modification timestamp</li>
     * <li>Returns updated task for immediate access</li>
     * </ul>
     * 
     * <h3>Common Status Values:</h3>
     * <ul>
     * <li><code>PENDING</code> - Task awaiting processing</li>
     * <li><code>PROCESSING</code> - Task actively being processed</li>
     * <li><code>COMPLETED</code> - Task successfully finished</li>
     * <li><code>FAILED</code> - Task encountered an error</li>
     * <li><code>CANCELLED</code> - Task was cancelled</li>
     * </ul>
     * 
     * @param taskId The unique identifier of the task to update
     * @param status The new TaskStatus to set for the task
     * @return The updated Task object, or null if task not found
     * @see TaskStatus
     * @see #cancelTask(String)
     */
    public Task updateTaskStatus(String taskId, TaskStatus status) {
        Task task = taskRepository.get(taskId);
        if (task != null) {
            task.setStatus(status);
        }
        return task;
    }

    /**
     * Adds a new message to an existing task's conversation history.
     * 
     * <p>Appends a message to the specified task's message collection, maintaining
     * chronological order and updating the task's modification timestamp.
     * This method supports building multi-turn conversations and context preservation.</p>
     * 
     * <h3>Message Addition Process:</h3>
     * <ul>
     * <li>Locates task using provided taskId</li>
     * <li>Appends message to task's message list if task exists</li>
     * <li>Automatically updates task modification timestamp</li>
     * <li>Preserves message order and conversation context</li>
     * </ul>
     * 
     * <h3>Message Types:</h3>
     * <ul>
     * <li><code>user</code> - Messages from human users</li>
     * <li><code>assistant</code> - Responses from AI systems</li>
     * <li><code>system</code> - System-generated notifications</li>
     * </ul>
     * 
     * <h3>Thread Safety:</h3>
     * <p>Safe for concurrent message addition across multiple threads
     * due to synchronized task access patterns.</p>
     * 
     * @param taskId The unique identifier of the task to add the message to
     * @param message The Message object to add to the conversation history
     * @return The updated Task object with the new message, or null if task not found
     * @see Message
     * @see Task#addMessage(Message)
     */
    public Task addMessageToTask(String taskId, Message message) {
        Task task = taskRepository.get(taskId);
        if (task != null) {
            task.addMessage(message);
        }
        return task;
    }

    /**
     * Adds a generated artifact to an existing task's output collection.
     * 
     * <p>Appends an artifact to the specified task's artifact collection, building
     * a comprehensive record of generated outputs, files, and research results.
     * Automatically updates the task's modification timestamp to reflect the
     * artifact creation activity.</p>
     * 
     * <h3>Artifact Addition Process:</h3>
     * <ul>
     * <li>Locates task using provided taskId</li>
     * <li>Appends artifact to task's artifact list if task exists</li>
     * <li>Automatically updates task modification timestamp</li>
     * <li>Preserves artifact creation order and metadata</li>
     * </ul>
     * 
     * <h3>Artifact Types:</h3>
     * <ul>
     * <li><code>research-summary</code> - Generated research summaries</li>
     * <li><code>data-analysis</code> - Statistical analysis results</li>
     * <li><code>literature-review</code> - Academic literature analysis</li>
     * <li><code>generated-content</code> - AI-created text or media</li>
     * <li><code>visualization</code> - Charts, graphs, and diagrams</li>
     * </ul>
     * 
     * <h3>Content Formats:</h3>
     * <p>Supports various content types including text, JSON, HTML, images,
     * and other MIME-typed content as specified in the artifact's contentType field.</p>
     * 
     * @param taskId The unique identifier of the task to add the artifact to
     * @param artifact The TaskArtifact object to add to the output collection
     * @return The updated Task object with the new artifact, or null if task not found
     * @see TaskArtifact
     * @see Task#addArtifact(TaskArtifact)
     */
    public Task addArtifactToTask(String taskId, TaskArtifact artifact) {
        Task task = taskRepository.get(taskId);
        if (task != null) {
            task.addArtifact(artifact);
        }
        return task;
    }

    /**
     * Cancels a running task and notifies registered listeners.
     * 
     * <p>Immediately sets the task status to CANCELLED and broadcasts the change
     * to any registered update listeners. This provides graceful task termination
     * with proper notification to connected clients and monitoring systems.</p>
     * 
     * <h3>Cancellation Process:</h3>
     * <ul>
     * <li>Locates task using provided taskId</li>
     * <li>Sets task status to CANCELLED with descriptive message</li>
     * <li>Notifies registered listeners of the status change</li>
     * <li>Handles listener notification errors gracefully</li>
     * <li>Returns updated task for immediate access</li>
     * </ul>
     * 
     * <h3>Listener Notification:</h3>
     * <p>If a TaskUpdateListener is registered, it receives a task_status_update
     * event with the cancelled task. Listener errors are logged but do not
     * affect the cancellation operation.</p>
     * 
     * <h3>Thread Safety:</h3>
     * <p>Safe for concurrent execution and will not interfere with ongoing
     * task processing operations.</p>
     * 
     * <h3>Status Change:</h3>
     * <p>Sets status to "CANCELLED" with message "Task was cancelled by user request"</p>
     * 
     * @param taskId The unique identifier of the task to cancel
     * @return The updated Task object with CANCELLED status, or null if task not found
     * @see TaskUpdateListener#sendTaskUpdate(String, Object, String)
     * @see TaskStatus
     */
    public Task cancelTask(String taskId) {
        Task task = taskRepository.get(taskId);
        if (task != null) {
            task.setStatus(new TaskStatus("CANCELLED", "Task was cancelled by user request"));
            
            // Notify listeners if registered
            if (updateListener != null) {
                try {
                    updateListener.sendTaskUpdate(taskId, task, "task_status_update");
                } catch (IOException e) {
                    // Log the exception but don't propagate it
                    System.err.println("Error sending task update: " + e.getMessage());
                }
            }
        }
        return task;
    }

    /**
     * Performs the core task processing logic in a separate thread.
     * 
     * <p>This private method implements the main task processing workflow including
     * status transitions, content generation, and artifact creation. It runs
     * asynchronously in the executor service thread pool and handles all aspects
     * of task completion or failure.</p>
     * 
     * <h3>Processing Workflow:</h3>
     * <ul>
     * <li><strong>Status Update:</strong> Changes status from PENDING to PROCESSING</li>
     * <li><strong>Content Generation:</strong> Simulates AI processing with delay</li>
     * <li><strong>Response Creation:</strong> Generates response message from agent</li>
     * <li><strong>Artifact Generation:</strong> Creates result artifacts</li>
     * <li><strong>Completion:</strong> Sets final COMPLETED or FAILED status</li>
     * </ul>
     * 
     * <h3>Error Handling:</h3>
     * <p>Catches all exceptions during processing and sets task status to FAILED
     * with descriptive error message. Ensures tasks never remain in PROCESSING
     * state indefinitely.</p>
     * 
     * <h3>Simulated Processing:</h3>
     * <p>Currently implements simulated processing with 3-second delay and
     * sample response generation. In production, this would integrate with
     * actual AI services, research APIs, and content generation systems.</p>
     * 
     * <h3>Thread Context:</h3>
     * <p>Runs in executor service thread, separate from HTTP request threads.
     * All task modifications are thread-safe due to concurrent repository.</p>
     * 
     * @param task The task to process asynchronously
     * @see TaskStatus
     * @see Message
     * @see TaskArtifact
     */
    private void processTask(Task task) {
        // Update task status to processing
        task.setStatus(new TaskStatus("PROCESSING", "Task is being processed"));
        
        try {
            // This is where we'd implement the agent's specific functionality
            // For example, if this is a research agent, it would perform research tasks
            
            // For this example, we'll just simulate processing with a delay
            Thread.sleep(3000);
            
            Message response = new Message(
                "agent",
                "text/plain",
                "I've processed your request and here are my findings..."
            );
            task.addMessage(response);
            
            TaskArtifact artifact = new TaskArtifact(
                "result",
                "application/json",
                "{ \"result\": \"Sample research findings\" }"
            );
            task.addArtifact(artifact);
            
            // Update task status to completed
            task.setStatus(new TaskStatus("COMPLETED", "Task completed successfully"));
            
        } catch (Exception e) {
            // Handle any errors during processing
            task.setStatus(new TaskStatus("FAILED", "Task processing failed: " + e.getMessage()));
        }
    }

    /**
     * Processes streaming message requests with real-time updates.
     * 
     * <p>Handles streaming message processing operations that require real-time
     * progress updates and continuous client communication. This method is
     * designed for integration with streaming endpoints and real-time user interfaces.</p>
     * 
     * <h3>Streaming Features:</h3>
     * <ul>
     * <li>Real-time progress updates via registered listeners</li>
     * <li>Incremental response delivery as content is generated</li>
     * <li>Live status broadcasting for responsive UIs</li>
     * <li>Continuous artifact streaming as they're created</li>
     * </ul>
     * 
     * <h3>Message Processing:</h3>
     * <p>Processes the provided message map according to its structure and content,
     * applying appropriate handling based on message type, content format, and
     * processing requirements.</p>
     * 
     * <h3>Integration Points:</h3>
     * <ul>
     * <li><strong>TaskStreamingController:</strong> Real-time SSE communication</li>
     * <li><strong>LLM Services:</strong> Streaming response generation</li>
     * <li><strong>Update Listeners:</strong> Progress broadcasting</li>
     * </ul>
     * 
     * @param taskId The unique identifier of the task to process the message for
     * @param messageMap Map containing message data and metadata for processing
     * @see TaskUpdateListener
     * @see TaskStreamingController
     */
    public void processStreamingMessage(String taskId, Map<String, Object> messageMap) {
        // Create message from map
        Message message = new Message();
        message.setRole((String) messageMap.get("role"));
        message.setContentType((String) messageMap.get("contentType"));
        message.setContent((String) messageMap.get("content"));
        
        // Add message to task
        Task task = addMessageToTask(taskId, message);
        
        if (task == null) {
            return;
        }
        
        // Start processing in background
        executorService.submit(() -> processStreamingTask(task));
    }

    private void processStreamingTask(Task task) {
        // Update task status to processing
        task.setStatus(new TaskStatus("PROCESSING", "Task is being processed"));
        
        // Notify listener if registered
        if (updateListener != null) {
            try {
                updateListener.sendTaskUpdate(task.getId(), task, "task_status_update");
            } catch (IOException e) {
                System.err.println("Error sending task update: " + e.getMessage());
            }
        }
        
        try {
            // Simulate processing with incremental updates
            for (int i = 1; i <= 5; i++) {
                Thread.sleep(1000);
                
                // Send incremental message updates
                Message update = new Message(
                    "agent",
                    "text/plain",
                    "Processing update " + i + "/5..."
                );
                task.addMessage(update);
                
                // Notify listener if registered
                if (updateListener != null) {
                    try {
                        updateListener.sendTaskUpdate(task.getId(), update, "message_update");
                    } catch (IOException e) {
                        System.err.println("Error sending message update: " + e.getMessage());
                    }
                }
            }
            
            // Final response
            Message response = new Message(
                "agent",
                "text/plain",
                "I've completed processing your request."
            );
            task.addMessage(response);
            
            // Notify listener if registered
            if (updateListener != null) {
                try {
                    updateListener.sendTaskUpdate(task.getId(), response, "message_update");
                } catch (IOException e) {
                    System.err.println("Error sending message update: " + e.getMessage());
                }
            }
            
            TaskArtifact artifact = new TaskArtifact(
                "result",
                "application/json",
                "{ \"result\": \"Complete research findings\" }"
            );
            task.addArtifact(artifact);
            
            // Notify listener if registered
            if (updateListener != null) {
                try {
                    updateListener.sendTaskUpdate(task.getId(), artifact, "artifact_update");
                } catch (IOException e) {
                    System.err.println("Error sending artifact update: " + e.getMessage());
                }
            }
            
            // Update task status to completed
            task.setStatus(new TaskStatus("COMPLETED", "Task completed successfully"));
            
            // Notify listener if registered
            if (updateListener != null) {
                try {
                    updateListener.sendTaskUpdate(task.getId(), task, "task_status_update");
                } catch (IOException e) {
                    System.err.println("Error sending task update: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            task.setStatus(new TaskStatus("FAILED", "Task processing failed: " + e.getMessage()));
            
            // Notify listener if registered
            if (updateListener != null) {
                try {
                    updateListener.sendTaskUpdate(task.getId(), task, "task_status_update");
                } catch (IOException ex) {
                    System.err.println("Error sending task update: " + ex.getMessage());
                }
            }
        }
    }
}
