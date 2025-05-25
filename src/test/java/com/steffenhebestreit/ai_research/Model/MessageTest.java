package com.steffenhebestreit.ai_research.Model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Message model class
 */
public class MessageTest {

    @Test
    void defaultConstructor_ShouldInitializeMetadataAndTimestamp() {
        // When
        Message message = new Message();
        
        // Then
        assertNotNull(message.getMetadata(), "Metadata should be initialized");
        assertTrue(message.getMetadata().isEmpty(), "Metadata should be empty");
        assertNotNull(message.getTimestamp(), "Timestamp should be initialized");
    }

    @Test
    void parameterizedConstructor_ShouldInitializeAllFields() {
        // Given
        String role = "user";
        String contentType = "text/plain";
        String content = "Test message content";
        
        // When
        Message message = new Message(role, contentType, content);
        
        // Then
        assertEquals(role, message.getRole());
        assertEquals(contentType, message.getContentType());
        assertEquals(content, message.getContent());
        assertNotNull(message.getMetadata());
        assertNotNull(message.getTimestamp());
    }

    @Test
    void setRole_ShouldUpdateRole() {
        // Given
        Message message = new Message();
        String role = "agent";
        
        // When
        message.setRole(role);
        
        // Then
        assertEquals(role, message.getRole());
    }

    @Test
    void setContentType_ShouldUpdateContentType() {
        // Given
        Message message = new Message();
        String contentType = "application/json";
        
        // When
        message.setContentType(contentType);
        
        // Then
        assertEquals(contentType, message.getContentType());
    }

    @Test
    void setContent_ShouldUpdateContent() {
        // Given
        Message message = new Message();
        String content = "Updated message content";
        
        // When
        message.setContent(content);
        
        // Then
        assertEquals(content, message.getContent());
    }

    @Test
    void setMetadata_ShouldUpdateMetadata() {
        // Given
        Message message = new Message();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 2);
        
        // When
        message.setMetadata(metadata);
        
        // Then
        assertEquals(metadata, message.getMetadata());
        assertEquals("value1", message.getMetadata().get("key1"));
        assertEquals(2, message.getMetadata().get("key2"));
    }

    @Test
    void setTimestamp_ShouldUpdateTimestamp() {
        // Given
        Message message = new Message();
        Instant timestamp = Instant.parse("2023-01-01T12:00:00Z");
        
        // When
        message.setTimestamp(timestamp);
        
        // Then
        assertEquals(timestamp, message.getTimestamp());
    }
}
