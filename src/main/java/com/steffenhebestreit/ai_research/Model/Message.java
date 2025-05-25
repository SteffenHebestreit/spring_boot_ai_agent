package com.steffenhebestreit.ai_research.Model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a message exchanged between a user and the agent.
 * 
 * Messages are the primary means of communication in the A2A protocol.
 * They contain the content of the communication, the role of the sender
 * (user or agent), the content type, and optional metadata.
 */
public class Message {
    private String role;
    private String contentType;
    private String content;
    private Map<String, Object> metadata;
    private Instant timestamp;

    /**
     * Default constructor.
     * 
     * Initializes a message with an empty metadata map and the current timestamp.
     */
    public Message() {
        this.metadata = new HashMap<>();
        this.timestamp = Instant.now();
    }

    /**
     * Creates a new Message with the specified role, content type, and content.
     * 
     * @param role The role of the sender (e.g., "user", "agent", "system")
     * @param contentType The MIME type of the content (e.g., "text/plain", "application/json")
     * @param content The actual content of the message
     */
    public Message(String role, String contentType, String content) {
        this.role = role;
        this.contentType = contentType;
        this.content = content;
        this.metadata = new HashMap<>();
        this.timestamp = Instant.now();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
