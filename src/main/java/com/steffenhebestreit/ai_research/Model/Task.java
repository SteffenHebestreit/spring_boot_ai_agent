package com.steffenhebestreit.ai_research.Model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain model representing a research task in the AI Research system.
 * 
 * <p>This class encapsulates a comprehensive research task with full lifecycle management,
 * message history, artifact collection, and status tracking. It implements the
 * Agent-to-Agent (A2A) protocol specifications for task management and provides
 * contextual linking capabilities for related task operations.</p>
 * 
 * <h3>Core Components:</h3>
 * <ul>
 * <li><strong>Identity Management:</strong> Unique task ID and contextual grouping ID</li>
 * <li><strong>Status Tracking:</strong> Comprehensive status information with descriptions</li>
 * <li><strong>Message History:</strong> Complete conversation and interaction log</li>
 * <li><strong>Artifact Collection:</strong> Generated outputs, files, and research results</li>
 * <li><strong>Lifecycle Timestamps:</strong> Creation and modification tracking</li>
 * </ul>
 * 
 * <h3>A2A Protocol Compliance:</h3>
 * <ul>
 * <li><strong>Task Structure:</strong> Follows A2A task management specifications</li>
 * <li><strong>Context Linking:</strong> Supports task relationship and grouping</li>
 * <li><strong>Status Semantics:</strong> Standard status progression and reporting</li>
 * <li><strong>Artifact Management:</strong> Structured output and result handling</li>
 * </ul>
 * 
 * <h3>Lifecycle Management:</h3>
 * <ul>
 * <li><strong>Creation:</strong> Automatic ID generation and initial status setting</li>
 * <li><strong>Processing:</strong> Status updates and message accumulation</li>
 * <li><strong>Completion:</strong> Final status and artifact collection</li>
 * <li><strong>Timestamping:</strong> Automatic tracking of modifications</li>
 * </ul>
 * 
 * <h3>Data Relationships:</h3>
 * <ul>
 * <li><strong>Messages:</strong> Ordered list of conversation interactions</li>
 * <li><strong>Artifacts:</strong> Collection of generated outputs and results</li>
 * <li><strong>Status:</strong> Current state with descriptive information</li>
 * <li><strong>Context:</strong> Grouping mechanism for related tasks</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>This class is not thread-safe. External synchronization should be used
 * when accessing task instances from multiple threads concurrently.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see TaskStatus
 * @see Message
 * @see TaskArtifact
 */
public class Task {
    /**
     * Unique identifier for the task using UUID format.
     * 
     * <p>This field provides globally unique identification for the task,
     * enabling precise task tracking and referencing across distributed
     * systems and A2A protocol communications.</p>
     */
    private String id;
    
    /**
     * Context identifier for grouping related tasks.
     * 
     * <p>This field enables logical grouping of related tasks within
     * the same research context or workflow, facilitating batch operations
     * and contextual task management.</p>
     */
    private String contextId;
    
    /**
     * Current status information including state and descriptive message.
     * 
     * <p>Comprehensive status object that tracks both machine-readable
     * state classification and human-readable progress descriptions.</p>
     */
    private TaskStatus status;
    
    /**
     * Chronologically ordered collection of conversation messages.
     * 
     * <p>Complete message history maintaining the full conversation
     * context and interaction timeline for the task.</p>
     */
    private List<Message> messages;
    
    /**
     * Collection of generated artifacts and task outputs.
     * 
     * <p>Comprehensive collection of files, documents, analyses, and
     * other deliverables produced during task processing.</p>
     */
    private List<TaskArtifact> artifacts;
    
    /**
     * Timestamp indicating when the task was initially created.
     * 
     * <p>Immutable creation timestamp used for lifecycle tracking
     * and temporal analysis of task processing patterns.</p>
     */
    private Instant createdAt;
    
