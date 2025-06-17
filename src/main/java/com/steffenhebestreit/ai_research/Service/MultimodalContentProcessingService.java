package com.steffenhebestreit.ai_research.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing and transforming multimodal content in chat history.
 * <p>
 * This service provides specialized methods for handling multimodal content in chat history,
 * particularly focusing on optimizing content for LLM context management.
 * <p>
 * Key features:
 * <ul>
 *   <li>Extracting text-only content from multimodal messages</li>
 *   <li>Creating text-only representations of previous multimodal messages</li>
 *   <li>Optimizing chat history to reduce token usage while preserving context</li>
 * </ul>
 */
@Service
public class MultimodalContentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalContentProcessingService.class);
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Extracts text content from a multimodal message, creating a new text-only representation.
     * <p>
     * This method parses the content of a multimodal message and extracts only the text portion,
     * discarding any file data (images, PDFs, etc.). This is useful for reducing token usage
     * when reusing previous messages in LLM conversations.
     * <p>
     * For multipart/mixed content, it attempts to parse the JSON structure and extract only the text parts.
     * For other content types, it returns the original content unchanged.
     * 
     * @param message The original message that may contain multimodal content
     * @return A new Message object with the same role but containing only text content
     */    public Message extractTextOnlyContent(ChatMessage message) {
        if (message == null || message.getContent() == null) {
            logger.debug("Cannot extract text from null message or content");
            return null;
        }
        
        // If not multimodal content, return as is
        if (!"multipart/mixed".equals(message.getContentType())) {
            logger.debug("Message is not multimodal, returning as is with type: {}", message.getContentType());
            return new Message(message.getRole(), message.getContentType(), message.getContent());
        }
        
        try {
            // For multimodal content, parse the JSON and extract only the text
            JsonNode contentNode = objectMapper.readTree(message.getContent());
            
            // Handle array format (most common for multimodal content)
            if (contentNode.isArray()) {
                StringBuilder textContent = new StringBuilder();
                int textPartsFound = 0;
                
                // Loop through all content parts
                for (JsonNode part : contentNode) {
                    if (part.has("type") && "text".equals(part.get("type").asText()) && part.has("text")) {
                        if (textContent.length() > 0) {
                            textContent.append("\n");
                        }
                        String textPart = part.get("text").asText();
                        textContent.append(textPart);
                        textPartsFound++;
                        logger.debug("Extracted text part ({} chars): {}", 
                                    textPart.length(),
                                    textPart.substring(0, Math.min(50, textPart.length())) + "...");
                    } else if (part.has("type")) {
                        logger.debug("Skipping non-text part of type: {}", part.get("type").asText());
                    }
                }
                
                if (textContent.length() > 0) {
                    logger.info("Successfully extracted text from multimodal content: {} text parts, {} total chars", 
                               textPartsFound, textContent.length());
                    return new Message(message.getRole(), "text/plain", textContent.toString());
                } else {
                    logger.warn("No text content found in multimodal message");
                }
            } else {
                logger.warn("Unexpected multimodal content structure: not an array");
            }            
            // Fallback: For unexpected structure, create a placeholder
            return new Message(message.getRole(), "text/plain", 
                    "[This message contained an image or other file that has been omitted to save context space]");
            
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse multimodal content: {}", e.getMessage());
            // Fallback if parsing fails
            return new Message(message.getRole(), "text/plain", 
                    "[This message contained unparseable multimodal content that has been omitted]");
        }
    }
    
    /**
     * Converts a list of chat messages to a text-only representation, removing file data.
     * <p>
     * This method processes a list of chat messages and converts any multimodal messages
     * to text-only representations. This is useful for optimizing context usage when sending
     * previous messages to an LLM, as it preserves the conversational context without including
     * large file data that would consume tokens unnecessarily.
     * <p>
     * The current message (typically the newest one) can be preserved with its full multimodal content
     * by providing its index.
     * 
     * @param messages The list of chat messages to process
     * @param preserveFullContentForIndex The index of the message to preserve with full multimodal content
     *                                    (typically the most recent message), or -1 to convert all messages
     * @return A new list of messages with multimodal content converted to text-only
     */    public List<Message> convertToTextOnlyHistory(List<ChatMessage> messages, int preserveFullContentForIndex) {
        if (messages == null || messages.isEmpty()) {
            logger.debug("No messages to convert to text-only history");
            return new ArrayList<>();
        }
        
        List<Message> processedMessages = new ArrayList<>();
        int textOnlyCount = 0;
        int preservedCount = 0;
        
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage original = messages.get(i);
            
            // If this is the message to preserve with full content
            if (i == preserveFullContentForIndex) {
                processedMessages.add(new Message(
                    original.getRole(), 
                    original.getContentType(), 
                    original.getContent()
                ));
                preservedCount++;
                logger.debug("Preserving full content for message at index {}", i);
            } else {
                // For all other messages, extract text-only content
                Message textOnlyMsg = extractTextOnlyContent(original);
                if (textOnlyMsg != null) {
                    processedMessages.add(textOnlyMsg);
                    textOnlyCount++;
                    logger.debug("Converted message at index {} to text-only", i);
                }
            }
        }
        
        logger.info("Converted {} messages to text-only, preserved {} with full content", 
                   textOnlyCount, preservedCount);
        return processedMessages;
    }
}
