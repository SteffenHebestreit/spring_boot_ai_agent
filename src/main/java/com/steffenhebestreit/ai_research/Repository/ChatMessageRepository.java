package com.steffenhebestreit.ai_research.Repository;

import com.steffenhebestreit.ai_research.Model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository interface for ChatMessage entity data access operations.
 * 
 * <p>This repository interface extends JpaRepository to provide comprehensive data access
 * capabilities for ChatMessage entities, including CRUD operations, custom query methods,
 * and specialized retrieval patterns for chat message management.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>CRUD Operations:</strong> Complete create, read, update, delete support</li>
 * <li><strong>Custom Queries:</strong> Chat-specific message retrieval methods</li>
 * <li><strong>Ordering Support:</strong> Chronological message ordering capabilities</li>
 * <li><strong>Relationship Queries:</strong> Chat-based message filtering and retrieval</li>
 * </ul>
 * 
 * <h3>Query Methods:</h3>
 * <ul>
 * <li><strong>Chat-based Retrieval:</strong> Find all messages for specific chats</li>
 * <li><strong>Temporal Ordering:</strong> Messages ordered by creation timestamp</li>
 * <li><strong>Conversation Flow:</strong> Maintains chronological message sequences</li>
 * </ul>
 * 
 * <h3>Performance Considerations:</h3>
 * <ul>
 * <li>Indexes on chat_id and timestamp for efficient querying</li>
 * <li>Lazy loading for chat relationships to minimize data transfer</li>
 * <li>Ordered retrieval for proper conversation display</li>
 * </ul>
 * 
 * <h3>Usage Patterns:</h3>
 * <ul>
 * <li>Loading conversation history for chat displays</li>
 * <li>Message pagination and infinite scrolling support</li>
 * <li>Chronological message ordering for proper conversation flow</li>
 * <li>Chat-specific message management and cleanup</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see ChatMessage
 * @see Chat
 * @see ChatRepository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    
    /**     * Retrieves all messages for a specific chat ordered chronologically by creation time.
     * 
     * <p>Finds all ChatMessage entities associated with the specified chat ID and
     * returns them in ascending order by creation timestamp. This method provides
     * the complete conversation history in the correct chronological sequence
     * for display in user interfaces.</p>
     * 
     * <h3>Query Behavior:</h3>
     * <ul>
     * <li>Filters messages by exact chat ID match</li>
     * <li>Orders results by createdAt in ascending order (oldest first)</li>
     * <li>Returns empty list if no messages exist for the chat</li>
     * <li>Includes all message types (user, assistant, system)</li>
     * </ul>
     * 
     * <h3>Performance:</h3>
     * <ul>
     * <li>Leverages database indexes on chat_id and createdAt</li>
     * <li>Single query execution for complete conversation retrieval</li>
     * <li>Optimized for chat display and conversation reconstruction</li>
     * </ul>
     * 
     * <h3>Usage Examples:</h3>
     * <ul>
     * <li>Loading chat history for user interface display</li>
     * <li>Conversation export and archival operations</li>
     * <li>Message analysis and conversation processing</li>
     * <li>Chat completion and context building</li>
     * </ul>
     * 
     * @param chatId The unique identifier of the chat to retrieve messages for
     * @return List of ChatMessage entities ordered by createdAt (ascending)
     * @see ChatMessage#getCreatedAt()
     * @see Chat#getId()
     */
    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(String chatId);
}
