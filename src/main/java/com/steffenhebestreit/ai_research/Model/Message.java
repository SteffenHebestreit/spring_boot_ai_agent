package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain model representing messages in the AI Research communication system.
 * 
 * <p>This class models comprehensive message exchange between users, agents, and
 * systems within the A2A (Agent-to-Agent) protocol framework. It supports both
 * simple text communication and advanced multimodal content including images,
 * documents, and structured data with extensive metadata capabilities.</p>
 * 
 * <h3>Core Features:</h3>
 * <ul>
 * <li><strong>Role-based Communication:</strong> Support for user, agent, and system roles</li>
 * <li><strong>Multimodal Content:</strong> Text, images, documents, and structured data</li>
 * <li><strong>Content Type Management:</strong> MIME type specification and handling</li>
 * <li><strong>Metadata Support:</strong> Extensible key-value metadata storage</li>
 * <li><strong>Temporal Tracking:</strong> Message creation and exchange timestamps</li>
 * </ul>
 * 
 * <h3>Content Flexibility:</h3>
 * <ul>
 * <li><strong>Simple Text:</strong> String content for basic text messages</li>
 * <li><strong>Structured Content:</strong> Object arrays for multimodal content blocks</li>
 * <li><strong>Media Content:</strong> Base64-encoded images, documents, and files</li>
 * <li><strong>JSON Structures:</strong> Complex data objects and API responses</li>
 * </ul>
 * 
 * <h3>A2A Protocol Compliance:</h3>
 * <ul>
 * <li><strong>Standard Structure:</strong> Follows A2A message specifications</li>
 * <li><strong>Interoperability:</strong> Compatible with other agent systems</li>
 * <li><strong>Content Negotiation:</strong> MIME type-based content handling</li>
 * <li><strong>Metadata Exchange:</strong> Structured additional information</li>
 * </ul>
 * 
 * <h3>Message Roles:</h3>
 * <ul>
 * <li><code>user</code> - Messages from human users</li>
 * <li><code>agent</code> - Responses and communications from AI agents</li>
 * <li><code>system</code> - System-generated messages and notifications</li>
 * <li><code>assistant</code> - AI assistant responses and interactions</li>
 * </ul>
 * 
 * <h3>Content Types:</h3>
 * <ul>
 * <li><code>text/plain</code> - Simple text messages</li>
 * <li><code>application/json</code> - Structured JSON data</li>
 * <li><code>multipart/mixed</code> - Multimodal content with multiple types</li>
 * <li><code>image/*</code> - Image content (PNG, JPEG, WebP, etc.)</li>
 * <li><code>application/pdf</code> - PDF documents</li>
 * </ul>
 * 
 * <h3>JSON Serialization:</h3>
 * <p>Configured with @JsonInclude to exclude null values from serialization,
 * ensuring clean JSON output for API responses and inter-service communication.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see ChatMessage
 * @see Task
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String role;
    private String contentType;
    
    // Content can be a simple String or a complex Object for multimodal content
    private Object content;
    
    private Map<String, Object> metadata;
    private Instant timestamp;    /**
     * Default constructor creating a new message with metadata and timestamp.
     * 
     * <p>Initializes a new message with an empty metadata map for extensible
     * key-value storage and current timestamp for temporal tracking. This
     * constructor prepares the message for content and role assignment.</p>
     * 
     * <h3>Initial State:</h3>
     * <ul>
     * <li>Empty HashMap for metadata storage</li>
     * <li>Current timestamp for message creation time</li>
     * <li>Null values for role, content type, and content</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Used by frameworks, serialization libraries, and when message
     * properties will be set incrementally during processing.</p>
     */
    public Message() {
        this.metadata = new HashMap<>();
        this.timestamp = Instant.now();
    }    /**
     * Constructor creating a new message with specified role, content type, and content.
     * 
     * <p>Initializes a complete message with all core properties set. Automatically
     * creates metadata storage and sets creation timestamp while accepting the
     * message's role, MIME content type, and polymorphic content object.</p>
     * 
     * <h3>Content Type Handling:</h3>
     * <ul>
     * <li><strong>String Content:</strong> Simple text messages</li>
     * <li><strong>Object Content:</strong> Multimodal content arrays, JSON structures</li>
     * <li><strong>Structured Data:</strong> Complex content blocks with metadata</li>
     * </ul>
     * 
     * <h3>Parameter Guidelines:</h3>
     * <ul>
     * <li><strong>role:</strong> Use standard roles (user, agent, system, assistant)</li>
     * <li><strong>contentType:</strong> Specify proper MIME type for content</li>
     * <li><strong>content:</strong> Provide appropriate content structure for type</li>
     * </ul>
     * 
     * <h3>Usage Examples:</h3>
     * <pre>
     * new Message("user", "text/plain", "Hello, can you help me?")
     * new Message("agent", "application/json", responseObject)
     * new Message("system", "multipart/mixed", multimodalContentArray)
     * </pre>
     * 
     * @param role The role of the message sender (user, agent, system, assistant)
     * @param contentType The MIME type of the content (text/plain, application/json, etc.)
     * @param content The message content (String for text, Object for structured/multimodal)
     */
    public Message(String role, String contentType, Object content) {
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
    }    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
      /**
     * Retrieves content as a string if it's string-type content.
     * 
     * <p>Convenience method for safely accessing string content from the
     * polymorphic content field. Returns the content as a string if it's
     * a String instance, or null if it's multimodal or structured content.</p>
     * 
     * <h3>Type Safety:</h3>
     * <ul>
     * <li>Performs instanceof check before casting</li>
     * <li>Returns null for non-string content types</li>
     * <li>Avoids ClassCastException for multimodal content</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Ideal for simple text processing scenarios where you need to
     * verify content is text-based before performing string operations.</p>
     * 
     * @return The content as a String if it's string content, null otherwise
     * @see #getContent()
     */
    @JsonIgnore
    public String getContentAsString() {
        return (content instanceof String) ? (String) content : null;
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
