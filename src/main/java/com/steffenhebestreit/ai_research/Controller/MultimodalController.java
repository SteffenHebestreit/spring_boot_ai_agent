package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.MultimodalContentService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for handling multimodal content interactions with vision-enabled LLMs.
 * <p>
 * This controller provides endpoints for processing requests that combine text prompts with
 * visual content (images) or document content (PDFs). It leverages vision-enabled Large Language
 * Models to analyze and respond to multimodal inputs.
 * <p>
 * Key features include:
 * <ul> *   <li><b>File upload handling</b> - Supports images (JPG, PNG, etc.) and PDF documents</li>
 *   <li><b>LLM capability validation</b> - Ensures selected models support the uploaded content type</li>
 *   <li><b>Streaming and non-streaming responses</b> - Both real-time and complete response options</li>
 *   <li><b>Chat history integration</b> - Automatically saves conversations for context</li>
 *   <li><b>Error handling</b> - Comprehensive validation and error reporting, including file size limits</li>
 * </ul>
 * <p> * The controller works with several supporting services:
 * <ul>
 *   <li>{@link MultimodalContentService} - File validation and processing</li>
 *   <li>{@link OpenAIService} - LLM API communication</li>
 *   <li>{@link ChatService} - Conversation history management</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * POST /research-agent/api/chat-multimodal
 * Content-Type: multipart/form-data
 * 
 * --boundary
 * Content-Disposition: form-data; name="file"; filename="image.jpg"
 * Content-Type: image/jpeg
 * [binary image data]
 * 
 * --boundary
 * Content-Disposition: form-data; name="prompt"
 * What do you see in this image?
 * </pre>
 *  * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see MultimodalContentService
 * @see OpenAIService
 */
@RestController
@RequestMapping("/research-agent/api")
public class MultimodalController {

    @Autowired
    private OpenAIService openAIService;
    
    @Autowired
    private ChatService chatService;
      @Autowired
    private MultimodalContentService multimodalContentService;
    
