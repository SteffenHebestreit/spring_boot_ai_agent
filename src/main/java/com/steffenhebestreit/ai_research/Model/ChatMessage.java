package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity representing individual messages within chat conversations.
 * 
 * <p>This class models persistent chat messages with comprehensive metadata,
 * relationship management, and content handling capabilities. It supports
 * multimodal content, read status tracking, content summarization, and
 * bidirectional JPA relationships with Chat entities.</p>
 * 
 * <h3>Core Features:</h3>
 * <ul>
 * <li><strong>Message Identity:</strong> Unique UUID-based identification</li>
 * <li><strong>Content Management:</strong> Flexible content storage with type metadata</li>
 * <li><strong>Role-based Classification:</strong> Support for user, assistant, and system roles</li>
 * <li><strong>Relationship Mapping:</strong> Many-to-one bidirectional relationship with Chat</li>
 * <li><strong>Read Status Tracking:</strong> Message read/unread state management</li>
 * <li><strong>Content Summarization:</strong> Optional summary field for large content</li>
 * </ul>
 * 
 * <h3>JPA Configuration:</h3>
 * <ul>
 * <li><strong>Entity Mapping:</strong> Mapped to "chat_messages" table</li>
 * <li><strong>Relationship:</strong> Many-to-one with Chat entity using chat_id foreign key</li>
 * <li><strong>Fetch Strategy:</strong> Lazy loading for chat relationship</li>
 * <li><strong>JSON Handling:</strong> Back reference to prevent circular serialization</li>
 * </ul>
 * 
 * <h3>Content Handling:</h3>
 * <ul>
 * <li><strong>Large Content:</strong> 10,000 character limit for message content</li>
 * <li><strong>Content Types:</strong> Support for text, multimodal, and structured content</li>
 * <li><strong>Type Safety:</strong> Polymorphic content setter for Object types</li>
 * <li><strong>Summarization:</strong> 2,000 character summary field for content overview</li>
 * </ul>
 * 
 * <h3>Message Roles:</h3>
 * <ul>
 * <li><code>user</code> - Messages from human users</li>
 * <li><code>assistant</code> - Responses from AI assistants</li>
 * <li><code>system</code> - System-generated messages and notifications</li>
 * </ul>
 * 
 * <h3>Conversion Capabilities:</h3>
 * <p>Provides static factory methods and conversion utilities for interoperability
 * with other message models in the system, enabling flexible data exchange.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see Chat
 * @see Message
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    @JsonBackReference
    private Chat chat;
    
    private String role;
      @Column(name = "content_type")
    private String contentType;
    
    @Column(length = 30000) // Increased column size to handle larger LLM messages (up to 30K characters)
    private String content;

    @Column(length = 2000) // Add a column for the summary
    private String summary; // Field to store the summary of the content
    
    @Column(name = "created_at")
    private Instant timestamp;
      @Column(name = "is_read")
    private boolean read;
    
    @Column(name = "tool_call_id")
    private String toolCallId;
    
    /**
     * Default constructor creating a new message with auto-generated ID and timestamp.
     * 
     * <p>Initializes a new chat message with a randomly generated UUID identifier,
     * current timestamp, and default read status of false. Prepares the message
     * for content and relationship assignment.</p>
     * 
     * <h3>Initial State:</h3>
     * <ul>
     * <li>Random UUID for unique identification</li>
     * <li>Current timestamp for message creation time</li>
     * <li>Read status set to false</li>
     * <li>All other fields initialized to null</li>
     * </ul>
     */
    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.read = false;
    }
    
    /**
     * Constructor creating a new message with specified role, content type, and content.
     * 
     * <p>Initializes a new chat message with default settings via the default
     * constructor, then sets the provided role, content type, and content.
     * This constructor enables immediate message creation with complete metadata.</p>
     * 
     * <h3>Parameters:</h3>
     * <ul>
     * <li><strong>role:</strong> Message role (user, assistant, system)</li>
     * <li><strong>contentType:</strong> MIME type or content format identifier</li>
     * <li><strong>content:</strong> The actual message content</li>
     * </ul>
     * 
     * @param role The role of the message sender (user, assistant, system)
     * @param contentType The type/format of the message content
     * @param content The actual message content
     */
    public ChatMessage(String role, String contentType, String content) {
        this();
        this.role = role;
        this.contentType = contentType;
        this.content = content;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Chat getChat() {
        return chat;
    }
    
    public void setChat(Chat chat) {
        this.chat = chat;
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
    
    /**
     * Sets message content with polymorphic type handling.
     * 
     * <p>Provides flexible content assignment supporting various object types.
     * String content is stored directly, while other objects are converted
     * to string representation. This method enables multimodal content
     * handling and type-safe content assignment.</p>
     * 
     * <h3>Type Handling:</h3>
     * <ul>
     * <li><strong>String:</strong> Stored directly without conversion</li>
     * <li><strong>Other Objects:</strong> Converted using toString() method</li>
     * <li><strong>Null:</strong> Sets content to null</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Particularly useful for handling complex content types like
     * multimodal messages, structured data, or converted content from
     * external systems.</p>
     * 
     * @param content The content object to set (String or other type)
     */
    public void setContent(Object content) {
        // Handle different content types appropriately
        if (content instanceof String) {
            this.content = (String) content;
        } else if (content != null) {
            // For non-string content, store a representation or convert as needed
            this.content = content.toString();
        } else {
            this.content = null;
        }
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return read;
    }
      public void setRead(boolean read) {
        this.read = read;
    }
    
    public String getToolCallId() {
        return toolCallId;
    }
    
    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }/**
     * Creates a ChatMessage instance from an existing Message object.
     * 
     * <p>Static factory method that converts a Message object to a ChatMessage
     * entity suitable for JPA persistence. Handles content type conversion,
     * multimodal content representation, and metadata preservation.</p>
     * 
     * <h3>Conversion Process:</h3>
     * <ul>
     * <li>Copies role, content type, and timestamp from source message</li>
     * <li>Handles string content directly</li>
     * <li>Converts multimodal content to textual representation</li>
     * <li>Preserves temporal information and metadata</li>
     * </ul>
     * 
     * <h3>Multimodal Handling:</h3>
     * <p>For non-string content, creates a descriptive textual representation
     * indicating the original content type. In production systems, this could
     * be enhanced to store JSON serialization or reference external content.</p>
     * 
     * <h3>Usage:</h3>
     * <p>Used by services and controllers to convert between domain models
     * and persistent entities during message processing and storage.</p>
     * 
     * @param message The source Message object to convert
     * @return A new ChatMessage entity with converted content and metadata
     * @see Message
     */
    // Method to convert from existing Message to ChatMessage
    public static ChatMessage fromMessage(Message message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRole(message.getRole());
        chatMessage.setContentType(message.getContentType());
        
        // Handle content conversion
        Object messageContent = message.getContent();
        if (messageContent instanceof String) {
            chatMessage.setContent((String) messageContent);
        } else if (messageContent != null) {
            // For multimodal content, store a textual representation
            // In a real implementation, you might want to store the JSON serialization
            chatMessage.setContent("Multimodal content: " + messageContent.getClass().getSimpleName());
        }
        
        chatMessage.setTimestamp(message.getTimestamp());
        return chatMessage;
    }
      /**
     * Converts this ChatMessage to a Message object.
     * 
     * <p>Creates a Message object from this ChatMessage entity, enabling
     * interoperability with other parts of the system that use the Message
     * model. Preserves all essential message data and metadata.</p>
     * 
     * <h3>Conversion Process:</h3>
     * <ul>
     * <li>Creates new Message with role, content type, and content</li>
     * <li>Preserves timestamp information</li>
     * <li>Maintains message semantics and metadata</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Used when ChatMessage entities need to be processed by services
     * or components that expect Message objects, such as task processing
     * or external API integrations.</p>
     * 
     * @return A new Message object with this ChatMessage's data
     * @see Message
     */
    // Method to convert to the existing Message model if needed
    public Message toMessage() {
        Message message = new Message(this.role, this.contentType, this.content);
        message.setTimestamp(this.timestamp);
        return message;
    }
}
