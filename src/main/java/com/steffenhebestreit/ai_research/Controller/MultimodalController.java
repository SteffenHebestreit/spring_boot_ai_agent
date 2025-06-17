package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Model.Chat;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.Message;
import com.steffenhebestreit.ai_research.Service.ChatService;
import com.steffenhebestreit.ai_research.Service.MultimodalContentService;
import com.steffenhebestreit.ai_research.Service.OpenAIService;
import com.steffenhebestreit.ai_research.Util.ContentFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            @RequestParam(value = "llmId", required = false) String llmId,
            @RequestParam(value = "chatId", required = false) String chatId) {
        
        // Use default LLM ID if not provided
        String modelId = (llmId != null && !llmId.isEmpty()) ? llmId : defaultLlmId;
        
        try {
            // Validate that the LLM can handle the file type
            Map<String, Object> validation = multimodalContentService.validateFile(file, modelId);
            if (!(boolean)validation.get("valid")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", validation.get("error"));                return ResponseEntity.badRequest().body(errorResponse);
            }
              // Create multimodal content for the API request
            Object multimodalContent = multimodalContentService.createMultimodalContent(prompt, file, modelId);
            
            // Create a new chat with the user message containing ORIGINAL multimodal content for frontend display
            Message userMsg = new Message();
            userMsg.setRole("user");
            userMsg.setContentType("multipart/mixed");
            userMsg.setContent(openAIService.convertMultimodalContentToString(multimodalContent)); // Store original content for frontend
            
            // Determine which chat to use
            Chat chat;
            if (chatId != null && !chatId.isEmpty()) {
                // Try to use the specified chat and update if found, otherwise create new
                Chat existingChat = chatService.getChatById(chatId).orElse(null);
                if (existingChat != null) {
                    chatService.addMessageToChat(existingChat.getId(), userMsg);
                    chat = existingChat;
                } else {
                    chat = chatService.createChat(userMsg);
                }
            } else {
                // Create a new chat
                chat = chatService.createChat(userMsg);
            }
            
            // Get AI response using the FULL multimodal content (with images) for processing
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
     */    @PostMapping(value = "/chat-stream-multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStreamMultimodal(
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "llmId", required = false) String llmId,
            @RequestParam(value = "chatId", required = false) String chatId) {
        
        // Use default LLM ID if not provided
        String modelId = (llmId != null && !llmId.isEmpty()) ? llmId : defaultLlmId;
        
        try {
            // Validate that the LLM can handle the file type
            Map<String, Object> validation = multimodalContentService.validateFile(file, modelId);
            if (!(boolean)validation.get("valid")) {
                return Flux.just("Error: " + validation.get("error"));
            }
            
            // Check if model is still loading and add a note to the stream
            boolean modelLoading = validation.containsKey("modelLoading") && (boolean)validation.get("modelLoading");
            if (modelLoading) {
                // Format the initialization message with italics and add linebreaks
                String message = "*" + validation.get("message") + "*\n\n";
                
                // Only return the initialization message, do not proceed with the request
                return Flux.just(message);
            }              // Create multimodal content for the API request
            Object multimodalContent = multimodalContentService.createMultimodalContent(prompt, file, modelId);
              
            // Create a new chat with the user message containing ORIGINAL multimodal content for frontend display
            Message userMsg = new Message();
            userMsg.setRole("user");
            userMsg.setContentType("multipart/mixed");
            userMsg.setContent(openAIService.convertMultimodalContentToString(multimodalContent)); // Store original content for frontend            // Determine which chat to use
            final Chat chat;
            List<ChatMessage> existingMessages = new ArrayList<>();
            if (chatId != null && !chatId.isEmpty()) {
                // Try to use the specified chat and update if found, otherwise create new
                Chat existingChat = chatService.getChatById(chatId).orElse(null);
                if (existingChat != null) {
                    // Get existing messages BEFORE adding the current message to avoid filtering issues
                    existingMessages = new ArrayList<>(existingChat.getMessages());
                    chatService.addMessageToChat(existingChat.getId(), userMsg);
                    chat = existingChat;
                } else {
                    chat = chatService.createChat(userMsg);
                }
            } else {
                // Create a new chat
                chat = chatService.createChat(userMsg);
            }
            
            // StringBuilder to accumulate the response
            final StringBuilder responseAggregator = new StringBuilder();              // If we have existing conversation history, use the conversation stream with history
            Flux<String> responseStream;
            if (!existingMessages.isEmpty()) {
                // Use existing ChatMessage objects directly (they already contain stored content)
                List<ChatMessage> conversationHistory = new ArrayList<>(existingMessages);
                
                // Add current user message with full multimodal content for current processing
                ChatMessage currentUserMessage = new ChatMessage();
                currentUserMessage.setRole("user");
                currentUserMessage.setContent(openAIService.convertMultimodalContentToString(multimodalContent));
                currentUserMessage.setContentType("multipart/mixed");
                conversationHistory.add(currentUserMessage);
                
                // Use conversation stream with history (history will be automatically stripped by prepareMessagesForLlm)
                responseStream = openAIService.getChatCompletionStream(conversationHistory, modelId);
            } else {
                // No history, use direct multimodal stream
                responseStream = openAIService.getMultimodalCompletionStream(multimodalContent, modelId);
            }
            
            // Get and return the streaming response
            return responseStream
                .doOnNext(chunk -> {
                    // Safely append chunks, checking for null
                    if (chunk != null) {
                        responseAggregator.append(chunk);
                    }
                })
                .doOnComplete(() -> {
                    // Save the completed response to chat history
                    try {
                        String fullResponse = responseAggregator.toString();
                        if (chat != null && !fullResponse.isEmpty()) {
                            // Filter out <think> tags and ensure content fits within database constraints
                            String filteredResponse = ContentFilterUtil.filterForDatabase(fullResponse);
                            Message agentMsg = new Message("agent", "text/plain", filteredResponse);
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
    
    /**
     * Creates a new chat with an initial multimodal message.
     * <p>
     * This endpoint is specifically designed to create a chat session with multimodal content 
     * (text + file) as the first message, preventing the need for separate chat creation and 
     * multimodal message addition calls that can cause duplicate messages.
     * <p>
     * <b>Process flow:</b>
     * <ol>
     *   <li>Validates the uploaded file against the selected LLM's capabilities</li>
     *   <li>Creates multimodal content structure</li>
     *   <li>Creates a new chat with the multimodal message as the initial message</li>
     *   <li>Returns the chat ID and initial message structure</li>
     * </ol>
     * <p>
     * <b>File requirements:</b> Same as other multimodal endpoints
     * 
     * @param prompt Optional text prompt to accompany the file analysis.
     *               If not provided, the image will be analyzed without specific instructions.
     * @param file The image or PDF file to include in the initial message (required).
     *             Must be compatible with the selected LLM.
     * @param llmId Optional ID of the specific LLM to use.
     *              If not provided, uses the default configured LLM.
     * @return ResponseEntity containing the chat ID and initial message structure,
     *         or error details if creation fails
     */
    @PostMapping(value = "/create-multimodal-chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMultimodalChat(
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
            
            // Create multimodal content for storage
            Object multimodalContent = multimodalContentService.createMultimodalContent(prompt, file, modelId);
            
            // Create the initial user message with ORIGINAL multimodal content for frontend display
            Message userMsg = new Message();
            userMsg.setRole("user");
            userMsg.setContentType("multipart/mixed");
            userMsg.setContent(openAIService.convertMultimodalContentToString(multimodalContent));
            
            // Create a new chat with the multimodal message as the initial message
            Chat chat = chatService.createChat(userMsg);
            
            // Prepare response with chat details
            Map<String, Object> response = new HashMap<>();
            response.put("chatId", chat.getId());
            response.put("message", "Multimodal chat created successfully");
            response.put("initialMessage", Map.of(
                "role", userMsg.getRole(),
                "contentType", userMsg.getContentType(),
                "content", userMsg.getContent()
            ));
            
            return ResponseEntity.ok(response);
            
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
            errorResponse.put("error", "Error creating multimodal chat: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Creates a new multimodal chat and streams the AI response in real-time.
     * <p>
     * This endpoint creates a new chat session with a multimodal message (text + file) 
     * and immediately streams the AI response back to the client. This eliminates the 
     * need for separate chat creation and multimodal processing calls, preventing 
     * duplicate messages and providing a seamless user experience.
     * <p>
     * <b>Process flow:</b>
     * <ol>
     *   <li>Validates the uploaded file against the selected LLM's capabilities</li>
     *   <li>Creates a new chat session with the multimodal message</li>
     *   <li>Converts file to base64 data URI and structures multimodal content</li>
     *   <li>Initiates streaming request to the vision-enabled LLM</li>
     *   <li>Returns response chunks as they arrive with chat ID in headers</li>
     *   <li>Saves the complete conversation when streaming completes</li>
     * </ol>
     * <p>
     * <b>Response format:</b> 
     * <ul>
     *   <li><b>Headers:</b> X-Chat-Id contains the newly created chat ID</li>
     *   <li><b>Body:</b> NDJSON stream of AI response chunks</li>
     * </ul>
     * <p>
     * <b>File requirements:</b> Same as other multimodal endpoints
     * 
     * @param prompt Optional text prompt to accompany the file analysis.
     *               If not provided, defaults to "Analyze this content."
     * @param file The image or PDF file to analyze (required).
     *             Must be compatible with the selected LLM.
     * @param llmId Optional ID of the specific LLM to use.
     *              If not provided, uses the default configured LLM.
     * @return A Flux stream of response chunks (NDJSON format) with chat ID in headers
     * @see #chatStreamMultimodal
     * @see #createMultimodalChat
     */
    @PostMapping(value = "/create-stream-multimodal-chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<String>> createStreamMultimodalChat(
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "llmId", required = false) String llmId) {
        
        // Use default LLM ID if not provided
        String modelId = (llmId != null && !llmId.isEmpty()) ? llmId : defaultLlmId;
        
        try {
            // Validate that the LLM can handle the file type
            Map<String, Object> validation = multimodalContentService.validateFile(file, modelId);
            if (!(boolean)validation.get("valid")) {
                Flux<String> errorFlux = Flux.just("Error: " + validation.get("error"));
                return ResponseEntity.badRequest().body(errorFlux);
            }
            
            // Check if model is still loading and add a note to the stream
            boolean modelLoading = validation.containsKey("modelLoading") && (boolean)validation.get("modelLoading");
            if (modelLoading) {
                // Format the initialization message with italics and add linebreaks
                String message = "*" + validation.get("message") + "*\n\n";
                Flux<String> initFlux = Flux.just(message);
                return ResponseEntity.ok().body(initFlux);
            }
            
            // Create multimodal content for the API request
            Object multimodalContent = multimodalContentService.createMultimodalContent(prompt, file, modelId);
            
            // Create a new chat with the user message containing ORIGINAL multimodal content for frontend display
            Message userMsg = new Message();
            userMsg.setRole("user");
            userMsg.setContentType("multipart/mixed");
            userMsg.setContent(openAIService.convertMultimodalContentToString(multimodalContent)); // Store original content for frontend
            
            // Create new chat (no existing chat ID since this is creation)
            final Chat chat = chatService.createChat(userMsg);
            
            // StringBuilder to accumulate the response
            final StringBuilder responseAggregator = new StringBuilder();
            
            // Get streaming response from LLM
            Flux<String> responseStream = openAIService.getMultimodalCompletionStream(multimodalContent, modelId);
            
            // Process the streaming response
            Flux<String> processedStream = responseStream
                .doOnNext(chunk -> {
                    // Safely append chunks, checking for null
                    if (chunk != null) {
                        responseAggregator.append(chunk);
                    }
                })
                .doOnComplete(() -> {
                    // Save the completed response to chat history
                    try {
                        String fullResponse = responseAggregator.toString();
                        if (chat != null && !fullResponse.isEmpty()) {
                            // Filter out <think> tags and ensure content fits within database constraints
                            String filteredResponse = ContentFilterUtil.filterForDatabase(fullResponse);
                            Message agentMsg = new Message("agent", "text/plain", filteredResponse);
                            chatService.addMessageToChat(chat.getId(), agentMsg);
                        }
                    } catch (Exception e) {
                        System.err.println("Error saving streamed response to chat history: " + e.getMessage());
                        e.printStackTrace();
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Error during multimodal chat creation stream: " + e.getMessage());
                    e.printStackTrace();
                    return Flux.just("Error processing your request: " + e.getMessage());
                });
            
            // Return response with chat ID in headers
            return ResponseEntity.ok()
                .header("X-Chat-Id", chat.getId())
                .header("Access-Control-Expose-Headers", "X-Chat-Id")
                .body(processedStream);
            
        } catch (IllegalArgumentException e) {
            Flux<String> errorFlux = Flux.just("Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorFlux);
        } catch (IOException e) {
            Flux<String> errorFlux = Flux.just("Error processing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorFlux);
        } catch (Exception e) {
            Flux<String> errorFlux = Flux.just("Error processing your request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorFlux);
        }
    }

    // ...existing code...
}
