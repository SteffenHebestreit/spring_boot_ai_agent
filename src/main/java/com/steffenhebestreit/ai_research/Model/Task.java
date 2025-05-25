package com.steffenhebestreit.ai_research.Model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a research task according to the A2A protocol.
 * 
 * This class encapsulates a research task with its associated messages, artifacts,
 * status, and lifecycle information. It follows the A2A protocol specifications
 * for task management and includes a contextId for linking related tasks.
 */
public class Task {
    private String id;
    private String contextId;
    private TaskStatus status;
    private List<Message> messages;
    private List<TaskArtifact> artifacts;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Creates a new task with default values.
     * 
     * Initializes a task with a randomly generated ID and contextId,
     * sets the status to PENDING, and initializes empty lists for messages
     * and artifacts.
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * Adds a message to the task and updates the task's updatedAt timestamp.
     * 
     * @param message The message to add to the task
     */
    public void addMessage(Message message) {
        this.messages.add(message);
        this.updatedAt = Instant.now();
    }

    public List<TaskArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<TaskArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    /**
     * Adds an artifact to the task and updates the task's updatedAt timestamp.
     * 
     * @param artifact The artifact to add to the task
     */
    public void addArtifact(TaskArtifact artifact) {
        this.artifacts.add(artifact);
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
