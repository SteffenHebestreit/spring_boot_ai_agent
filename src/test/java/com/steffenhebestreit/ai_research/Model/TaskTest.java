package com.steffenhebestreit.ai_research.Model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Task model class
 */
public class TaskTest {

    @Test
    void constructor_ShouldInitializeWithDefaultValues() {
        // When
        Task task = new Task();
        
        // Then
        assertNotNull(task.getId(), "Task ID should be generated");
        assertNotNull(task.getContextId(), "Context ID should be generated");
        assertNotNull(task.getStatus(), "Status should be initialized");
        assertEquals("PENDING", task.getStatus().getState(), "Initial state should be PENDING");
        assertNotNull(task.getMessages(), "Messages list should be initialized");
        assertTrue(task.getMessages().isEmpty(), "Messages list should be empty");
        assertNotNull(task.getArtifacts(), "Artifacts list should be initialized");
        assertTrue(task.getArtifacts().isEmpty(), "Artifacts list should be empty");
        assertNotNull(task.getCreatedAt(), "Creation timestamp should be set");
        assertNotNull(task.getUpdatedAt(), "Update timestamp should be set");
    }

    @Test
    void addMessage_ShouldAddMessageAndUpdateTimestamp() {
        // Given
        Task task = new Task();
        Instant initialUpdateTime = task.getUpdatedAt();
        
        // Ensure a small delay to make timestamps different
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        Message message = new Message("user", "text/plain", "Test message");
        
        // When
        task.addMessage(message);
        
        // Then
        assertEquals(1, task.getMessages().size(), "Message should be added to the list");
        assertEquals(message, task.getMessages().get(0), "Added message should be retrievable");
        assertTrue(task.getUpdatedAt().isAfter(initialUpdateTime), "Updated timestamp should be later than initial");
    }

    @Test
    void addArtifact_ShouldAddArtifactAndUpdateTimestamp() {
        // Given
        Task task = new Task();
        Instant initialUpdateTime = task.getUpdatedAt();
        
        // Ensure a small delay to make timestamps different
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        TaskArtifact artifact = new TaskArtifact("test", "application/json", "{\"data\": \"test\"}");
        
        // When
        task.addArtifact(artifact);
        
        // Then
        assertEquals(1, task.getArtifacts().size(), "Artifact should be added to the list");
        assertEquals(artifact, task.getArtifacts().get(0), "Added artifact should be retrievable");
        assertTrue(task.getUpdatedAt().isAfter(initialUpdateTime), "Updated timestamp should be later than initial");
    }

    @Test
    void setStatus_ShouldUpdateStatusAndUpdateTimestamp() {
        // Given
        Task task = new Task();
        Instant initialUpdateTime = task.getUpdatedAt();
        
        // Ensure a small delay to make timestamps different
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        TaskStatus newStatus = new TaskStatus("COMPLETED", "Task completed successfully");
        
        // When
        task.setStatus(newStatus);
        
        // Then
        assertEquals(newStatus, task.getStatus(), "Status should be updated");
        assertTrue(task.getUpdatedAt().isAfter(initialUpdateTime), "Updated timestamp should be later than initial");
    }
}
