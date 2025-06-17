package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity representing a chat session in the AI Research system.
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
    
    public Chat() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
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
    
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        message.setChat(this);
        this.updatedAt = Instant.now();
    }
    
    public ChatMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
    /**
     * Automatically generates a chat title from the first user message content.
     * <p>
     * This method scans through the chat messages to find the first user message
     * and creates a descriptive title from its content. For multimodal content,
     * it extracts only the text portions to create a meaningful title.
     * If no suitable content is found, it falls back to using the creation timestamp.
     * </p>
     */
    public void generateTitleFromContent() {
        if (this.title == null || this.title.isEmpty()) {
            for (ChatMessage message : this.messages) {
                if ("user".equals(message.getRole())) {
                    String content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        String titleContent = content;
                        
                        // Handle multimodal content by extracting text parts only
                        if ("multipart/mixed".equals(message.getContentType())) {
                            titleContent = extractTextFromMultimodalContent(content);
                        }
                        
                        if (titleContent != null && !titleContent.isEmpty()) {
                            // Use first 30 chars of the text content or the full text if shorter
                            this.title = titleContent.length() <= 30 ? 
                                        titleContent : 
                                        titleContent.substring(0, 27) + "...";
                            break;
                        }
                    }
                }
            }
            
            // If still no title (no user messages), use timestamp
            if (this.title == null || this.title.isEmpty()) {
                this.title = "Chat " + this.createdAt.toString();
            }
        }
    }
    
    /**
     * Extracts text content from multimodal content for title generation.
     * <p>
     * This method parses the JSON structure of multimodal content and extracts
     * only the text parts, ignoring image and other non-text content.
     * 
     * @param multimodalContent The JSON string representation of multimodal content
     * @return Extracted text content, or null if no text found or parsing fails
     */
    private String extractTextFromMultimodalContent(String multimodalContent) {
        try {
            // Try to parse the multimodal content as JSON
            ObjectMapper objectMapper = new ObjectMapper();
            Object[] contentArray = objectMapper.readValue(multimodalContent, Object[].class);
            
            StringBuilder textContent = new StringBuilder();
            
            for (Object item : contentArray) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    String type = (String) itemMap.get("type");
                    
                    if ("text".equals(type)) {
                        String text = (String) itemMap.get("text");
                        if (text != null && !text.trim().isEmpty()) {
                            if (textContent.length() > 0) {
                                textContent.append(" ");
                            }
                            textContent.append(text.trim());
                        }
                    }
                }
            }
            
            return textContent.length() > 0 ? textContent.toString() : null;
        } catch (Exception e) {
            // If parsing fails, return null so we fall back to timestamp
            return null;
        }
    }
}
