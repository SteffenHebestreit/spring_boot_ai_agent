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
    }/**
     * Add a message to an existing chat
     */    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Chat addMessageToChat(String chatId, Message message) {
        // Add enhanced logging for debugging message issues
        System.out.println("Adding message to chat " + chatId + " with role: " + message.getRole() + 
                           ", content type: " + message.getContentType() +
                           ", content preview: " + (message.getContent() instanceof String ? 
                               ((String)message.getContent()).substring(0, Math.min(50, ((String)message.getContent()).length())) + "..." 
                               : "Non-string content"));
        
        return getChatById(chatId).map(chat -> {
            // Enhanced duplicate message detection with better logging
            boolean isDuplicate = false;
            String duplicateReason = "";
            
            for (ChatMessage existingMsg : chat.getMessages()) {
                // Compare role and content to identify duplicates
                boolean sameRole = existingMsg.getRole().equals(message.getRole());
                boolean sameContent = false;
                
                if (existingMsg.getContent() instanceof String && message.getContent() instanceof String) {
                    String existingContent = (String) existingMsg.getContent();
                    String newContent = (String) message.getContent();
                    
                    // Check for exact match
                    if (existingContent.equals(newContent)) {
                        sameContent = true;
                    }
                    
                    // Check for substring match (for handling truncated responses)
                    // Only for agent/assistant responses which might be saved twice
                    if (!sameContent && ("agent".equals(message.getRole()) || "assistant".equals(message.getRole()))) {
                        if (existingContent.length() > 20 && newContent.length() > 20) {
                            if (existingContent.contains(newContent) || newContent.contains(existingContent)) {
                                sameContent = true;
                                duplicateReason = "content substring match";
                            }
                        }
                    }
                }
                
                if (sameRole && sameContent) {
                    isDuplicate = true;
                    duplicateReason = duplicateReason.isEmpty() ? "exact content match" : duplicateReason;
                    break;
                }
            }
            
            if (isDuplicate) {
                System.out.println("DUPLICATE MESSAGE DETECTED! Not saving duplicate message. Reason: " + duplicateReason);
                return chat; // Return existing chat without adding the duplicate
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
    }

    /**
     * Get all messages for a specific chat
     */
    public List<ChatMessage> getChatMessages(String chatId) {
        return chatMessageRepository.findByChatIdOrderByTimestampAsc(chatId);
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
        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByTimestampAsc(chatId);
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
}
