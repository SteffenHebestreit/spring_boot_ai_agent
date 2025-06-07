package com.steffenhebestreit.ai_research.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.client.ClientHttpRequestFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.steffenhebestreit.ai_research.Configuration.IntegrationProperties;
import com.steffenhebestreit.ai_research.Configuration.McpServerConfig;

/**
 * Unit tests for HTTP 304 "Not Modified" handling in DynamicIntegrationService.
 * 
 * This test validates that HTTP 304 responses from MCP servers are correctly
 * treated as successful tool executions rather than errors.
 */
public class DynamicIntegrationServiceHttp304Test {

    @Mock
    private IntegrationProperties integrationProperties;
    
    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    
    @Mock
    private RestTemplate restTemplate;
    
    private DynamicIntegrationService dynamicIntegrationService;
    private McpServerConfig testServerConfig;    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Create test server config
        testServerConfig = new McpServerConfig();
        testServerConfig.setName("test-server");
        testServerConfig.setUrl("http://test-server.local");
        
        // Setup integration properties with our test server
        List<McpServerConfig> mcpServers = new ArrayList<>();
        mcpServers.add(testServerConfig);
        when(integrationProperties.getMcpServers()).thenReturn(mcpServers);
        when(integrationProperties.getA2aPeers()).thenReturn(new ArrayList<>());        // Setup RestTemplateBuilder chain to return our mock RestTemplate
        when(restTemplateBuilder.requestFactory(Mockito.<Supplier<ClientHttpRequestFactory>>any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        // Mock session initialization response - this will be called by executeToolCall
        Map<String, Object> sessionInitResponse = new HashMap<>();
        sessionInitResponse.put("jsonrpc", "2.0");
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", "test-session-123");
        sessionInitResponse.put("result", result);        // Setup RestTemplate to handle both session initialization and tool calls
        // Mock session initialization - uses exchange() method
        when(restTemplate.exchange(eq("http://test-server.local/mcp"), eq(HttpMethod.POST), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(
                "{\"jsonrpc\":\"2.0\",\"result\":{\"sessionId\":\"test-session-123\"}}", 
                HttpStatus.OK));
        
        // Setup sophisticated mocking for postForObject calls to distinguish between 
        // session validation and tool execution calls
        when(restTemplate.postForObject(eq("http://test-server.local/mcp"), any(), eq(Map.class)))
            .thenAnswer(invocation -> {
                // Get the request entity from the method call
                Object requestEntityArg = invocation.getArgument(1);
                if (requestEntityArg instanceof HttpEntity) {
                    HttpEntity<?> requestEntity = (HttpEntity<?>) requestEntityArg;
                    Object body = requestEntity.getBody();
                    
                    if (body instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> requestBody = (Map<String, Object>) body;
                        String method = (String) requestBody.get("method");
                          if ("tools/list".equals(method)) {
                            // This is a session validation call - allow it to succeed
                            Map<String, Object> validationResponse = new HashMap<>();
                            validationResponse.put("jsonrpc", "2.0");
                            Map<String, Object> toolsResult = new HashMap<>();
                            toolsResult.put("tools", new ArrayList<>());
                            validationResponse.put("result", toolsResult);
                            return validationResponse;
                        } else if ("tools/call".equals(method)) {
                            // This is an actual tool execution call - throw HTTP 304
                            String responseBody = "{\"jsonrpc\":\"2.0\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Found 4 pages matching your query\"}]}}";
                            throw new HttpClientErrorException(
                                HttpStatus.NOT_MODIFIED, 
                                "Not Modified", 
                                responseBody.getBytes(), 
                                null
                            );
                        }
                    }
                }
                  // Default response for any other calls
                Map<String, Object> defaultResponse = new HashMap<>();
                defaultResponse.put("jsonrpc", "2.0");
                defaultResponse.put("result", new HashMap<>());
                return defaultResponse;
            });
        
        // Mock the initialized notification call (uses postForObject with Object.class return type)
        when(restTemplate.postForObject(eq("http://test-server.local/mcp"), any(), eq(Object.class)))
            .thenReturn(new HashMap<>());
        
        // Create service instance
        dynamicIntegrationService = new DynamicIntegrationService(integrationProperties, restTemplateBuilder);
        
        // Use reflection to populate the discoveredMcpTools field so executeToolCall can find the server
        populateDiscoveredTools();
    }
    
    /**
     * Populates the discoveredMcpTools field using reflection so the service
     * can find which server provides the "search" tool.
     */
    private void populateDiscoveredTools() throws Exception {
        // Create a mock tool that the "search" tool calls will find
        Map<String, Object> searchTool = new HashMap<>();
        searchTool.put("name", "search");
        searchTool.put("description", "Search for information");
        searchTool.put("sourceMcpServerName", "test-server");
        searchTool.put("sourceMcpServerUrl", "http://test-server.local");
        
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(searchTool);
        
        // Use reflection to access the private discoveredMcpTools field
        Field discoveredMcpToolsField = DynamicIntegrationService.class.getDeclaredField("discoveredMcpTools");
        discoveredMcpToolsField.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> discoveredTools = (List<Map<String, Object>>) discoveredMcpToolsField.get(dynamicIntegrationService);
        discoveredTools.clear();
        discoveredTools.addAll(tools);
    }    @Test
    void testHttp304ResponseTreatedAsSuccess() {
        // Create test parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "test search");
        
        // Create HTTP 304 exception with response body containing successful result
        String responseBody = "{\"jsonrpc\":\"2.0\",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"Found 4 pages matching your query\"}]}}";
        HttpClientErrorException http304Exception = new HttpClientErrorException(
            HttpStatus.NOT_MODIFIED, 
            "Not Modified", 
            responseBody.getBytes(), 
            null
        );
        
        // Override the mock for tool execution calls only, keeping session validation mocks intact
        when(restTemplate.postForObject(eq("http://test-server.local/mcp"), argThat(request -> {
            if (request instanceof HttpEntity) {
                HttpEntity<?> httpEntity = (HttpEntity<?>) request;
                if (httpEntity.getBody() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) httpEntity.getBody();
                    String method = (String) body.get("method");
                    // Only throw HTTP 304 for actual tool calls
                    if ("tools/call".equals(method)) {
                        return true;
                    }
                }
            }
            return false;
        }), eq(Map.class))).thenThrow(http304Exception);
        
        // Execute tool call
        Map<String, Object> result = dynamicIntegrationService.executeToolCall("search", parameters);
        
        // Verify that HTTP 304 is treated as success
        assertNotNull(result, "HTTP 304 response should be treated as successful");
        assertTrue(result.containsKey("result"), "Response should contain result key");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultContent = (Map<String, Object>) result.get("result");
        assertNotNull(resultContent, "Result content should not be null");
        assertTrue(resultContent.containsKey("content"), "Result should contain content");
    }    @Test
    void testHttp304WithoutResponseBodyCreatesSuccessResponse() {
        // Create test parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "test search");
        
        // Create HTTP 304 exception without response body
        HttpClientErrorException http304Exception = new HttpClientErrorException(
            HttpStatus.NOT_MODIFIED, 
            "Not Modified"
        );
        
        // Override the mock for tool execution calls only, keeping session validation mocks intact
        when(restTemplate.postForObject(eq("http://test-server.local/mcp"), argThat(request -> {
            if (request instanceof HttpEntity) {
                HttpEntity<?> httpEntity = (HttpEntity<?>) request;
                if (httpEntity.getBody() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) httpEntity.getBody();
                    String method = (String) body.get("method");
                    // Only throw HTTP 304 for actual tool calls
                    if ("tools/call".equals(method)) {
                        return true;
                    }
                }
            }
            return false;
        }), eq(Map.class))).thenThrow(http304Exception);
        
        // Execute tool call
        Map<String, Object> result = dynamicIntegrationService.executeToolCall("search", parameters);
        
        // Verify that HTTP 304 creates a cache success response
        assertNotNull(result, "HTTP 304 without body should create success response");
        assertTrue(result.containsKey("result"), "Response should contain result key");
        assertEquals("2.0", result.get("jsonrpc"), "Response should have correct JSON-RPC version");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultContent = (Map<String, Object>) result.get("result");
        assertNotNull(resultContent, "Result content should not be null");
        assertTrue(resultContent.containsKey("content"), "Result should contain content indicating cache hit");
    }    @Test
    void testOtherHttpErrorsStillTreatedAsErrors() {
        // Create test parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "test search");
        
        // Create HTTP 404 exception (should still be treated as error)
        HttpClientErrorException http404Exception = new HttpClientErrorException(
            HttpStatus.NOT_FOUND, 
            "Not Found"
        );
        
        // Override the mock for tool execution calls only, keeping session validation mocks intact
        when(restTemplate.postForObject(eq("http://test-server.local/mcp"), argThat(request -> {
            if (request instanceof HttpEntity) {
                HttpEntity<?> httpEntity = (HttpEntity<?>) request;
                if (httpEntity.getBody() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) httpEntity.getBody();
                    String method = (String) body.get("method");
                    // Only throw HTTP 404 for actual tool calls
                    if ("tools/call".equals(method)) {
                        return true;
                    }
                }
            }
            return false;
        }), eq(Map.class))).thenThrow(http404Exception);
        
        // Execute tool call
        Map<String, Object> result = dynamicIntegrationService.executeToolCall("search", parameters);
        
        // Verify that HTTP 404 is still treated as error
        assertNull(result, "HTTP 404 should still be treated as error and return null");
    }
}
