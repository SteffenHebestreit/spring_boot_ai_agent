package com.steffenhebestreit.ai_research.Model;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskArtifact model class
 */
public class TaskArtifactTest {

    @Test
    void defaultConstructor_ShouldInitializeIdAndCreatedAt() {
        // When
        TaskArtifact artifact = new TaskArtifact();
        
        // Then
        assertNotNull(artifact.getId(), "ID should be generated");
        assertNotNull(artifact.getCreatedAt(), "Creation timestamp should be set");
    }

    @Test
    void parameterizedConstructor_ShouldInitializeAllFields() {
        // Given
        String type = "research_summary";
        String contentType = "application/pdf";
        String content = "PDF content as base64";
        
        // When
        TaskArtifact artifact = new TaskArtifact(type, contentType, content);
        
        // Then
        assertNotNull(artifact.getId(), "ID should be generated");
        assertEquals(type, artifact.getType());
        assertEquals(contentType, artifact.getContentType());
        assertEquals(content, artifact.getContent());
        assertNotNull(artifact.getCreatedAt(), "Creation timestamp should be set");
    }

    @Test
    void setId_ShouldUpdateId() {
        // Given
        TaskArtifact artifact = new TaskArtifact();
        String id = "custom-id-123";
        
        // When
        artifact.setId(id);
        
        // Then
        assertEquals(id, artifact.getId());
    }

    @Test
    void setType_ShouldUpdateType() {
        // Given
        TaskArtifact artifact = new TaskArtifact();
        String type = "data_visualization";
        
        // When
        artifact.setType(type);
        
        // Then
        assertEquals(type, artifact.getType());
    }

    @Test
    void setContentType_ShouldUpdateContentType() {
        // Given
        TaskArtifact artifact = new TaskArtifact();
        String contentType = "image/png";
        
        // When
        artifact.setContentType(contentType);
        
        // Then
        assertEquals(contentType, artifact.getContentType());
    }

    @Test
    void setContent_ShouldUpdateContent() {
        // Given
        TaskArtifact artifact = new TaskArtifact();
        String content = "Updated content data";
        
        // When
        artifact.setContent(content);
        
        // Then
        assertEquals(content, artifact.getContent());
    }

    @Test
    void setCreatedAt_ShouldUpdateCreatedAt() {
        // Given
        TaskArtifact artifact = new TaskArtifact();
        Instant createdAt = Instant.parse("2023-01-01T12:00:00Z");
        
        // When
        artifact.setCreatedAt(createdAt);
        
        // Then
        assertEquals(createdAt, artifact.getCreatedAt());
    }
}
