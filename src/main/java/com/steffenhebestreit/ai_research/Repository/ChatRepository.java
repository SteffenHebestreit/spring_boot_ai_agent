package com.steffenhebestreit.ai_research.Repository;

import com.steffenhebestreit.ai_research.Model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
    
    // Find all chats ordered by most recent update
    List<Chat> findAllByOrderByUpdatedAtDesc();
}
