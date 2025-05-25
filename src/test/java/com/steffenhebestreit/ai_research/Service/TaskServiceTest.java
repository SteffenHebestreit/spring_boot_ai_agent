package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Model.Task;
import com.steffenhebestreit.ai_research.Model.TaskArtifact;
import com.steffenhebestreit.ai_research.Model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskService}
 */
class TaskServiceTest {

    private TaskService taskService;

    @Mock
    private TaskUpdateListener mockListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taskService = new TaskService();
        taskService.registerTaskUpdateListener(mockListener);
    }

    @Test
    void createTask_ShouldCreateNewTaskWithInitialMessage() {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing trends");
        
        // When
        Task task = taskService.createTask(initialMessage);
        
        // Then
        assertNotNull(task);
        assertNotNull(task.getId());
        assertEquals("PENDING", task.getStatus().getState());
        assertEquals(1, task.getMessages().size());
        assertEquals(initialMessage.getContent(), task.getMessages().get(0).getContent());
    }

    @Test
    void getTask_ShouldReturnExistingTask() {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing trends");
        Task createdTask = taskService.createTask(initialMessage);
        
        // When
        Task retrievedTask = taskService.getTask(createdTask.getId());
        
        // Then
        assertNotNull(retrievedTask);
        assertEquals(createdTask.getId(), retrievedTask.getId());
    }

    @Test
    void updateTaskStatus_ShouldUpdateStatus() {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing trends");
        Task task = taskService.createTask(initialMessage);
        TaskStatus newStatus = new TaskStatus("IN_PROGRESS", "Task is now being processed");
        
        // When
        Task updatedTask = taskService.updateTaskStatus(task.getId(), newStatus);
          // Then
        assertNotNull(updatedTask);
        assertEquals("IN_PROGRESS", updatedTask.getStatus().getState());
        assertEquals("Task is now being processed", updatedTask.getStatus().getMessage());
    }

    @Test
    void addMessageToTask_ShouldAddMessageToExistingTask() {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing trends");
        Task task = taskService.createTask(initialMessage);
        Message newMessage = new Message("agent", "text/plain", "Here are the latest trends in quantum computing");
        
        // When
        Task updatedTask = taskService.addMessageToTask(task.getId(), newMessage);
        
        // Then
        assertNotNull(updatedTask);
        assertEquals(2, updatedTask.getMessages().size());
        assertEquals("user", updatedTask.getMessages().get(0).getRole());
        assertEquals("agent", updatedTask.getMessages().get(1).getRole());
    }

    @Test
    void addArtifactToTask_ShouldAddArtifactToExistingTask() {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing trends");
        Task task = taskService.createTask(initialMessage);
        TaskArtifact artifact = new TaskArtifact("research_summary", "application/pdf", "content of the PDF");
        
        // When
        Task updatedTask = taskService.addArtifactToTask(task.getId(), artifact);
          // Then
        assertNotNull(updatedTask);
        assertEquals(1, updatedTask.getArtifacts().size());
        assertEquals("research_summary", updatedTask.getArtifacts().get(0).getType());
        assertEquals("application/pdf", updatedTask.getArtifacts().get(0).getContentType());
    }

    @Test
    void cancelTask_ShouldSetTaskStatusToCancelled() throws IOException {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing trends");
        Task task = taskService.createTask(initialMessage);
        
        // When
        Task cancelledTask = taskService.cancelTask(task.getId());
          // Then
        assertNotNull(cancelledTask);
        assertEquals("CANCELLED", cancelledTask.getStatus().getState());
        assertEquals("Task was cancelled by user request", cancelledTask.getStatus().getMessage());
        
        // Verify that the listener was notified
        verify(mockListener).sendTaskUpdate(eq(task.getId()), any(Task.class), eq("task_status_update"));
    }

    @Test
    void processStreamingMessage_ShouldStartProcessingAndNotifyListener() throws IOException, InterruptedException {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing trends");
        Task task = taskService.createTask(initialMessage);
        
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("contentType", "text/plain");
        messageMap.put("content", "Tell me more about quantum entanglement");
        
        // When
        taskService.processStreamingMessage(task.getId(), messageMap);
        
        // Allow task processing to start
        TimeUnit.MILLISECONDS.sleep(100);
        
        // Then
        // Verify task status update was sent at least once
        verify(mockListener, atLeastOnce()).sendTaskUpdate(eq(task.getId()), any(), eq("task_status_update"));
    }
}
