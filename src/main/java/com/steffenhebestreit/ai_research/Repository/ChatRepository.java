package com.steffenhebestreit.ai_research.Repository;

import com.steffenhebestreit.ai_research.Model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository interface for Chat entity data access operations.
 * 
 * <p>This repository interface extends JpaRepository to provide comprehensive data access
 * capabilities for Chat entities, including CRUD operations, custom query methods,
 * and specialized retrieval patterns for chat session management.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>CRUD Operations:</strong> Complete create, read, update, delete support</li>
 * <li><strong>Custom Queries:</strong> Temporal-based chat retrieval methods</li>
 * <li><strong>Ordering Support:</strong> Recent activity-based chat ordering</li>
 * <li><strong>Session Management:</strong> Chat lifecycle and state management</li>
 * </ul>
 * 
 * <h3>Query Methods:</h3>
 * <ul>
 * <li><strong>Activity-based Ordering:</strong> Chats ordered by most recent activity</li>
 * <li><strong>Temporal Retrieval:</strong> Chats sorted by update timestamps</li>
 * <li><strong>Session Discovery:</strong> Find and manage active chat sessions</li>
 * </ul>
 * 
 * <h3>Performance Considerations:</h3>
 * <ul>
 * <li>Indexes on updated_at for efficient ordering</li>
 * <li>Lazy loading for message collections to optimize retrieval</li>
 * <li>Efficient sorting for chat list displays</li>
 * </ul>
 * 
 * <h3>Usage Patterns:</h3>
 * <ul>
 * <li>Chat list displays with recent activity prioritization</li>
 * <li>Session management and lifecycle tracking</li>
 * <li>Chat discovery and selection interfaces</li>
 * <li>Activity monitoring and user engagement tracking</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see Chat
 * @see ChatMessage
 * @see ChatMessageRepository
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
    
    /**
     * Retrieves all chats ordered by most recent update timestamp.
     * 
     * <p>Finds all Chat entities and returns them in descending order by their
     * last update timestamp. This method provides a chronologically ordered
     * list of chats with the most recently active chats appearing first,
     * ideal for chat list displays and user interfaces.</p>
     * 
     * <h3>Query Behavior:</h3>
     * <ul>
     * <li>Retrieves all Chat entities from the database</li>
     * <li>Orders results by updated_at in descending order (newest first)</li>
     * <li>Returns empty list if no chats exist</li>
     * <li>Includes all chat states and types</li>
     * </ul>
     * 
     * <h3>Performance:</h3>
     * <ul>
     * <li>Leverages database index on updated_at column</li>
     * <li>Single query execution for complete chat list retrieval</li>
     * <li>Optimized for chat list displays and recent activity views</li>
     * <li>Lazy loading of message collections to minimize data transfer</li>
     * </ul>
     * 
     * <h3>Usage Examples:</h3>
     * <ul>
     * <li>Main chat list displays in user interfaces</li>
     * <li>Recent activity dashboards and summaries</li>
     * <li>Chat session management and monitoring</li>
     * <li>User engagement analytics and reporting</li>
     * </ul>
     * 
     * <h3>UI Integration:</h3>
     * <p>This method is particularly useful for frontend applications that need
     * to display a list of chats with the most recent conversations at the top,
     * providing users with quick access to their latest interactions.</p>
     * 
     * @return List of Chat entities ordered by update timestamp (descending)
     * @see Chat#getUpdatedAt()
     * @see Chat#getLastMessage()
     */
    List<Chat> findAllByOrderByUpdatedAtDesc();
}