    /**
     * Timestamp indicating the most recent task modification.
     * 
     * <p>Automatically updated whenever task state, messages, or
     * artifacts are modified, enabling change tracking and monitoring.</p>
     */
    private Instant updatedAt;    /**
     * Creates a new task with auto-generated identifiers and initial state.
     * 
     * <p>Initializes a task with randomly generated UUIDs for both task identification
     * and context grouping. Sets the initial status to PENDING with appropriate
     * description, and prepares empty collections for messages and artifacts.
     * Timestamps are set to current system time for lifecycle tracking.</p>
     * 
     * <h3>Initial State:</h3>
     * <ul>
     * <li><strong>ID:</strong> Randomly generated UUID for unique identification</li>
     * <li><strong>Context ID:</strong> Randomly generated UUID for grouping related tasks</li>
     * <li><strong>Status:</strong> PENDING with descriptive message</li>
     * <li><strong>Collections:</strong> Empty lists for messages and artifacts</li>
     * <li><strong>Timestamps:</strong> Current time for both creation and update</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Used by task creation endpoints and services to establish new research
     * tasks with proper initialization and A2A protocol compliance.</p>
     */
    public Task() {
        this.id = UUID.randomUUID().toString();
        this.contextId = UUID.randomUUID().toString();
        this.status = new TaskStatus("PENDING", "Task created and pending processing");
        this.messages = new ArrayList<>();
        this.artifacts = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the unique identifier for this task.
     * 
     * @return The task's UUID-based unique identifier
     * @see #setId(String)
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this task.
     * 
     * @param id The UUID-based unique identifier to set
     * @see #getId()
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the context identifier for task grouping.
     * 
     * @return The context ID used for grouping related tasks
     * @see #setContextId(String)
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Sets the context identifier for task grouping.
     * 
     * @param contextId The context ID for grouping related tasks
     * @see #getContextId()
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * Returns the current status information for this task.
     * 
     * @return The TaskStatus containing state and message information
     * @see #setStatus(TaskStatus)
     * @see TaskStatus
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Sets the task status and automatically updates the modification timestamp.
     * 
     * <p>Updates the task's current status and automatically refreshes the
     * updatedAt timestamp to reflect the state change. This ensures proper
     * lifecycle tracking and A2A protocol compliance for status reporting.</p>
     * 
     * <h3>Side Effects:</h3>
     * <ul>
     * <li>Updates the updatedAt timestamp to current system time</li>
     * <li>Triggers potential status change notifications in listening services</li>
     * </ul>
     * 
     * @param status The new TaskStatus to set for this task
     * @see TaskStatus
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the complete message history for this task.
     * 
     * @return List of messages in chronological order
     * @see #setMessages(List)
     * @see #addMessage(Message)
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Sets the complete message history for this task.
     * 
     * @param messages List of messages to set
     * @see #getMessages()
     * @see #addMessage(Message)
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Adds a message to the task's conversation history and updates timestamp.
     * 
     * <p>Appends a new message to the task's message collection, maintaining
     * chronological order of the conversation. Automatically updates the
     * task's modification timestamp to reflect the new activity.</p>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     * <li>Appends message to existing message list</li>
     * <li>Maintains chronological message ordering</li>
     * <li>Updates task modification timestamp</li>
     * <li>Preserves message integrity and relationships</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Used by conversation management systems and task processing
     * services to build comprehensive interaction histories.</p>
     * 
     * @param message The message to add to the task's conversation history
     * @see Message
     */
    public void addMessage(Message message) {
        this.messages.add(message);
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the complete artifact collection for this task.
     * 
     * @return List of task artifacts and generated outputs
     * @see #setArtifacts(List)
     * @see #addArtifact(TaskArtifact)
     */
    public List<TaskArtifact> getArtifacts() {
        return artifacts;
    }

    /**
     * Sets the complete artifact collection for this task.
     * 
     * @param artifacts List of task artifacts to set
     * @see #getArtifacts()
     * @see #addArtifact(TaskArtifact)
     */
    public void setArtifacts(List<TaskArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    /**
     * Adds an artifact to the task's output collection and updates timestamp.
     * 
     * <p>Appends a new artifact to the task's artifact collection, building
     * a comprehensive record of generated outputs, files, and research results.
     * Automatically updates the task's modification timestamp to reflect
     * the artifact creation activity.</p>
     * 
     * <h3>Behavior:</h3>
     * <ul>
     * <li>Appends artifact to existing artifact list</li>
     * <li>Maintains artifact creation order</li>
     * <li>Updates task modification timestamp</li>
     * <li>Preserves artifact metadata and relationships</li>
     * </ul>
     * 
     * <h3>Artifact Types:</h3>
     * <p>Supports various artifact types including generated text, images,
     * documents, analysis results, and other task outputs.</p>
     * 
     * <h3>Usage:</h3>
     * <p>Used by task processing services and LLM integrations to collect
     * and organize generated outputs and research results.</p>
     * 
     * @param artifact The artifact to add to the task's output collection
     * @see TaskArtifact
     */
    public void addArtifact(TaskArtifact artifact) {
        this.artifacts.add(artifact);
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the task creation timestamp.
     * 
     * @return The instant when this task was created
     * @see #setCreatedAt(Instant)
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the task creation timestamp.
     * 
     * @param createdAt The creation timestamp to set
     * @see #getCreatedAt()
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the task's last modification timestamp.
     * 
     * @return The instant when this task was last updated
     * @see #setUpdatedAt(Instant)
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the task's last modification timestamp.
     * 
     * @param updatedAt The update timestamp to set
     * @see #getUpdatedAt()
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
