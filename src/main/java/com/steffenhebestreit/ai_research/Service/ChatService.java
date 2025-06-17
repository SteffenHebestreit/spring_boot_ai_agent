package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Repository.ChatMessageRepository;
import com.steffenhebestreit.ai_research.Repository.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    public ChatService(ChatRepository chatRepository, ChatMessageRepository chatMessageRepository) {
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Get all chats, ordered by most recent update
     */
    public List<Chat> getAllChats() {
        return chatRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * Get a chat by ID
     */
    public Optional<Chat> getChatById(String chatId) {
        return chatRepository.findById(chatId);
    }    /**
     * Create a new chat with an initial message
     */    @Transactional
    public Chat createChat(Message initialMessage) {
        Chat chat = new Chat();
        
        // System message is no longer added here and saved to the database.
        // It will be dynamically prepended by OpenAIService before calling the LLM.
        // String systemRole = openAIProperties.getSystemRole();
        // if (systemRole != null && !systemRole.isEmpty()) {
        //     String timeAppendedSystemRole = systemRole + " Current time: " + Instant.now().toString() + ".";
        //     Message systemMessage = new Message("system", "text/plain", timeAppendedSystemRole);
        //     ChatMessage systemChatMessage = ChatMessage.fromMessage(systemMessage);
        //     // chat.addMessage(systemChatMessage); // Removed: Do not add to chat messages to be saved
        //     logger.debug("System message (not saved) for new chat would be: {}", 
        //         timeAppendedSystemRole.substring(0, Math.min(50, timeAppendedSystemRole.length())) + "...");
        // }
        
        // Add the user's initial message
        ChatMessage chatMessage = ChatMessage.fromMessage(initialMessage);
        chat.addMessage(chatMessage);
        // Ensure title is generated if this is the first actual message and content is suitable
        if (initialMessage.getContent() instanceof String && !((String) initialMessage.getContent()).trim().isEmpty()) {
            chat.generateTitleFromContent(); 
        }
        
        return chatRepository.save(chat);
    }    /**
     * Add a message to an existing chat
     */    @Transactional
    public Chat addMessageToChat(String chatId, Message message) {
        // Add enhanced logging for debugging message issues
        System.out.println("Adding message to chat " + chatId + " with role: " + message.getRole() + 
                           ", content type: " + message.getContentType() +
                           ", content preview: " + (message.getContent() instanceof String ? 
                               ((String)message.getContent()).substring(0, Math.min(50, ((String)message.getContent()).length())) + "..." 
                               : "Non-string content"));
        
        return getChatById(chatId).map(chat -> {            // Enhanced duplicate message detection with better logging
            for (ChatMessage existingMsg : chat.getMessages()) {                // Compare role and content to identify duplicates
                boolean sameRole = existingMsg.getRole().equals(message.getRole());
                boolean sameContentType = (existingMsg.getContentType() == null && message.getContentType() == null) ||
                                          (existingMsg.getContentType() != null && 
                                           existingMsg.getContentType().equals(message.getContentType()));
                
                boolean sameContent = false;
                String duplicateReason = null;                // Handle multimodal content comparison
                if ("multipart/mixed".equals(message.getContentType()) || "multipart/mixed".equals(existingMsg.getContentType())) {
                    if (message.getContent() != null && existingMsg.getContent() != null) {
                        // For multimodal content, be more strict about duplicate detection
                        // Only consider it a duplicate if it's EXACTLY the same content
                        String existingStr = existingMsg.getContent().toString();
                        String newStr = message.getContent().toString();
                        
                        // Only exact string matches are considered duplicates for multimodal content
                        // This prevents false positives from similar but different multimodal content
                        if (existingStr.equals(newStr)) {
                            sameContent = true;
                            duplicateReason = "exact multimodal content match";
                        }
                        // Additional safeguard: don't consider multimodal content duplicates
                        // unless they are truly identical (same timestamp, same content)
                        else if (existingStr.length() > 1000 && newStr.length() > 1000 && 
                                existingStr.equals(newStr)) {
                            sameContent = true;
                            duplicateReason = "identical large multimodal content";
                        }
                    }
                }
                // Regular string content comparison
                else if (existingMsg.getContent() instanceof String && message.getContent() instanceof String) {
                    String existingContent = (String) existingMsg.getContent();
                    String newContent = (String) message.getContent();
                    
                    // Check for exact match
                    if (existingContent.equals(newContent)) {
                        sameContent = true;
                        duplicateReason = "exact content match";
                    }
                    
                    // Check for substring match (for handling truncated responses)
                    // Only for agent/assistant responses which might be saved twice
                    else if (("agent".equals(message.getRole()) || "assistant".equals(message.getRole())) &&
                            existingContent.length() > 20 && newContent.length() > 20) {
                        if (existingContent.contains(newContent) || newContent.contains(existingContent)) {
                            sameContent = true;
                            duplicateReason = "content substring match";
                        }
                    }
                }
                // For non-string content types, compare their string representation
                else if (existingMsg.getContent() != null && message.getContent() != null) {
                    if (existingMsg.getContent().toString().equals(message.getContent().toString())) {
                        sameContent = true;
                        duplicateReason = "toString match on non-string content";
                    }
                }
                // For null content
                else if (existingMsg.getContent() == null && message.getContent() == null) {
                    sameContent = true;
                    duplicateReason = "both contents null";
                }
                  // Message is considered duplicate if role, content type and content match
                if (sameRole && sameContentType && sameContent) {
                    System.out.println("DUPLICATE MESSAGE DETECTED! Not saving duplicate message. Reason: " + duplicateReason);
                    return chat; // Return existing chat without adding the duplicate
                }
            }
            
            ChatMessage chatMessage = ChatMessage.fromMessage(message);            
            
            chat.addMessage(chatMessage);
            
            // If this is the first user message, generate a title
            if (chat.getTitle() == null || chat.getTitle().isEmpty()) {
                chat.generateTitleFromContent();
            }
            
            chat.setUpdatedAt(Instant.now());
            
            // Explicitly save the chat message first to ensure it's in the database
            chatMessageRepository.save(chatMessage);
            
            // Then save the chat
            return chatRepository.save(chat);
        }).orElseThrow(() -> new RuntimeException("Chat not found with ID: " + chatId));
    }    /**
     * Get all messages for a specific chat
     */
    public List<ChatMessage> getChatMessages(String chatId) {
        return chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    /**
     * Update chat title
     */
    @Transactional
    public Chat updateChatTitle(String chatId, String newTitle) {
        return getChatById(chatId).map(chat -> {
            chat.setTitle(newTitle);
            chat.setUpdatedAt(Instant.now());
            return chatRepository.save(chat);
        }).orElseThrow(() -> new RuntimeException("Chat not found with ID: " + chatId));
    }

    /**
     * Delete a chat
     */
    @Transactional
    public void deleteChat(String chatId) {
        chatRepository.deleteById(chatId);
    }
      /**
     * Mark all messages in a chat as read
     */
    @Transactional
    public void markChatAsRead(String chatId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        messages.forEach(message -> {
            message.setRead(true);
        });
        chatMessageRepository.saveAll(messages);
    }

    /**
     * Update the raw content of a message
     */
    @Transactional
    public void updateMessageRawContent(String messageId, String rawContent) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isPresent()) {
            ChatMessage message = messageOpt.get();
            message.setRawContent(rawContent);
            chatMessageRepository.save(message);
        }
    }

    /**
     * Get recent chats, ordered by most recent first, limited to 10
     * 
     * @return List of 10 most recent chats
     */
    public List<Chat> getRecentChats(int limit) {
        if (limit > 10) {
            limit = 10; // Cap at 10 for performance reasons
        }
        List<Chat> recentChats = chatRepository.findTop10ByOrderByCreatedAtDesc();
        // Return only the requested number
        return recentChats.subList(0, Math.min(limit, recentChats.size()));
    }
}
