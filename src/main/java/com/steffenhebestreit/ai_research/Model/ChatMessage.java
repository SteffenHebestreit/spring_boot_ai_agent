package com.steffenhebestreit.ai_research.Model;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID; // For generating ID
import jakarta.persistence.*; // JPA annotations
import com.fasterxml.jackson.annotation.JsonBackReference; // For bi-directional relationship

/**
 * JPA Entity representing individual messages within chat conversations.
 * Now restored as a JPA Entity.
 */
// Remove Lombok constructors and use explicit ones only
@Data
// @NoArgsConstructor - We'll define our own no-arg constructor explicitly
@Entity
@Table(name = "chat_messages") // Define table name
public class ChatMessage {
    // Manual implementation of no-arg constructor to ensure it's available
    public ChatMessage() {
        // This is intentionally empty to provide a no-arg constructor
    }
    
    @Id
    private String id;

    @Column(nullable = false) // Role should not be null
    private String role;

    @Lob // Content can be large
    @Column(columnDefinition = "TEXT")
    private String content;

    private String name; // For 'tool' role, this is the function name; for 'user' role, optional user name
    private String toolCallId; // For 'tool' role messages

    @Lob // For potentially large JSON string
    @Column(columnDefinition = "TEXT") // Ensure enough space for toolCalls JSON
    private String toolCallsJson; // Storing List<Map<String, Object>> as JSON string

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    @JsonBackReference // To prevent serialization loops with Chat
    private Chat chat;

    // JPA specific fields that were missing
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    @Column(name = "content_type") // Assuming you might need this based on original DTO
    private String contentType;

    @Lob
    @Column(name = "raw_content", columnDefinition = "TEXT") // For storing raw content if different from processed
    private String rawContent;

    @Column(name = "is_read")
    private boolean isRead = false;

    // Virtual field for backward compatibility with repository methods
    // Maps to createdAt for queries but isn't stored directly in the database
    @Transient
    private java.time.Instant timestamp;
    
    // Transient field for toolCalls, to be populated from toolCallsJson
    @Transient
    private List<Map<String, Object>> toolCalls;

    // Lifecycle callback to ensure ID is generated before persist
    @PrePersist
    public void ensureIdAndTimestamps() { // Renamed and expanded
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = java.time.Instant.now();
        this.updatedAt = java.time.Instant.now();
    }

    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = java.time.Instant.now();
    }

    // Constructor for simple user/assistant/system messages
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // Constructor for tool messages (response from a tool)
    public ChatMessage(String role, String content, String toolCallId, String name) {
        if (!"tool".equals(role)) {
            throw new IllegalArgumentException("This constructor is for 'tool' role messages only.");
        }
        this.role = role;
        this.content = content; // Result of the tool call
        this.toolCallId = toolCallId;
        this.name = name; // Function name
    }

    // Constructor for assistant messages requesting tool calls (content might be null)
    // This constructor will need to handle the toolCallsJson serialization
    public ChatMessage(String role, String content, List<Map<String, Object>> toolCalls) {
        if (!"assistant".equals(role)) {
            throw new IllegalArgumentException("This constructor is for 'assistant' role messages.");
        }
        this.role = role;
        this.content = content; // Can be null or a message accompanying tool calls
        this.toolCalls = toolCalls;
        // Note: toolCallsJson would need to be set by serializing toolCalls
        // This could be done in a setter or a specific service method
    }
    
    // Constructor for assistant messages with only tool calls (content is null)
    // This constructor will also need to handle toolCallsJson serialization
    public ChatMessage(String role, List<Map<String, Object>> toolCalls) {
        if (!"assistant".equals(role)) {
            throw new IllegalArgumentException("This constructor is for 'assistant' role messages with tool_calls only.");
        }
        this.role = role;
        this.toolCalls = toolCalls;
        // Note: toolCallsJson would need to be set by serializing toolCalls
    }

    // Constructor for messages with content type (used in tests)
    public ChatMessage(String role, String contentType, String content) {
        this.role = role;
        this.contentType = contentType;
        this.content = content;
    }
    
    // Getter and Setter for Chat to maintain bi-directional relationship
    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }
    
    // Custom getter/setter for toolCalls to handle JSON serialization/deserialization
    // This would typically involve ObjectMapper from Jackson
    // For simplicity, direct access to toolCalls (transient) and toolCallsJson (persistent) is shown
    // Actual JSON conversion logic would be needed in service layers or @PostLoad / @PreUpdate methods

    public List<Map<String, Object>> getToolCalls() {
        // Deserialize from toolCallsJson if needed and toolCalls is null
        // For now, just returning the transient field
        return toolCalls;
    }

    public void setToolCalls(List<Map<String, Object>> toolCalls) {
        this.toolCalls = toolCalls;
        // Serialize to toolCallsJson here
        // For now, just setting the transient field
    }

    public String getToolCallsJson() {
        return toolCallsJson;
    }

    public void setToolCallsJson(String toolCallsJson) {
        this.toolCallsJson = toolCallsJson;
    }

    // Add getters and setters for the new JPA fields and other missing fields
    // Lombok's @Data should handle most, but explicit ones might be needed if
    // custom logic is involved, or to resolve specific "symbol not found" errors.

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
    
    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }    // Getter and setter for timestamp (virtual field)
    public java.time.Instant getTimestamp() {
        // Return createdAt as the timestamp for backward compatibility
        return this.createdAt;
    }

    public void setTimestamp(java.time.Instant timestamp) {
        // When timestamp is set, update both createdAt and timestamp
        this.timestamp = timestamp;
        this.createdAt = timestamp;
        this.updatedAt = timestamp;
    }    // Static factory method to convert from Message (if still needed)
    // This is a placeholder, actual implementation depends on Message class structure
    public static ChatMessage fromMessage(com.steffenhebestreit.ai_research.Model.Message message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRole(message.getRole());
        // Ensure content is string. If message.getContent() can be non-string, add conversion.
        if (message.getContent() instanceof String) {
            chatMessage.setContent((String) message.getContent());
        } else if (message.getContent() != null) {
            // Handle non-string content properly - use JSON serialization for multimodal content
            // This prevents the "[Ljava.lang.Object;@hash" issue
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                chatMessage.setContent(objectMapper.writeValueAsString(message.getContent()));
            } catch (Exception e) {
                // Fallback to toString if JSON serialization fails
                chatMessage.setContent(message.getContent().toString());
            }
        }
        chatMessage.setContentType(message.getContentType());
        if (message.getTimestamp() != null) {
            chatMessage.setTimestamp(message.getTimestamp());
        }
        // Map other relevant fields from Message to ChatMessage
        return chatMessage;
    }
}
