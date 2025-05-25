package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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
    
    @Column(length = 10000) // Increase column size for larger messages
    private String content;

    @Column(length = 2000) // Add a column for the summary
    private String summary; // Field to store the summary of the content
    
    @Column(name = "created_at")
    private Instant timestamp;
    
    @Column(name = "is_read")
    private boolean read;
    
    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.read = false;
    }
    
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
    
    // Method to convert from existing Message to ChatMessage
    public static ChatMessage fromMessage(Message message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRole(message.getRole());
        chatMessage.setContentType(message.getContentType());
        chatMessage.setContent(message.getContent());
        chatMessage.setTimestamp(message.getTimestamp());
        return chatMessage;
    }
    
    // Method to convert to the existing Message model if needed
    public Message toMessage() {
        Message message = new Message(this.role, this.contentType, this.content);
        message.setTimestamp(this.timestamp);
        return message;
    }
}
