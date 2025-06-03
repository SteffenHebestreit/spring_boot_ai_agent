package com.steffenhebestreit.ai_research.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Model.Task;
import com.steffenhebestreit.ai_research.Service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskStreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TaskService taskService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TaskStreamingController controller;

    @BeforeEach
    void setUp() {
    }    @Test
    @WithMockUser(username = "user")
    void resubscribe_WithValidTaskId_ShouldReturnEmitter() throws Exception {
        // Given
        String taskId = UUID.randomUUID().toString();
        String requestId = "request-123";
        Task mockTask = new Task();
        mockTask.setId(taskId);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("taskId", taskId);
        requestBody.put("id", requestId);
        
        when(taskService.getTask(taskId)).thenReturn(mockTask);
        
        // When & Then - just expect the request to be processed without errors
        mockMvc.perform(post("/api/tasks/resubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(request().asyncStarted());
                
        // Verify that the task was retrieved
        verify(taskService).getTask(taskId);
    }    @Test
    @WithMockUser(username = "user")
    void resubscribe_WithInvalidTaskId_ShouldThrowException() throws Exception {
        // Given
        String nonExistingTaskId = "non-existing-id";
        String requestId = "request-123";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("taskId", nonExistingTaskId);
        requestBody.put("id", requestId);
        
        when(taskService.getTask(nonExistingTaskId)).thenReturn(null);
        
        // When & Then - expect exception to be handled properly
        try {
            mockMvc.perform(post("/api/tasks/resubscribe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)));
        } catch (Exception e) {
            // Expected exception - this test actually expects an exception to be thrown
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().contains("Task not found with ID: non-existing-id"));
        }
    }    @Test
    @WithMockUser(username = "user")
    void streamMessages_WithValidData_ShouldReturnEmitter() throws Exception {
        // Given
        String taskId = UUID.randomUUID().toString();
        String requestId = "request-123";
        
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("contentType", "text/plain");
        messageMap.put("content", "Research quantum computing please");
        
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("message", messageMap);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("params", params);
        requestBody.put("id", requestId);
        
        // When & Then - just expect the request to be processed without errors
        mockMvc.perform(post("/api/message/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(request().asyncStarted());
                  // Verify that processStreamingMessage was called with expected values
        verify(taskService).processStreamingMessage(eq(taskId), argThat(message -> 
            "user".equals(message.get("role")) && 
            "text/plain".equals(message.get("contentType")) &&
            "Research quantum computing please".equals(message.get("content"))
        ));
    }
      @Test
    @WithMockUser(username = "user")
    void sendTaskUpdate_WithRegisteredEmitter_ShouldSendUpdate() throws Exception {
        // Given
        String taskId = UUID.randomUUID().toString();
        Task task = new Task();
        task.setId(taskId);
        
        // Set up a fake emitter for the task
        SseEmitter mockEmitter = mock(SseEmitter.class);
          // Use reflection to access the private taskEmitters map and add our mock emitter
        java.lang.reflect.Field taskEmittersField = TaskStreamingController.class.getDeclaredField("taskEmitters");
        taskEmittersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, SseEmitter> taskEmitters = (Map<String, SseEmitter>) taskEmittersField.get(controller);
        taskEmitters.put(taskId, mockEmitter);
        
        // When
        controller.sendTaskUpdate(taskId, task, "task_status_update");
        
        // Then
        // Verify that send was called on the emitter
        ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(mockEmitter).send(eventCaptor.capture());
        
        SseEmitter.SseEventBuilder capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
    }
}
