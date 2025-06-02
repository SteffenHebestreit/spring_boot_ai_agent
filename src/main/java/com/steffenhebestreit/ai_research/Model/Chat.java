package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity representing a chat session in the AI Research system.
 * 
 * <p>This class models a persistent chat conversation with automatic lifecycle
 * management, message relationships, and intelligent title generation. It provides
 * comprehensive conversation tracking with JPA persistence, bi-directional message
 * relationships, and convenient utility methods for chat management.</p>
 * 
 * <h3>Core Features:</h3>
 * <ul>
 * <li><strong>Session Management:</strong> Unique chat identification and title management</li>
 * <li><strong>Message Relationships:</strong> One-to-many mapping with ChatMessage entities</li>
 * <li><strong>Lifecycle Tracking:</strong> Creation and modification timestamps</li>
 * <li><strong>Title Generation:</strong> Automatic title creation from conversation content</li>
 * </ul>
 * 
 * <h3>JPA Configuration:</h3>
 * <ul>
 * <li><strong>Entity Mapping:</strong> Mapped to "chats" table with UUID primary key</li>
 * <li><strong>Cascade Operations:</strong> ALL cascade for message relationships</li>
 * <li><strong>Fetch Strategy:</strong> Lazy loading for message collections</li>
 * <li><strong>JSON Serialization:</strong> Managed reference for circular dependency prevention</li>
 * </ul>
 * 
 * <h3>Message Management:</h3>
 * <ul>
 * <li><strong>Bidirectional Mapping:</strong> Maintains parent-child relationships</li>
 * <li><strong>Automatic Timestamping:</strong> Updates modification time on message addition</li>
 * <li><strong>Ordered Collection:</strong> Messages maintained in chronological order</li>
 * <li><strong>Convenience Methods:</strong> Easy access to latest message and content</li>
 * </ul>
 * 
 * <h3>Title Management:</h3>
 * <ul>
 * <li><strong>Auto-generation:</strong> Creates titles from first user message content</li>
 * <li><strong>Fallback Strategy:</strong> Uses timestamp if no user content available</li>
 * <li><strong>Length Limitation:</strong> Truncates long content with ellipsis</li>
 * <li><strong>Manual Override:</strong> Supports explicit title setting</li>
 * </ul>
 * 
 * <h3>Database Schema:</h3>
 * <p>Persisted to "chats" table with created_at and updated_at columns for
 * temporal tracking and audit capabilities.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see ChatMessage
 */
@Entity
@Table(name = "chats")
public class Chat {
    @Id
    private String id;
    
    private String title;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ChatMessage> messages = new ArrayList<>();
    
    /**
     * Default constructor creating a new chat with auto-generated ID and timestamps.
     * 
     * <p>Initializes a new chat session with a randomly generated UUID identifier
     * and current timestamp for both creation and modification times. Prepares
     * an empty message collection for conversation content.</p>
     * 
     * <h3>Initial State:</h3>
     * <ul>
     * <li>Random UUID for unique identification</li>
     * <li>Current timestamp for creation and update times</li>
     * <li>Empty ArrayList for message collection</li>
     * <li>Null title (to be generated or set later)</li>
     * </ul>
     */
    public Chat() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Constructor creating a new chat with specified title.
     * 
     * <p>Initializes a new chat session with default settings via the default
     * constructor, then sets the provided title. This allows immediate chat
     * creation with a known title without requiring auto-generation.</p>
     * 
     * @param title The initial title for the chat session
     */
    public Chat(String title) {
        this();
        this.title = title;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
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
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    /**
     * Adds a message to the chat and establishes bidirectional relationship.
     * 
     * <p>Appends a new message to the chat's message collection while maintaining
     * the bidirectional JPA relationship by setting the message's chat reference.
     * Automatically updates the chat's modification timestamp to reflect the
     * new activity.</p>
     * 
     * <h3>Relationship Management:</h3>
     * <ul>
     * <li>Adds message to this chat's message collection</li>
     * <li>Sets the message's chat reference for bidirectional mapping</li>
     * <li>Updates chat modification timestamp</li>
     * <li>Maintains chronological message ordering</li>
     * </ul>
     * 
     * <h3>JPA Considerations:</h3>
     * <p>Properly manages the owning side of the relationship and ensures
     * both sides of the bidirectional mapping are correctly maintained.</p>
     * 
     * @param message The ChatMessage to add to this chat session
     * @see ChatMessage#setChat(Chat)
     */
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        message.setChat(this);
        this.updatedAt = Instant.now();
    }
      /**
     * Retrieves the most recent message in the chat conversation.
     * 
     * <p>Returns the last message in the chronologically ordered message collection,
     * useful for displaying conversation previews in chat lists or determining
     * the current conversation state.</p>
     * 
     * <h3>Usage:</h3>
     * <ul>
     * <li>Chat list previews showing latest message content</li>
     * <li>Conversation state determination</li>
     * <li>Last activity indicators</li>
     * <li>UI summary displays</li>
     * </ul>
     * 
     * @return The most recent ChatMessage, or null if no messages exist
     * @see ChatMessage
     */
    // Helper method to get the last message for display in chat list
    public ChatMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
      /**
     * Automatically generates a chat title from conversation content.
     * 
     * <p>Creates an intelligent title for the chat based on the first user message
     * content. If no title is currently set, this method extracts meaningful
     * content from the conversation to create a descriptive title with appropriate
     * length limitations.</p>
     * 
     * <h3>Title Generation Strategy:</h3>
     * <ul>
     * <li><strong>Primary:</strong> Uses first user message content (max 30 chars)</li>
     * <li><strong>Truncation:</strong> Adds ellipsis for longer content</li>
     * <li><strong>Fallback:</strong> Uses creation timestamp if no user messages</li>
     * <li><strong>Preservation:</strong> Skips generation if title already exists</li>
     * </ul>
     * 
     * <h3>Content Processing:</h3>
     * <ul>
     * <li>Searches for first message with "user" role</li>
     * <li>Extracts non-empty content from the message</li>
     * <li>Applies 30-character limit with ellipsis truncation</li>
     * <li>Generates timestamp-based fallback if needed</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Typically called after adding the first user message to establish
     * a meaningful chat title for UI display and organization.</p>
     * 
     * @see ChatMessage#getRole()
     * @see ChatMessage#getContent()
     */
    // Auto-generate title from first user message if not set
    public void generateTitleFromContent() {
        if (this.title == null || this.title.isEmpty()) {
            for (ChatMessage message : this.messages) {
                if ("user".equals(message.getRole())) {
                    String content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        // Use first 30 chars of the first user message or the full message if shorter
                        this.title = content.length() <= 30 ? 
                                    content : 
                                    content.substring(0, 27) + "...";
                        break;
                    }
                }
            }
            
            // If still no title (no user messages), use timestamp
            if (this.title == null || this.title.isEmpty()) {
                this.title = "Chat " + this.createdAt.toString();
            }
        }
    }
}
