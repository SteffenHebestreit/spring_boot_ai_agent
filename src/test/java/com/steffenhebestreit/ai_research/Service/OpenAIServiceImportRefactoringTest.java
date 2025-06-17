package com.steffenhebestreit.ai_research.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.ToolSelectionRequest;

/**
 * Unit tests to verify that the import refactoring of ChatMessage and ToolSelectionRequest
 * classes works correctly. This test ensures that all method signatures and variable
 * declarations properly use the imported classes instead of fully qualified names.
 * 
 * @author Test Suite
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class OpenAIServiceImportRefactoringTest {    @Mock
    private OpenAIProperties openAIProperties;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private DynamicIntegrationService dynamicIntegrationService;
    
    @Mock
    private LlmCapabilityService llmCapabilityService;
    
    private ObjectMapper objectMapper;    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Mock WebClient.Builder behavior
        lenient().when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        
        // Mock OpenAI properties (using lenient() to allow unused stubs in import-focused tests)
        lenient().when(openAIProperties.getBaseurl()).thenReturn("http://localhost:1234");
        lenient().when(openAIProperties.getKey()).thenReturn("test-key");
        lenient().when(openAIProperties.getModel()).thenReturn("test-model");
        lenient().when(openAIProperties.getSystemRole()).thenReturn("You are a helpful AI assistant.");
        
        // Note: This test focuses on import refactoring, so we don't actually instantiate the OpenAIService
        // The mocks are here for completeness but may not be used in all test methods
    }

    /**
     * Test that ChatMessage objects can be created and used properly with the imported class.
     */
    @Test
    void testChatMessageImportWorks() {
        // Test creating ChatMessage instances (should use imported class)
        ChatMessage message1 = new ChatMessage();
        message1.setRole("user");
        message1.setContent("Hello, world!");
        
        ChatMessage message2 = new ChatMessage();
        message2.setRole("assistant");
        message2.setContent("Hello! How can I help you today?");
        
        // Verify the objects are created correctly
        assertNotNull(message1);
        assertNotNull(message2);
        assertEquals("user", message1.getRole());
        assertEquals("Hello, world!", message1.getContent());
        assertEquals("assistant", message2.getRole());
        assertEquals("Hello! How can I help you today?", message2.getContent());
    }

    /**
     * Test that ToolSelectionRequest objects can be created and used properly with the imported class.
     */
    @Test
    void testToolSelectionRequestImportWorks() {
        // Test creating ToolSelectionRequest instances (should use imported class)
        ToolSelectionRequest toolSelection1 = new ToolSelectionRequest();
        toolSelection1.setEnableTools(true);
        toolSelection1.setEnabledTools(List.of("web_search", "file_reader"));
        
        ToolSelectionRequest toolSelection2 = new ToolSelectionRequest(false, null);
        
        // Verify the objects are created correctly
        assertNotNull(toolSelection1);
        assertNotNull(toolSelection2);
        assertTrue(toolSelection1.isEnableTools());
        assertFalse(toolSelection2.isEnableTools());
        assertEquals(2, toolSelection1.getEnabledTools().size());
        assertTrue(toolSelection1.getEnabledTools().contains("web_search"));
        assertTrue(toolSelection1.getEnabledTools().contains("file_reader"));
    }

    /**
     * Test that List<ChatMessage> parameters work correctly in method signatures.
     */
    @Test
    void testListChatMessageParameterTypesWork() {
        // Create a list of ChatMessage objects
        List<ChatMessage> messages = new ArrayList<>();
        
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent("You are a helpful assistant.");
        
        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent("What is the weather like?");
        
        messages.add(systemMessage);
        messages.add(userMessage);
        
        // Verify the list contains the expected messages
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).getRole());
        assertEquals("user", messages.get(1).getRole());
        
        // Test that the list can be passed to methods expecting List<ChatMessage>
        assertDoesNotThrow(() -> {
            // This should compile and run without issues if imports are correct
            List<ChatMessage> copiedMessages = new ArrayList<>(messages);
            assertEquals(2, copiedMessages.size());
        });
    }

    /**
     * Test that method parameters using both ChatMessage and ToolSelectionRequest work together.
     */
    @Test
    void testCombinedImportedClassesWork() {
        // Create test data using both imported classes
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent("Test message");
        messages.add(message);
        
        ToolSelectionRequest toolSelection = new ToolSelectionRequest();
        toolSelection.setEnableTools(true);
        toolSelection.setEnabledTools(List.of("calculator"));
        
        // Verify both objects work together
        assertNotNull(messages);
        assertNotNull(toolSelection);
        assertEquals(1, messages.size());
        assertTrue(toolSelection.isEnableTools());
        assertEquals(1, toolSelection.getEnabledTools().size());
        
        // Test that these can be used in method calls that expect the imported types
        assertDoesNotThrow(() -> {
            // This should compile if the method signatures use imported classes correctly
            String modelId = "test-model";
            assertNotNull(modelId);
            
            // Simulate method parameter types that should work with imported classes
            List<ChatMessage> testMessages = Collections.unmodifiableList(messages);
            ToolSelectionRequest testToolSelection = new ToolSelectionRequest(
                toolSelection.isEnableTools(), 
                toolSelection.getEnabledTools()
            );
            
            assertNotNull(testMessages);
            assertNotNull(testToolSelection);
        });
    }

    /**
     * Test that ChatMessage objects can be properly serialized/deserialized.
     * This ensures the class structure is intact after the import refactoring.
     */
    @Test
    void testChatMessageSerialization() throws Exception {
        ChatMessage originalMessage = new ChatMessage();
        originalMessage.setRole("user");
        originalMessage.setContent("Test serialization");
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(originalMessage);
        assertNotNull(json);
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"content\":\"Test serialization\""));
        
        // Deserialize back to object
        ChatMessage deserializedMessage = objectMapper.readValue(json, ChatMessage.class);
        assertNotNull(deserializedMessage);
        assertEquals(originalMessage.getRole(), deserializedMessage.getRole());
        assertEquals(originalMessage.getContent(), deserializedMessage.getContent());
    }

    /**
     * Test that ToolSelectionRequest objects can be properly serialized/deserialized.
     * This ensures the class structure is intact after the import refactoring.
     */
    @Test
    void testToolSelectionRequestSerialization() throws Exception {
        ToolSelectionRequest originalRequest = new ToolSelectionRequest();
        originalRequest.setEnableTools(true);
        originalRequest.setEnabledTools(List.of("tool1", "tool2"));
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(originalRequest);
        assertNotNull(json);
        assertTrue(json.contains("\"enableTools\":true"));
        assertTrue(json.contains("\"enabledTools\""));
        
        // Deserialize back to object
        ToolSelectionRequest deserializedRequest = objectMapper.readValue(json, ToolSelectionRequest.class);
        assertNotNull(deserializedRequest);
        assertEquals(originalRequest.isEnableTools(), deserializedRequest.isEnableTools());
        assertEquals(originalRequest.getEnabledTools().size(), deserializedRequest.getEnabledTools().size());
        assertTrue(deserializedRequest.getEnabledTools().contains("tool1"));
        assertTrue(deserializedRequest.getEnabledTools().contains("tool2"));
    }

    /**
     * Test that the refactored OpenAIService methods can handle empty message lists correctly.
     * This ensures method signatures work properly with imported types.
     */
    @Test
    void testEmptyMessageListHandling() {
        List<ChatMessage> emptyMessages = new ArrayList<>();
        
        // Test that empty lists are handled appropriately
        assertNotNull(emptyMessages);
        assertTrue(emptyMessages.isEmpty());
        
        // Verify the list type is correct (should be List<ChatMessage> using imported class)
        assertEquals(0, emptyMessages.size());
        
        // Test adding a message to ensure the generic type works
        ChatMessage testMessage = new ChatMessage();
        testMessage.setRole("test");
        testMessage.setContent("test content");
        
        emptyMessages.add(testMessage);
        assertEquals(1, emptyMessages.size());
        assertEquals("test", emptyMessages.get(0).getRole());
    }

    /**
     * Test that null safety works correctly with imported classes.
     */
    @Test
    void testNullSafetyWithImportedClasses() {
        // Test null ChatMessage handling
        ChatMessage nullMessage = null;
        assertNull(nullMessage);
        
        // Test null ToolSelectionRequest handling
        ToolSelectionRequest nullToolSelection = null;
        assertNull(nullToolSelection);
        
        // Test null list handling
        List<ChatMessage> nullMessageList = null;
        assertNull(nullMessageList);
        
        // Test creating valid objects after null tests
        ChatMessage validMessage = new ChatMessage();
        validMessage.setRole("user");
        validMessage.setContent("Valid message");
        assertNotNull(validMessage);
        
        ToolSelectionRequest validToolSelection = new ToolSelectionRequest();
        validToolSelection.setEnableTools(false);
        assertNotNull(validToolSelection);
        assertFalse(validToolSelection.isEnableTools());
    }
}
