package com.steffenhebestreit.ai_research.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.ToolSelectionRequest;

/**
 * Simple unit tests to verify that the import refactoring of ChatMessage and ToolSelectionRequest
 * classes works correctly. This test ensures that the imported classes work properly
 * without any service dependencies.
 * 
 * @author Test Suite
 * @version 1.0
 */
public class SimpleImportRefactoringTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
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
        
        // Test that these types work correctly together
        assertDoesNotThrow(() -> {
            String modelId = "test-model";
            assertNotNull(modelId);
            
            // These should all compile correctly with imported classes
            List<ChatMessage> testMessages = new ArrayList<>(messages);
            ToolSelectionRequest testToolSelection = new ToolSelectionRequest(
                toolSelection.isEnableTools(), 
                new ArrayList<>(toolSelection.getEnabledTools())
            );
            
            assertNotNull(testMessages);
            assertNotNull(testToolSelection);
        });
    }
}
