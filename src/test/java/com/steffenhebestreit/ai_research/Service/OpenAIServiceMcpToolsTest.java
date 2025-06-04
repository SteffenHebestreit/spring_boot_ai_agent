package com.steffenhebestreit.ai_research.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.ChatMessage;

/**
 * Unit tests for the OpenAIService class, focusing on MCP tool integration.
 */
public class OpenAIServiceMcpToolsTest {

    @Mock
    private OpenAIProperties openAIProperties;
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
      @Mock
    private DynamicIntegrationService dynamicIntegrationService;
    
    @Mock
    private LlmCapabilityService llmCapabilityService;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    private OpenAIService openAIService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup WebClient mock chain
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        // Mock OpenAI properties
        when(openAIProperties.getBaseurl()).thenReturn("http://test-api.local");
        when(openAIProperties.getKey()).thenReturn("test-api-key");
        when(openAIProperties.getModel()).thenReturn("test-model");
          // Initialize service with mocks
        openAIService = new OpenAIService(openAIProperties, webClientBuilder, objectMapper, dynamicIntegrationService, llmCapabilityService);
    }
    
    @Test
    void testMcpToolsIncludedInRequestWhenAvailable() {
        // Create mock MCP tools
        List<Map<String, Object>> mockTools = new ArrayList<>();
        Map<String, Object> tool1 = new HashMap<>();
        Map<String, Object> function1 = new HashMap<>();
        function1.put("name", "testTool1");
        function1.put("description", "A test tool");
        tool1.put("type", "function");
        tool1.put("function", function1);
        mockTools.add(tool1);
        
        // Setup dynamic integration service to return mock tools
        when(dynamicIntegrationService.getDiscoveredMcpTools()).thenReturn(mockTools);
        
        // Create test messages
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent("Test message");
        messages.add(message);
        
        // Capture the request body
        ArgumentCaptor<Map<String, Object>> requestBodyCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Execute the method (we don't care about the response in this test)
        try {
            openAIService.getChatCompletionStream(messages);
        } catch (Exception e) {
            // Ignore any exceptions as we're just testing the request construction
        }
        
        // Verify the bodyValue method was called and capture the request body
        verify(requestBodySpec).bodyValue(requestBodyCaptor.capture());
        
        // Get the captured request body
        Map<String, Object> requestBody = requestBodyCaptor.getValue();
        
        // Verify tools were included in the request
        assertTrue(requestBody.containsKey("tools"), "Request should include tools");
        assertEquals(mockTools, requestBody.get("tools"), "Request should include the correct tools");
    }
    
    @Test
    void testNoMcpToolsIncludedWhenNoneAvailable() {
        // Setup dynamic integration service to return empty tools list
        when(dynamicIntegrationService.getDiscoveredMcpTools()).thenReturn(new ArrayList<>());
        
        // Create test messages
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent("Test message");
        messages.add(message);
        
        // Capture the request body
        ArgumentCaptor<Map<String, Object>> requestBodyCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Execute the method (we don't care about the response in this test)
        try {
            openAIService.getChatCompletionStream(messages);
        } catch (Exception e) {
            // Ignore any exceptions as we're just testing the request construction
        }
        
        // Verify the bodyValue method was called and capture the request body
        verify(requestBodySpec).bodyValue(requestBodyCaptor.capture());
        
        // Get the captured request body
        Map<String, Object> requestBody = requestBodyCaptor.getValue();
        
        // Verify tools were not included in the request
        assertFalse(requestBody.containsKey("tools"), "Request should not include tools when none are available");
    }
    
    /**
     * Test the complete tool execution workflow with getChatCompletionStreamWithToolExecution.
     */
    @Test
    public void testGetChatCompletionStreamWithToolExecution() {
        // Setup
        List<ChatMessage> messages = createSampleConversation();
        String modelId = "gpt-4";
        
        // Mock MCP tools
        List<Map<String, Object>> mcpTools = createSampleMcpTools();
        when(dynamicIntegrationService.getDiscoveredMcpTools()).thenReturn(mcpTools);
        
        // Mock tool execution result
        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("result", "Tool executed successfully");
        when(dynamicIntegrationService.executeToolCall(anyString(), anyMap())).thenReturn(toolResult);
          // Test the method exists and can be called
        assertDoesNotThrow(() -> {
            var flux = openAIService.getChatCompletionStreamWithToolExecution(messages, modelId);
            assertNotNull(flux, "Tool execution streaming method should return a non-null Flux");
        });
    }
    
    /**
     * Test the convenience overload method that uses default model.
     */
    @Test
    public void testGetChatCompletionStreamWithToolExecutionDefaultModel() {
        // Setup
        List<ChatMessage> messages = createSampleConversation();
        when(openAIProperties.getModel()).thenReturn("gpt-3.5-turbo");
        
        // Mock MCP tools
        List<Map<String, Object>> mcpTools = createSampleMcpTools();
        when(dynamicIntegrationService.getDiscoveredMcpTools()).thenReturn(mcpTools);
        
        // Test the convenience method exists and can be called
        assertDoesNotThrow(() -> {
            var flux = openAIService.getChatCompletionStreamWithToolExecution(messages);
            assertNotNull(flux, "Tool execution streaming convenience method should return a non-null Flux");
        });
    }
    
    /**
     * Creates sample MCP tools for testing.
     */
    private List<Map<String, Object>> createSampleMcpTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        Map<String, Object> tool1 = new HashMap<>();
        tool1.put("name", "test_tool");
        tool1.put("description", "A test tool for verification");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> queryParam = new HashMap<>();
        queryParam.put("type", "string");
        queryParam.put("description", "Test parameter");
        properties.put("query", queryParam);
        
        parameters.put("properties", properties);
        tool1.put("parameters", parameters);
        
        tools.add(tool1);
        
        return tools;
    }
    
    /**
     * Creates a sample conversation for testing.
     */
    private List<ChatMessage> createSampleConversation() {
        List<ChatMessage> messages = new ArrayList<>();
        
        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent("Please use the test tool to help me");
        messages.add(userMessage);
        
        return messages;
    }
}
