package com.steffenhebestreit.ai_research.Manual;

import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Manual test to verify what fields are included when ChatMessage is serialized to JSON
 * This helps us understand what the frontend receives when loading chat history.
 */
public class ChatHistorySerializationTest {
    
    public static void main(String[] args) {
        try {
            // Create a sample ChatMessage with both content and rawContent
            ChatMessage message = new ChatMessage("assistant", "This is the filtered content for LLM.");
            message.setRawContent("This is the filtered content for LLM. <think>Here's my internal reasoning that should be hidden from LLM but shown to user.</think> [Tool calls requested by LLM. Executing tools...]");
            
            // Create ObjectMapper (same as used by Spring Boot)
            ObjectMapper mapper = new ObjectMapper();
            
            // Serialize to JSON
            String json = mapper.writeValueAsString(message);
            
            System.out.println("=== ChatMessage JSON Serialization ===");
            System.out.println("JSON Output:");
            System.out.println(json);
            
            // Parse back to see the structure
            JsonNode jsonNode = mapper.readTree(json);
            
            System.out.println("\n=== Field Analysis ===");
            System.out.println("Has 'content' field: " + jsonNode.has("content"));
            System.out.println("Has 'rawContent' field: " + jsonNode.has("rawContent"));
            
            if (jsonNode.has("content")) {
                System.out.println("Content value: " + jsonNode.get("content").asText());
            }
            
            if (jsonNode.has("rawContent")) {
                System.out.println("RawContent value: " + jsonNode.get("rawContent").asText());
            }
            
            System.out.println("\n=== Conclusion ===");
            if (jsonNode.has("rawContent")) {
                System.out.println("✅ rawContent IS included in JSON serialization");
                System.out.println("👉 Frontend WILL receive tool calls and think tags in rawContent field");
            } else {
                System.out.println("❌ rawContent is NOT included in JSON serialization");
                System.out.println("👉 Frontend will only receive filtered content");
            }
            
        } catch (Exception e) {
            System.err.println("Error during serialization test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