    @Value("${openai.api.model}")
    private String defaultLlmId;/**
     * Processes multimodal content (text + file) and returns a complete AI response.
     * <p>
     * This endpoint handles requests containing both textual prompts and visual/document content,
     * processing them through vision-enabled LLMs to generate comprehensive responses. The entire
     * conversation is automatically saved to chat history for future reference.
     * <p>
     * <b>Process flow:</b>
     * <ol>
     *   <li>Validates the uploaded file against the selected LLM's capabilities</li>
     *   <li>Creates a new chat session</li>
     *   <li>Converts file to base64 data URI and structures multimodal content</li>
     *   <li>Sends the structured content to the vision-enabled LLM</li>
     *   <li>Returns the complete response and saves the conversation</li>
     * </ol>
     * <p>
    * <b>File requirements:</b>
     * <ul>
     *   <li>Images: JPG, PNG, GIF, WebP (max 10MB)</li>
     *   <li>Documents: PDF (max 20MB)</li>
     *   <li>LLM must support the specific file type</li>
     * </ul>
     * <p>
     * Exceeding these file size limits will result in a 400 Bad Request response with 
     * a descriptive error message. See {@link GlobalExceptionHandler} for details on 
     * exception handling.
     * 
     * @param prompt Optional text prompt to accompany the file analysis.
     *               If not provided, defaults to "Analyze this content."
     * @param file The image or PDF file to analyze (required).
     *             Must be compatible with the selected LLM.
     * @param llmId Optional ID of the specific LLM to use.
     *              If not provided, uses the default configured LLM.
     *              Must be a vision-enabled model for file processing.
     * @return ResponseEntity containing the AI's complete response as plain text,
     *         or error details if processing fails
     * @throws IllegalArgumentException if the file is incompatible with the selected LLM
     */
    @PostMapping(value = "/chat-multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> chatMultimodal(
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "llmId", required = false) String llmId) {
        
        // Use default LLM ID if not provided
        String modelId = (llmId != null && !llmId.isEmpty()) ? llmId : defaultLlmId;
        
        try {
            // Validate that the LLM can handle the file type
            Map<String, Object> validation = multimodalContentService.validateFile(file, modelId);
            if (!(boolean)validation.get("valid")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", validation.get("error"));
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Create multimodal content for the API request
            Object multimodalContent = multimodalContentService.createMultimodalContent(prompt, file, modelId);
            
            // Create a new chat with the user message containing both text and image
            Message userMsg = new Message();
            userMsg.setRole("user");
            userMsg.setContentType("multipart/mixed");
            userMsg.setContent(multimodalContent);
            Chat chat = chatService.createChat(userMsg);
            
            // Get AI response - this would need to be implemented in OpenAIService to handle multimodal content
            String aiResponse = openAIService.getMultimodalCompletion(multimodalContent, modelId);
            
            // Add the AI response to the chat history
            Message agentMsg = new Message("agent", "text/plain", aiResponse);
            chatService.addMessageToChat(chat.getId(), agentMsg);
            
            return ResponseEntity.ok(aiResponse);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing your request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
      /**
     * Processes multimodal content (text + file) and streams the AI response in real-time.
     * <p>
     * This endpoint provides the same functionality as {@link #chatMultimodal} but returns
     * the AI response as a stream of chunks, allowing for real-time display of the response
     * as it's being generated. This creates a more interactive user experience, especially
     * for longer responses.
     * <p>
     * <b>Process flow:</b>
     * <ol>
     *   <li>Validates the uploaded file against the selected LLM's capabilities</li>
     *   <li>Creates a new chat session and adds the user message</li>
     *   <li>Converts file to base64 data URI and structures multimodal content</li>
     *   <li>Initiates streaming request to the vision-enabled LLM</li>
     *   <li>Returns response chunks as they arrive</li>
     *   <li>Saves the complete conversation when streaming completes</li>
     * </ol>
     * <p>
    * <b>Response format:</b> Each chunk is delivered as a newline-delimited JSON (NDJSON) stream,
     * where each line contains a piece of the response text. The stream ends when the LLM
     * completes its response.
     * <p>
     * <b>File requirements:</b> Same as {@link #chatMultimodal}
     * <p>
     * Exceeding the file size limits will result in an error response in the stream with 
     * a descriptive error message. See {@link GlobalExceptionHandler} for details on 
     * exception handling.
     * 
     * @param prompt Optional text prompt to accompany the file analysis.
     *               If not provided, defaults to "Analyze this content."
     * @param file The image or PDF file to analyze (required).
     *             Must be compatible with the selected LLM.
     * @param llmId Optional ID of the specific LLM to use.
     *              If not provided, uses the default configured LLM.
     *              Must be a vision-enabled model for file processing.
     * @return A Flux stream of response chunks (NDJSON format), allowing real-time display
     *         of the AI's response as it's generated
     * @see #chatMultimodal
     */
    @PostMapping(value = "/chat-stream-multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStreamMultimodal(
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "llmId", required = false) String llmId) {
        
        // Use default LLM ID if not provided
        String modelId = (llmId != null && !llmId.isEmpty()) ? llmId : defaultLlmId;
        
        try {
            // Validate that the LLM can handle the file type
            Map<String, Object> validation = multimodalContentService.validateFile(file, modelId);
            if (!(boolean)validation.get("valid")) {
                return Flux.just("Error: " + validation.get("error"));
            }
            
            // Create multimodal content for the API request
            Object multimodalContent = multimodalContentService.createMultimodalContent(prompt, file, modelId);
            
            // Create a new chat with the user message containing both text and image
            Message userMsg = new Message();
            userMsg.setRole("user");
            userMsg.setContentType("multipart/mixed");
            userMsg.setContent(multimodalContent);
            Chat chat = chatService.createChat(userMsg);
            
            // StringBuilder to accumulate the response
            StringBuilder responseAggregator = new StringBuilder();
            
            // Get and return the streaming response - this would need to be implemented in OpenAIService
            return openAIService.getMultimodalCompletionStream(multimodalContent, modelId)
                    .doOnNext(responseAggregator::append) // Append each chunk to the aggregator
                    .doOnComplete(() -> {
                        // Save the completed response to chat history
                        try {
                            String fullResponse = responseAggregator.toString();
                            if (chat != null && !fullResponse.isEmpty()) {
                                Message agentMsg = new Message("agent", "text/plain", fullResponse);
                                chatService.addMessageToChat(chat.getId(), agentMsg);
                            }
                        } catch (Exception e) {
                            System.err.println("Error saving streamed response to chat history: " + e.getMessage());
                            e.printStackTrace();
                        }
                    })
                    .onErrorResume(e -> {
                        System.err.println("Error during multimodal chat stream: " + e.getMessage());
                        e.printStackTrace();
                        return Flux.just("Error processing your request: " + e.getMessage());
                    });
            
        } catch (IllegalArgumentException e) {
            return Flux.just("Error: " + e.getMessage());
        } catch (IOException e) {
            return Flux.just("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            return Flux.just("Error processing your request: " + e.getMessage());
        }
    }
}
