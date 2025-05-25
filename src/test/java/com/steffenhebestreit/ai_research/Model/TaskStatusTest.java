package com.steffenhebestreit.ai_research.Model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskStatus model class
 */
public class TaskStatusTest {

    @Test
    void defaultConstructor_ShouldInitializeWithNullValues() {
        // When
        TaskStatus status = new TaskStatus();
        
        // Then
        assertNull(status.getState());
        assertNull(status.getMessage());
    }

    @Test
    void parameterizedConstructor_ShouldInitializeWithGivenValues() {
        // Given
        String state = "PROCESSING";
        String message = "Task is being processed";
        
        // When
        TaskStatus status = new TaskStatus(state, message);
        
        // Then
        assertEquals(state, status.getState());
        assertEquals(message, status.getMessage());
    }

    @Test
    void setState_ShouldUpdateState() {
        // Given
        TaskStatus status = new TaskStatus("PENDING", "Task is pending");
        String newState = "CANCELLED";
        
        // When
        status.setState(newState);
        
        // Then
        assertEquals(newState, status.getState());
    }

    @Test
    void setMessage_ShouldUpdateMessage() {
        // Given
        TaskStatus status = new TaskStatus("PENDING", "Task is pending");
        String newMessage = "Task processing has been cancelled";
        
        // When
        status.setMessage(newMessage);
        
        // Then
        assertEquals(newMessage, status.getMessage());
    }    @Test
    void getMessage_ShouldReturnMessage() {
        // Given
        String message = "Task is being processed";
        TaskStatus status = new TaskStatus("PROCESSING", message);
        
        // When & Then
        assertEquals(message, status.getMessage());
    }
}
