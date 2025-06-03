package com.steffenhebestreit.ai_research.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Model.Task;
import com.steffenhebestreit.ai_research.Model.TaskStatus;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import com.steffenhebestreit.ai_research.Service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link TaskController}
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private OpenAIService openAIService;

    @MockBean
    private ChatService chatService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }    @Test
    @WithMockUser(username = "user")
    void createTask_ShouldReturnCreatedStatus() throws Exception {
        // Given
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing");
        Task mockTask = createMockTask(initialMessage);
        
        when(taskService.createTask(any(Message.class))).thenReturn(mockTask);        // When & Then
        mockMvc.perform(post("/research-agent/api/tasks/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialMessage)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.id").value(mockTask.getId()))
                .andExpect(jsonPath("$.result.status.state").value("PENDING"));
    }
      @Test
    @WithMockUser(username = "user")
    void getTask_WithExistingId_ShouldReturnTask() throws Exception {
        // Given
        String taskId = UUID.randomUUID().toString();
        Message initialMessage = new Message("user", "text/plain", "Research quantum computing");
        Task mockTask = createMockTask(initialMessage);
        mockTask.setId(taskId);
        
        when(taskService.getTask(taskId)).thenReturn(mockTask);        // When & Then
        mockMvc.perform(get("/research-agent/api/tasks/{taskId}/get", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(taskId))
                .andExpect(jsonPath("$.result.messages[0].role").value("user"));
    }
      @Test
    @WithMockUser(username = "user")
    void getTask_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        // Given
        String nonExistingTaskId = "non-existing-id";
        when(taskService.getTask(nonExistingTaskId)).thenReturn(null);        // When & Then
        mockMvc.perform(get("/research-agent/api/tasks/{taskId}/get", nonExistingTaskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("Task not found with ID: " + nonExistingTaskId));
    }
      @Test
    @WithMockUser(username = "user")
    void sendMessage_ToExistingTask_ShouldReturnUpdatedTask() throws Exception {
        // Given
        String taskId = UUID.randomUUID().toString();
        Message initialMessage = new Message("user", "text/plain", "Initial message");
        Task mockTask = createMockTask(initialMessage);
        mockTask.setId(taskId);
        
        Message newMessage = new Message("user", "text/plain", "Follow-up question");
        mockTask.addMessage(newMessage);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("taskId", taskId);
        
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("contentType", "text/plain");
        messageMap.put("content", "Follow-up question");
        
        requestBody.put("message", messageMap);
        
        when(taskService.addMessageToTask(anyString(), any(Message.class))).thenReturn(mockTask);        // When & Then
        mockMvc.perform(post("/research-agent/api/message/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(taskId))
                .andExpect(jsonPath("$.result.messages.length()").value(2));
    }
      @Test
    @WithMockUser(username = "user")
    void cancelTask_ExistingTask_ShouldReturnCancelledTask() throws Exception {
        // Given
        String taskId = UUID.randomUUID().toString();
        Task mockTask = createMockTask(new Message("user", "text/plain", "Research task"));
        mockTask.setId(taskId);
        mockTask.setStatus(new TaskStatus("CANCELLED", "Task was cancelled by user request"));
        
        when(taskService.cancelTask(taskId)).thenReturn(mockTask);        // When & Then
        mockMvc.perform(post("/research-agent/api/tasks/{taskId}/cancel", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(taskId))
                .andExpect(jsonPath("$.result.status.state").value("CANCELLED"));
    }
      @Test
    @WithMockUser(username = "user")
    void cancelTask_NonExistingTask_ShouldReturnNotFound() throws Exception {
        // Given
        String nonExistingTaskId = "non-existing-id";
        when(taskService.cancelTask(nonExistingTaskId)).thenReturn(null);        // When & Then
        mockMvc.perform(post("/research-agent/api/tasks/{taskId}/cancel", nonExistingTaskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(-32602));
    }
    
    /**
     * Helper method to create a mock Task with initial message
     */
    private Task createMockTask(Message initialMessage) {
        Task task = new Task();
        task.addMessage(initialMessage);
        return task;
    }
}
