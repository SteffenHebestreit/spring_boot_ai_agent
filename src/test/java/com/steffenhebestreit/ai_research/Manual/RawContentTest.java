package com.steffenhebestreit.ai_research.Manual;

import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Util.ContentFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

/**
 * Manual test utility to verify raw content storage functionality.
 * 
 * <p>This test component is designed for manual execution to validate
 * that the raw content storage feature works correctly. It creates a test
 * chat with filtered content while preserving the original unfiltered
 * content for transparency and debugging purposes.</p>
 * 
 * <h3>Test Workflow:</h3>
 * <ul>
 * <li>Creates a message with think tags that should be filtered</li>
 * <li>Stores both filtered and raw versions in the database</li>
 * <li>Verifies that both versions are correctly persisted</li>
 * <li>Validates the content filtering process</li>
 * </ul>
 * 
 * <h3>Manual Execution:</h3>
 * <p>To run this test, uncomment the @Component annotation and restart
 * the application. The test will execute automatically on startup and
 * output results to the console.</p>
 * 
 * <p><strong>Note:</strong> This is a manual test utility and should not
 * be included in automated test suites. It's designed for development
 * and debugging purposes.</p>
 * 
 * @author AI Research System
 * @version 1.0
 * @since 1.0
 * @see ContentFilterUtil
 * @see ChatService
 */
// @Component  // Uncomment to enable automatic execution on startup
public class RawContentTest implements CommandLineRunner {

    @Autowired
    private ChatService chatService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Testing Raw Content Storage ===");
        
        // Test message with think tags
        String originalContent = "<think>This is internal reasoning that should be filtered</think>This is the visible message that should be stored.";
        String filteredContent = ContentFilterUtil.filterForDatabase(originalContent);
        
        System.out.println("Original content: " + originalContent);
        System.out.println("Filtered content: " + filteredContent);
        System.out.println("Filtering occurred: " + !originalContent.equals(filteredContent));
        
        // Create a test message
        Message testMessage = new Message("user", "text/plain", filteredContent);
        
        try {
            // Create a new chat with the test message
            var chat = chatService.createChat(testMessage);
            System.out.println("Created chat with ID: " + chat.getId());
            
            // Get the message ID from the created chat
            if (!chat.getMessages().isEmpty()) {
                ChatMessage lastMessage = chat.getMessages().get(chat.getMessages().size() - 1);
                String messageId = lastMessage.getId();
                
                // Update with raw content
                chatService.updateMessageRawContent(messageId, originalContent);
                System.out.println("Updated message " + messageId + " with raw content");
                
                // Verify the raw content was saved
                var messages = chatService.getChatMessages(chat.getId());
                for (ChatMessage msg : messages) {
                    if (msg.getId().equals(messageId)) {
                        System.out.println("Message filtered content: " + msg.getContent());
                        System.out.println("Message raw content: " + msg.getRawContent());
                        System.out.println("Raw content properly saved: " + (msg.getRawContent() != null && msg.getRawContent().equals(originalContent)));
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== Raw Content Test Complete ===");
    }
}
