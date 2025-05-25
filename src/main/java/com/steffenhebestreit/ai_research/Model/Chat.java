package com.steffenhebestreit.ai_research.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    
    // Helper method to get the last message for display in chat list
    public ChatMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
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
