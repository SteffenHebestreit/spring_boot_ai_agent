package com.steffenhebestreit.ai_research.Service;

import java.io.IOException;

/**
 * Interface for components that need to listen for task updates
 */
public interface TaskUpdateListener {
    
    /**
     * Called when a task has an update to be sent to clients
     * 
     * @param taskId The ID of the task being updated
     * @param update The data object containing the update
     * @param eventType The type of the event (e.g., "task_status_update", "message_update")
     * @throws IOException If there's an error sending the update
     */
    void sendTaskUpdate(String taskId, Object update, String eventType) throws IOException;
}
