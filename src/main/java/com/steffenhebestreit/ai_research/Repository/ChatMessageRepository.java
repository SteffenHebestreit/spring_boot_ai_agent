package com.steffenhebestreit.ai_research.Repository;

import com.steffenhebestreit.ai_research.Model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    
    // Find all messages for a specific chat ordered by timestamp
    List<ChatMessage> findByChatIdOrderByTimestampAsc(String chatId);
}
