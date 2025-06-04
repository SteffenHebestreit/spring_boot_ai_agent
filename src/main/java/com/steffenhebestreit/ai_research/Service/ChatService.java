package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Repository.ChatMessageRepository;
import com.steffenhebestreit.ai_research.Repository.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    // OpenAIService is no longer needed after removing summarization
    
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
    }

    /**
     * Create a new chat with an initial message
     */
    @Transactional
    public Chat createChat(Message initialMessage) {
        Chat chat = new Chat();
        ChatMessage chatMessage = ChatMessage.fromMessage(initialMessage);
        
        chat.addMessage(chatMessage);
        chat.generateTitleFromContent();
        
        return chatRepository.save(chat);
    }

    /**
     * Add a message to an existing chat
     */    @Transactional
    public Chat addMessageToChat(String chatId, Message message) {
        return getChatById(chatId).map(chat -> {
            ChatMessage chatMessage = ChatMessage.fromMessage(message);            // Message summarization has been removed
            
            chat.addMessage(chatMessage);
            
            // If this is the first user message, generate a title
            if (chat.getTitle() == null || chat.getTitle().isEmpty()) {
                chat.generateTitleFromContent();
            }
            
            chat.setUpdatedAt(Instant.now());
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
}
