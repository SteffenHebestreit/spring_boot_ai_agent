package com.steffenhebestreit.ai_research.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.ChatMessage;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import com.steffenhebestreit.ai_research.Model.LlmConfigurationAdapter;
import com.steffenhebestreit.ai_research.Model.ProviderModel;
import com.steffenhebestreit.ai_research.Model.ProviderModelAdapter;
import com.steffenhebestreit.ai_research.Model.ToolSelectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Service for interacting with OpenAI-compatible LLM APIs, including multimodal content processing.
 * <p>
 * This service provides comprehensive functionality for communicating with Language Learning Models (LLMs)
 * that follow the OpenAI API specification. It supports both traditional text-based interactions and
 * advanced multimodal content processing that includes images and PDF documents.
 * <p>
 * Key capabilities include:
 * <ul>
 *   <li><b>Text-only chat completions</b> - Traditional text-based LLM interactions</li>
 *   <li><b>Streaming responses</b> - Real-time response streaming for better user experience</li>
 *   <li><b>Multimodal processing</b> - Handle images and PDFs with vision-enabled models</li>
 *   <li><b>Message summarization</b> - Automatic content summarization for context management</li>
 *   <li><b>Conversation history</b> - Support for multi-turn conversations with context</li>
 *   <li><b>Model Capability Detection</b> - Dynamically determines model features</li>
 * </ul>
 * <p>
 * The service automatically handles:
 * <ul>
 *   <li>Authentication using API keys</li>
 *   <li>Request/response format conversion</li>
 *   <li>Error handling and logging</li>
 *   <li>Stream parsing for real-time responses</li>
 *   <li>Role mapping (internal 'agent' role to API 'assistant' role)</li>
 * </ul>
 * <p>
 * <b>Multimodal Support:</b>
 * The service includes specialized methods for processing content that combines text with images
 * or PDF documents. This requires vision-enabled LLM models (e.g., GPT-4V, GPT-4o) and properly
 * formatted content with base64-encoded file data.
 * <p>
 * <b>Model Capability Detection:</b>
 * The service fetches available models from the configured endpoint and attempts to determine
 * their capabilities. It first checks for an `LlmConfiguration` provided by `LlmCapabilityService`.
 * If found, those settings are used. Otherwise, a basic fallback is applied, enabling text generation
 * and tool use by default, with conservative token limits.
 * 
 * @author Steffen Hebestreit
 * @version 1.1
 * @since 1.0
 * @see LlmCapabilityService
 * @see MultimodalContentService
 */
@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private final OpenAIProperties openAIProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper; // Retained initialization in constructor
    private final DynamicIntegrationService dynamicIntegrationService;
    private final LlmCapabilityService llmCapabilityService;

    public OpenAIService(OpenAIProperties openAIProperties, WebClient.Builder webClientBuilder, 
                         ObjectMapper objectMapper, DynamicIntegrationService dynamicIntegrationService,
                         LlmCapabilityService llmCapabilityService) {
        this.openAIProperties = openAIProperties;
        this.dynamicIntegrationService = dynamicIntegrationService;
        this.llmCapabilityService = llmCapabilityService;
        String baseUrl = openAIProperties.getBaseurl();
        if (baseUrl == null) {
            baseUrl = "http://localhost:1234"; 
            logger.warn("Base URL is null, using default fallback: {}", baseUrl);
        } else if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper; // Ensure it's assigned
    }

    @PostConstruct
    public void init() {
        logger.debug("=== OpenAIService Initialization ===");
        logger.debug("System Role value: '{}'", openAIProperties.getSystemRole());
        logger.debug("System Role length: {}", openAIProperties.getSystemRole() != null ? openAIProperties.getSystemRole().length() : "null");
        logger.debug("Base URL: {}", openAIProperties.getBaseurl());
        logger.debug("Model: {}", openAIProperties.getModel());
        logger.debug("=====================================");
    }
    
    // Helper class for assembling tool calls from stream chunks
    static class StreamingToolCall {
        String id;
        String type = "function"; // Default type
        StringBuilder functionName = new StringBuilder();
        StringBuilder functionArguments = new StringBuilder();
        Integer index; // OpenAI ToolCall in stream has an index
        
        public StreamingToolCall(Integer index) {
            this.index = index;
        }
        
        public void setId(String id) {
            if (this.id == null && id != null) { 
                this.id = id;
            }
        }
        
        public void setType(String type) {
            if (type != null) {
                this.type = type;
            }
        }
        
        public void appendName(String namePart) {
            if (namePart != null) {
                this.functionName.append(namePart);
            }
        }
        
        public void appendArguments(String argsPart) {
            if (argsPart != null) {
                this.functionArguments.append(argsPart);
            }
        }
          public boolean isComplete() {
            // ID and function name are essential. Arguments can be an empty string.
            return id != null && !id.isEmpty() && functionName.length() > 0;
        }

        public Map<String, Object> build() {
            if (!isComplete()) {
                OpenAIService.logger.warn("Attempting to build incomplete tool call. Index: {}, ID: {}, Name: '{}', Args (preview): '{}'", 
                                           index, id, functionName.toString(), functionArguments.toString().substring(0, Math.min(50, functionArguments.length())));
                // Depending on requirements, might throw an exception or return null for critical incompleteness.
                // For now, proceeds to build with available data, consistent with previous logging behavior.
            }
            Map<String, Object> toolCallMap = new HashMap<>();
            toolCallMap.put("id", this.id);
            toolCallMap.put("type", this.type); // Typically "function"

            Map<String, String> functionDetailsMap = new HashMap<>();
            functionDetailsMap.put("name", functionName.toString());
            functionDetailsMap.put("arguments", functionArguments.toString());

            toolCallMap.put("function", functionDetailsMap);
            return toolCallMap;
        }
    }    // Helper methods for sink management
    private void tryCloseSinkWithCompletion(FluxSink<String> sink, AtomicBoolean sinkClosedFlag, String modelId, String reason, AtomicBoolean cancellationFlag) {
        if (cancellationFlag.get()) {
            if (sinkClosedFlag.compareAndSet(false, true)) {
                logger.info("Sink completion for modelId: {} ({}) aborted due to cancellation.", modelId, reason);
                try {
                    sink.error(new CancellationException("Stream cancelled before explicit completion: " + reason));
                } catch (UnsupportedOperationException ex) {
                    logger.warn("UnsupportedOperationException when trying to signal error on cancellation for modelId: {}. Sink may already be completed.", modelId);
                }
            }
            return;
        }
        if (sinkClosedFlag.compareAndSet(false, true)) {
            logger.info("Completing sink for modelId: {} due to: {}", modelId, reason);
            try {
                sink.complete();
            } catch (UnsupportedOperationException ex) {
                logger.warn("UnsupportedOperationException when trying to complete sink for modelId: {}. Sink may already be in terminal state.", modelId);
            }
        } else {
            logger.warn("Sink already closed for modelId: {}, completion attempt for reason '{}' ignored.", modelId, reason);
        }
    }

    private void tryCloseSinkWithError(FluxSink<String> sink, AtomicBoolean sinkClosedFlag, Throwable error, String modelId, AtomicBoolean cancellationFlag) {
        if (error instanceof CancellationException) {
            if (sinkClosedFlag.compareAndSet(false, true)) {
                logger.info("Sink closing for modelId: {} due to explicit CancellationException: {}", modelId, error.getMessage());
                try {
                    sink.error(error);
                } catch (UnsupportedOperationException ex) {
                    logger.warn("UnsupportedOperationException when trying to signal cancellation error for modelId: {}. Sink may already be in terminal state.", modelId);
                }
            } else {
                logger.info("Sink already closed for modelId: {}. CancellationException ({}) noted.", modelId, error.getMessage());
            }
            return;
        }
        if (cancellationFlag.get()) {
            if (sinkClosedFlag.compareAndSet(false, true)) {
                logger.info("Sink closing for modelId: {} due to error ({}) occurring during active cancellation.", modelId, error.getClass().getSimpleName());
                try {
                    sink.error(new CancellationException("Stream cancelled, original error: " + error.getMessage()));
                } catch (UnsupportedOperationException ex) {
                    logger.warn("UnsupportedOperationException when trying to signal error during cancellation for modelId: {}. Sink may already be in terminal state.", modelId);
                }
            } else {
                 logger.info("Sink already closed for modelId: {}. Error ({}) occurred during active cancellation, but sink was already handled.", modelId, error.getClass().getSimpleName());
            }
            return;
        }

        if (sinkClosedFlag.compareAndSet(false, true)) {
            logger.error("Erroring sink for modelId: {}", modelId, error);
            try {
                sink.error(error);
            } catch (UnsupportedOperationException ex) {
                logger.warn("UnsupportedOperationException when trying to signal error for modelId: {}. Sink may already be in terminal state.", modelId);
            }
        } else {
            logger.warn("Sink already closed for modelId: {}, error attempt ignored. Original error for modelId {}:", modelId, modelId, error);        }
    }
      // Prepares messages for LLM by converting ChatMessage objects to the format expected by OpenAI API
    private List<Map<String, Object>> prepareMessagesForLlm(List<ChatMessage> messages) {
        // The last message is typically the current/new message that should keep full multimodal content
        // All previous messages are history and should use token-efficient content
        final int lastMessageIndex = messages.size() - 1;
        
        return messages.stream()
                .map(messages::indexOf) // Get index of each message
                .map(index -> {
                    ChatMessage msg = messages.get(index);
                    boolean isCurrentMessage = (index == lastMessageIndex);
                    
                    Map<String, Object> llmMessage = new HashMap<>();
                    String role = msg.getRole();
                    
                    // Map "agent" role to "assistant" for compatibility with LLM API
                    if ("agent".equals(role)) {
                        role = "assistant";
                        logger.debug("Mapped 'agent' role to 'assistant' for LLM API compatibility");
                    }
                    
                    llmMessage.put("role", role);                    // Content is nullable, especially for assistant messages with tool_calls
                    if (msg.getContent() != null) {                        // Handle multimodal content specifically
                        if ("multipart/mixed".equals(msg.getContentType())) {
                            try {
                                // Try to parse the content as JSON for multimodal content
                                Object parsedContent = objectMapper.readValue(msg.getContent(), Object.class);
                                
                                // Check if this looks like a history message with stripped content
                                if (isHistoryFriendlyContent(parsedContent)) {
                                    // This is already a history message with stripped images - keep as-is
                                    llmMessage.put("content", parsedContent);
                                    logger.debug("Using pre-existing history-friendly multimodal content for role: {}", role);
                                } else {
                                    // This is a message with full multimodal content
                                    if (isCurrentMessage) {
                                        // Current/new message - keep full multimodal content with images
                                        llmMessage.put("content", parsedContent);
                                        logger.debug("Using full multimodal content for current message with role: {}", role);
                                    } else {
                                        // Historical message - convert to history-friendly for token efficiency
                                        Object historyFriendlyContent = createHistoryFriendlyMultimodalContent(parsedContent);
                                        llmMessage.put("content", historyFriendlyContent);
                                        logger.debug("Converted full multimodal content to history-friendly format for historical message with role: {}", role);
                                    }
                                }
                            } catch (Exception e) {
                                // If parsing fails, check if this might be a simple text string from history
                                String content = msg.getContent();
                                if (content.contains("[Image content omitted") || content.contains("[Multimodal content")) {
                                    // This looks like a stripped text representation
                                    llmMessage.put("content", content);
                                    logger.debug("Using text representation of multimodal content for history message");
                                } else {
                                    // If parsing fails, fall back to using the string directly
                                    logger.warn("Failed to parse multimodal content JSON, using raw string: {}", e.getMessage());
                                    llmMessage.put("content", content);
                                }
                            }
                        } else {
                            // Regular text content
                            llmMessage.put("content", msg.getContent());
                        }
                    } else {
                        // If content is null, explicitly put null, unless it's an assistant message
                        // that will have tool_calls, in which case content might be omitted by OpenAI spec.
                        // For now, always include it if null, unless tool_calls are present.                        llmMessage.put("content", null);
                    }
                    
                    if ("assistant".equals(role)) {
                        // Handle tool calls for assistant messages
                        if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                            llmMessage.put("tool_calls", msg.getToolCalls());
                            // OpenAI API: "content is required if tool_calls is not specified".
                            // "If tool_calls is specified, content is optional."
                            // Remove content key if null when tool_calls are present
                            if (msg.getContent() == null) {
                                llmMessage.remove("content");
                            }
                        }
                        // The 'name' field from ChatMessage is not used for 'assistant' role messages.
                    } else if ("tool".equals(role)) {
                        llmMessage.put("tool_call_id", msg.getToolCallId());
                        // The 'name' field in ChatMessage for 'tool' role corresponds to the function name.
                        llmMessage.put("name", msg.getName());
                        // Content for a 'tool' message is the result of the tool execution (JSON string).
                        // It's already handled by msg.getContent() above.
                    } else if ("user".equals(role)) {
                        // For user messages, 'name' can optionally be used to specify the user's name.
                        if (msg.getName() != null && !msg.getName().isEmpty()) {
                            llmMessage.put("name", msg.getName());
                        }
                    }
                    // 'system' role messages typically only have 'role' and 'content'.

                    return llmMessage;
                })
                .collect(Collectors.toList());
    }

    public Flux<String> getChatCompletionStream(List<ChatMessage> conversationMessages) {
        return getChatCompletionStream(conversationMessages, openAIProperties.getModel());
    }
    
    public Flux<String> getChatCompletionStream(List<ChatMessage> conversationMessages, String modelId) {
        List<Map<String, Object>> messagesForLlm = prepareMessagesForLlm(conversationMessages); // Uses updated method

        if (messagesForLlm.isEmpty() || (messagesForLlm.size() == 1 && "system".equals(messagesForLlm.get(0).get("role")) && (conversationMessages == null || conversationMessages.isEmpty()))) {
            logger.warn("Conversation messages list is effectively empty. Cannot make request to LLM.");
            return Flux.error(new IllegalArgumentException("Cannot send an effectively empty message list to LLM."));
        }        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", messagesForLlm); 
        requestBody.put("stream", true);
        
        // Check if we have multimodal content in the conversation
        boolean hasMultimodalContent = conversationMessages.stream()
            .anyMatch(msg -> "multipart/mixed".equals(msg.getContentType()));
        
        // Add discovered MCP tools to request when available, but exclude for multimodal content
        if (!hasMultimodalContent) {
            List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
            if (mcpTools != null && !mcpTools.isEmpty()) {
                requestBody.put("tools", convertMcpToolsToOpenAIFormat(mcpTools));
                logger.debug("Added {} tools to basic streaming request for modelId: {}", mcpTools.size(), modelId);
            }
        } else {
            logger.info("Multimodal content detected in basic stream. Excluding tools to let LLM focus on native vision capabilities for modelId: {}", modelId);
        }

        String path = "/chat/completions";
        logger.info("Sending basic streaming request to LLM: {} with model: {} and {} messages.", openAIProperties.getBaseurl() + path, modelId, messagesForLlm.size());        return webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractContentFromStreamResponse)
                .filter(content -> content != null && !content.isEmpty())
                .doOnError(error -> logger.error("Error during basic LLM stream processing for modelId: {}", modelId, error))
                .doOnComplete(() -> logger.info("Basic LLM stream completed for modelId: {}", modelId));
    }
    
    /**
     * Gets a completion from a vision-enabled LLM using multimodal content (text + image/PDF).
     * <p>
     * This method processes multimodal content that contains both text and file data (images or PDFs)
     * using vision-enabled LLM models. The content is sent to the specified LLM model and a response
     * is generated synchronously.
     * <p>
     * The multimodal content should be structured as expected by the OpenAI API format, typically
     * containing a text prompt and base64-encoded file data with appropriate metadata.
     * <p>
     * <b>Note:</b> This method blocks until the LLM response is complete. For streaming responses,
     * use {@link #getMultimodalCompletionStream(Object, String)} instead.
     * 
     * @param multimodalContent The structured content object containing text and file data,
     *                         formatted according to OpenAI API specifications
     * @param llmId The ID of the vision-enabled LLM model to use for processing
     * @return The LLM's response as text, or an error message if processing fails
     * @throws IllegalArgumentException if multimodalContent or llmId is null
     * @see #getMultimodalCompletionStream(Object, String)
     */    public String getMultimodalCompletion(Object multimodalContent, String llmId) {
        logger.info("Processing multimodal content with model: {}", llmId);
          // Prepare the request body for the OpenAI API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmId);
        
        // Convert the multimodal content to the format expected by the API
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Add system message only if model supports multimodal content and content is actually multimodal
        Map<String, Object> systemMessage = createMultimodalSystemMessage(llmId, multimodalContent);
        if (systemMessage != null) {
            messages.add(systemMessage);
            logger.debug("Added multimodal system message for model: {}", llmId);
        }
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", multimodalContent);
        messages.add(userMessage);
        requestBody.put("messages", messages);
          // For safety, set a reasonable maximum token limit
        requestBody.put("max_tokens", 2000);
        
        // Don't add tools for multimodal content - let the LLM use its native vision capabilities
        // Adding tools confuses the LLM and makes it think it can't analyze images directly
        logger.debug("Multimodal content detected. Not adding tools to let LLM focus on native vision capabilities for modelId: {}", llmId);

        try {
            // Build the URL
            String url = "/chat/completions";
            
            // Make the API call using WebClient for better handling of complex objects
            String responseJson = webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse the response to extract the generated text
            JsonNode responseNode = objectMapper.readTree(responseJson);
            String generatedText = responseNode
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
            
            return generatedText;
        } catch (Exception e) {
            logger.error("Error getting multimodal completion", e);
            return "Error processing multimodal content: " + e.getMessage();
        }
    }
    
    /**
     * Gets a streaming completion from the LLM for multimodal content.
     * <p>
     * This method processes multimodal content (text + images/PDFs) and returns a reactive stream
     * of response chunks as they are generated by the LLM. This provides a better user experience
     * for long responses as content appears progressively rather than waiting for the complete response.
     * <p>
     * The streaming response is particularly useful for:
     * <ul>
     *   <li>Real-time user interfaces where responses appear as they're generated</li>
     *   <li>Long-form content analysis where the LLM response may be lengthy</li>
     *   <li>Interactive applications requiring immediate feedback</li>
     * </ul>
     * <p>
     * The multimodal content structure should match OpenAI API expectations, containing
     * text prompts and base64-encoded file data with appropriate MIME type metadata.
     * 
     * @param multimodalContent The structured content containing text and file data,
     *                         formatted for OpenAI API consumption
     * @param modelId The ID of the vision-enabled LLM model to use for processing
     * @return A Flux of response chunks as they are generated by the LLM.
     *         Each chunk contains a portion of the response text.
     * @throws IllegalArgumentException if multimodalContent or modelId is null
     * @see #getMultimodalCompletion(Object, String)
     */    public Flux<String> getMultimodalCompletionStream(Object multimodalContent, String modelId) {
        logger.info("Processing streaming multimodal content with model: {}", modelId);
        
        // Convert multimodal content to ChatMessage format for unified processing
        List<ChatMessage> messages = new ArrayList<>();
        
        // Add system message if appropriate
        Map<String, Object> systemMessage = createMultimodalSystemMessage(modelId, multimodalContent);
        if (systemMessage != null) {
            ChatMessage systemChatMsg = new ChatMessage();
            systemChatMsg.setRole("system");
            systemChatMsg.setContent((String) systemMessage.get("content"));
            messages.add(systemChatMsg);
            logger.debug("Added multimodal system message for streaming model: {}", modelId);
        }
        
        // Create user message with multimodal content
        // Convert multimodal content to a string representation for ChatMessage storage
        String multimodalContentString = convertMultimodalContentToString(multimodalContent);
        
        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent(multimodalContentString);
        userMessage.setContentType("multipart/mixed"); // Indicate this is multimodal content
        // Also store the original multimodal content object in rawContent for potential future use
        userMessage.setRawContent(multimodalContentString);
        messages.add(userMessage);
        
        logger.debug("Converted multimodal content to ChatMessage format for unified streaming. Content type: {}", userMessage.getContentType());
        
        // Use the same robust streaming framework as text-only streaming
        // This ensures proper tool call handling and stream management
        AtomicBoolean cancellationFlag = new AtomicBoolean(false);
        
        return Flux.create(sink -> {
            executeStreamingConversationWithToolsInternal(messages, modelId, sink, null, cancellationFlag);
        });
    }
      /**
     * Extracts content from a streaming response chunk received from the LLM API.
     * <p>
     * This helper method parses individual chunks of the server-sent events (SSE) stream
     * from the OpenAI API and extracts the actual content text. It handles the specific
     * format used by OpenAI's streaming API where content is nested under "choices" >
     * "delta" > "content" in the JSON structure.
     * <p>
     * The method also handles special cases like:
     * <ul>
     *   <li>Data prefix removal ("data: " at the beginning of chunks)</li>
     *   <li>[DONE] marker indicating the end of the stream</li>
     *   <li>Malformed JSON chunks (returns empty string)</li>
     * </ul>
     * 
     * @param jsonChunk A single JSON chunk from the streaming API response
     * @return The extracted content text, or empty string if no content is found or parsing fails
     */
    private String extractContentFromStreamResponse(String chunk) {
        try {
            if (chunk == null || chunk.isEmpty() || "data: [DONE]".equals(chunk)) {
                return "";
            }
            
            String data = chunk.startsWith("data: ") ? chunk.substring(6) : chunk;
            
            // Skip empty or heartbeat chunks
            if (data.isEmpty() || "[DONE]".equals(data)) {
                return "";
            }
            
            JsonNode responseNode = objectMapper.readTree(data);
            
            // Standard OpenAI format
            if (responseNode.has("choices") && responseNode.path("choices").path(0).has("delta")) {
                JsonNode deltaNode = responseNode.path("choices").path(0).path("delta");
                if (deltaNode.has("content")) {
                    return deltaNode.path("content").asText();
                }
            }
            
            return "";
        } catch (Exception e) {
            logger.warn("Error parsing stream chunk: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Creates an appropriate system message for multimodal content based on model capabilities and content type.
     * Only adds system messages if the model supports vision/multimodal capabilities.
     * 
     * @param modelId The ID of the model to check capabilities for
     * @param multimodalContent The content to analyze for type detection
     * @return A system message map if appropriate, null if no system message needed
     */
    private Map<String, Object> createMultimodalSystemMessage(String modelId, Object multimodalContent) {
        // Check if the model supports vision/multimodal content
        LlmConfiguration llmConfig = llmCapabilityService.getLlmConfiguration(modelId);
        if (llmConfig == null || (!llmConfig.isSupportsImage() && !llmConfig.isSupportsPdf())) {
            // Model doesn't support multimodal content, no system message needed
            return null;
        }
        
        // Detect content type to tailor the system message
        String contentType = detectContentType(multimodalContent);
        String systemPrompt = createSystemPromptForContentType(contentType, llmConfig);
        
        if (systemPrompt == null) {
            return null;
        }
        
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        return systemMessage;
    }
    
    /**
     * Detects the type of content in multimodal input.
     * 
     * @param multimodalContent The content to analyze
     * @return Content type: "image", "pdf", "text", or "mixed"
     */
    private String detectContentType(Object multimodalContent) {
        if (multimodalContent instanceof Object[]) {
            Object[] contentArray = (Object[]) multimodalContent;
            boolean hasImage = false;
            boolean hasPdf = false;
            
            for (Object item : contentArray) {
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    String type = (String) itemMap.get("type");
                    
                    if ("image_url".equals(type)) {
                        hasImage = true;
                    } else if (itemMap.containsKey("image_url")) {
                        Object imageUrl = itemMap.get("image_url");
                        if (imageUrl instanceof Map) {
                            Map<?, ?> imageUrlMap = (Map<?, ?>) imageUrl;
                            String url = (String) imageUrlMap.get("url");
                            if (url != null && url.startsWith("data:application/pdf")) {
                                hasPdf = true;
                            } else if (url != null && url.startsWith("data:image/")) {
                                hasImage = true;
                            }
                        }
                    }
                }
            }
            
            if (hasImage && hasPdf) return "mixed";
            if (hasImage) return "image";
            if (hasPdf) return "pdf";
        }
        
        return "text";
    }
      /**
     * Creates an appropriate system prompt based on content type and model capabilities.
     * 
     * @param contentType The detected content type
     * @param llmConfig The LLM configuration
     * @return System prompt string or null if no special handling needed
     */
    private String createSystemPromptForContentType(String contentType, LlmConfiguration llmConfig) {
        switch (contentType) {
            case "image":
                if (llmConfig.isSupportsImage()) {
                    return "You are a vision-enabled AI assistant with native image analysis capabilities. You can directly analyze and describe visual content in images, including objects, people, text, scenes, colors, and other visual details. " +
                           "When working with images, you have two capabilities: " +
                           "1. Native vision analysis: You can directly see and analyze image content without needing tools " +
                           "2. Tool usage: You can also use available tools when requested for tasks like web crawling, calculations, or accessing external information " +
                           "Focus on providing detailed visual analysis of the image content, and use tools when they would enhance your response or when explicitly requested.";
                }
                break;
                
            case "pdf":
                if (llmConfig.isSupportsPdf()) {
                    return "You are an AI assistant with native PDF analysis capabilities. You can directly analyze PDF documents, understanding document structure, text content, and visual elements within PDFs. " +
                           "When working with PDFs, you have two capabilities: " +
                           "1. Native document analysis: You can directly read and analyze PDF content without needing tools " +
                           "2. Tool usage: You can also use available tools when requested for tasks like web crawling, calculations, or accessing external information " +
                           "Focus on providing comprehensive analysis of the document content, and use tools when they would enhance your response or when explicitly requested.";
                }
                break;
                
            case "mixed":
                if (llmConfig.isSupportsImage() || llmConfig.isSupportsPdf()) {
                    return "You are a multimodal AI assistant with native capabilities to analyze both images and documents. You can directly process visual content, PDFs, and other multimedia materials. " +
                           "When working with multimodal content, you have two capabilities: " +
                           "1. Native multimodal analysis: You can directly see and analyze images, read documents, and process multimedia content without needing tools " +
                           "2. Tool usage: You can also use available tools when requested for tasks like web crawling, calculations, or accessing external information " +
                           "Focus on providing detailed analysis of all provided materials, and use tools when they would enhance your response or when explicitly requested.";
                }
                break;
                
            case "text":
            default:
                // For text-only content, don't add a special system message
                // Let the model use its normal behavior and tools as appropriate
                return null;
        }
        
        return null;
    }
      /**
     * Converts MCP tools to the format expected by OpenAI API.
     * 
     * <p>This method transforms tools discovered from Model Context Protocol (MCP) servers
     * into the format required by OpenAI-compatible APIs. The main transformation ensures 
     * that all tools have the required "type": "function" field as expected by the API.
     * 
     * @param mcpTools The list of tools from the MCP server
     * @return A list of tools in the format expected by OpenAI API
     */    private List<Map<String, Object>> convertMcpToolsToOpenAIFormat(List<Map<String, Object>> mcpTools) {
        if (mcpTools == null || mcpTools.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Map<String, Object>> openAiTools = new ArrayList<>();
        
        for (Map<String, Object> mcpTool : mcpTools) {
            Map<String, Object> openAiTool = new HashMap<>();
            
            // Ensure the tool has the required "type": "function" field
            openAiTool.put("type", "function");
            
            // If the MCP tool already has a function field, use it but ensure parameters are properly formatted
            if (mcpTool.containsKey("function")) {
                Map<String, Object> functionCopy = new HashMap<>();
                
                @SuppressWarnings("unchecked")
                Map<String, Object> originalFunction = (Map<String, Object>) mcpTool.get("function");
                
                // Copy all properties from the original function
                functionCopy.putAll(originalFunction);
                  // Ensure parameters are properly formatted if they exist
                if (functionCopy.containsKey("parameters")) {
                    functionCopy.put("parameters", validateAndFormatParameters(functionCopy.get("parameters")));
                }
                // Note: Don't add empty parameters if not present - OpenAI allows functions without parameters
                
                openAiTool.put("function", functionCopy);
                logger.debug("Using existing function definition with properly formatted parameters");
            } else {
                // Create a function structure from the MCP tool properties
                Map<String, Object> function = new HashMap<>();
                
                // Use the tool name as function name, or a default if not available
                String toolName = (String) mcpTool.getOrDefault("name", 
                        mcpTool.getOrDefault("id", "mcp_tool_" + openAiTools.size()));
                function.put("name", toolName);
                
                // Add description if available
                if (mcpTool.containsKey("description")) {
                    function.put("description", mcpTool.get("description"));
                }
                  // Handle inputSchema (from MCP) as parameters (for OpenAI)
                if (mcpTool.containsKey("inputSchema")) {
                    Map<String, Object> parameters = validateAndFormatParameters(mcpTool.get("inputSchema"));
                    function.put("parameters", parameters);
                } else if (mcpTool.containsKey("parameters")) {
                    Map<String, Object> parameters = validateAndFormatParameters(mcpTool.get("parameters"));
                    function.put("parameters", parameters);
                }
                // Note: Don't add empty parameters if not present - OpenAI allows functions without parameters
                openAiTool.put("function", function);
                
                logger.debug("Converted MCP tool '{}' to OpenAI format with type 'function'", toolName);
            }
              openAiTools.add(openAiTool);
        }
        
        // Log the completed tool conversion for debugging
        if (!openAiTools.isEmpty()) {
            try {
                logger.debug("Converted {} MCP tools to OpenAI format. First tool: {}", 
                    openAiTools.size(), objectMapper.writeValueAsString(openAiTools.get(0)));
            } catch (JsonProcessingException e) {
                logger.warn("Could not serialize converted tools for logging", e);
            }
        }
        
        return openAiTools;
    }
    
    // Definition for filterMcpTools
    private List<Map<String, Object>> filterMcpTools(List<Map<String, Object>> allMcpTools, ToolSelectionRequest toolSelection) {
        if (allMcpTools == null || allMcpTools.isEmpty()) {
            return Collections.emptyList();
        }

        // If toolSelection is null or tools are explicitly disabled via enableTools=false
        if (toolSelection == null || !toolSelection.isEnableTools()) {
            logger.debug("Tools are disabled by ToolSelectionRequest (null or enableTools=false). Using 0 tools.");
            return Collections.emptyList();
        }

        // Tools are enabled (isEnableTools() is true)
        List<String> enabledToolNames = toolSelection.getEnabledTools(); // This is List<String>

        if (enabledToolNames == null || enabledToolNames.isEmpty()) {
            // No specific tools listed, so all tools are considered enabled because enableTools is true.
            logger.debug("ToolSelectionRequest has tools enabled with no specific list; using all {} available MCP tools.", allMcpTools.size());
            return allMcpTools;
        }

        // Specific tools are listed, filter by name
        logger.debug("ToolSelectionRequest is active, filtering for {} specified tool names: {}", enabledToolNames.size(), enabledToolNames);
        
        return allMcpTools.stream()
            .filter(toolMap -> {
                String toolName = null;
                // MCP tools might have name directly, or under "function" if already partially formatted
                // Prefer "function.name" if available from OpenAI formatting, then direct "name" from MCP
                Object functionProperty = toolMap.get("function");
                if (functionProperty instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> funcDetails = (Map<String, Object>) functionProperty;
                    toolName = (String) funcDetails.get("name");
                }
                if (toolName == null && toolMap.containsKey("name")) { // Fallback to top-level "name" from MCP tool definition
                     toolName = (String) toolMap.get("name"); 
                }
                
                boolean match = toolName != null && enabledToolNames.contains(toolName);
                if (match) {
                    logger.trace("Tool '{}' matched enabled tool list.", toolName);
                } else {
                    logger.trace("Tool '{}' did not match enabled tool list: {}", toolName, enabledToolNames);
                }
                return match;            })
            .collect(Collectors.toList());
    }
    
    /**
     * Validates and formats the parameters object for OpenAI function calling.
     * <p>
     * This helper method ensures that the parameters object has the required structure
     * according to OpenAI's function calling API specifications. It validates that:
     * <ul>
     *   <li>The "type" field is set to "object"</li>
     *   <li>A "properties" object exists (creating an empty one if absent)</li>
     *   <li>All nested schema objects have proper formats</li>
     * </ul>
     * 
     * @param rawParameters The raw parameters object from MCP format
     * @return A properly formatted parameters object for OpenAI
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> validateAndFormatParameters(Object rawParameters) {
        if (rawParameters == null) {
            Map<String, Object> defaultParams = new HashMap<>();
            defaultParams.put("type", "object");
            defaultParams.put("properties", new HashMap<>());
            return defaultParams;
        }
        
        if (!(rawParameters instanceof Map)) {
            logger.warn("Parameters is not a Map object. Creating default parameters structure.");
            Map<String, Object> defaultParams = new HashMap<>();
            defaultParams.put("type", "object");
            defaultParams.put("properties", new HashMap<>());
            return defaultParams;
        }
        
        Map<String, Object> parameters = new HashMap<>((Map<String, Object>) rawParameters);
        
        // Ensure type is set to "object" as required by OpenAI
        if (!parameters.containsKey("type")) {
            parameters.put("type", "object");
        }
        
        // Ensure properties exists
        if (!parameters.containsKey("properties")) {
            parameters.put("properties", new HashMap<>());
        } else if (!(parameters.get("properties") instanceof Map)) {
            logger.warn("Properties is not a Map object. Creating empty properties.");
            parameters.put("properties", new HashMap<>());
        }
        
        // Log the formatted parameters for debugging
        try {
            logger.debug("Formatted parameters for OpenAI: {}", 
                objectMapper.writeValueAsString(parameters));
        } catch (JsonProcessingException e) {
            logger.warn("Could not serialize parameters for logging", e);
        }
        
        return parameters;
    }
    
    // Updated executeStreamingConversationWithToolsInternal
    private void executeStreamingConversationWithToolsInternal(
            List<com.steffenhebestreit.ai_research.Model.ChatMessage> originalMessages,
            String modelId,
            FluxSink<String> sink,
            ToolSelectionRequest toolSelection,
            AtomicBoolean cancellationFlag) {

        final AtomicBoolean sinkClosed = new AtomicBoolean(false);
        // Create a mutable copy of messages that can be modified
        // Use an AtomicReference to hold our messages list so we can modify it safely from lambdas
        final AtomicReference<List<ChatMessage>> messagesRef = new AtomicReference<>(new ArrayList<>(originalMessages));

        if (cancellationFlag.get()) {
            tryCloseSinkWithError(sink, sinkClosed, new CancellationException("Stream cancelled by client before start."), modelId, cancellationFlag);
            return;
        }
        
        List<Map<String, Object>> messagesForLlm = prepareMessagesForLlm(messagesRef.get());

        if (messagesForLlm.isEmpty() || (messagesForLlm.size() == 1 && "system".equals(messagesForLlm.get(0).get("role")) && messagesRef.get().isEmpty())) {
            tryCloseSinkWithError(sink, sinkClosed, new IllegalArgumentException("Cannot send an effectively empty message list to LLM."), modelId, cancellationFlag);
            return;
        }

        // Log information about multimodal content for debugging
        for (int i = 0; i < messagesForLlm.size(); i++) {
            Map<String, Object> msgMap = messagesForLlm.get(i);
            if (i < messagesRef.get().size() && "multipart/mixed".equals(messagesRef.get().get(i).getContentType())) {
                logger.debug("Sending multimodal content in message {} to LLM model {}. Role: {}", 
                    i, modelId, msgMap.get("role"));
            }
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", messagesForLlm);
        requestBody.put("stream", true);        // Check if we have multimodal content in the messages
        boolean hasMultimodalContent = messagesRef.get().stream()
            .anyMatch(msg -> "multipart/mixed".equals(msg.getContentType()));
        
        // Get MCP tools and handle tool selection
        List<Map<String, Object>> allMcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        // toolsForRequest must be effectively final for use in lambda
        final List<Map<String, Object>> finalToolsForRequest;
        
        ToolSelectionRequest effectiveToolSelection = toolSelection;
        if (effectiveToolSelection == null) {
            effectiveToolSelection = new ToolSelectionRequest(true, null);
            logger.debug("No ToolSelectionRequest provided, defaulting to enableTools=true, enabledTools=null (effectively 'all available tools').");
        }

        // Don't include tools when we have multimodal content - let the LLM use its native vision capabilities
        if (hasMultimodalContent) {
            logger.info("Multimodal content detected. Excluding tools to let LLM focus on native vision capabilities for modelId: {}", modelId);
            finalToolsForRequest = Collections.emptyList();
        } else if (allMcpTools != null && !allMcpTools.isEmpty() && effectiveToolSelection.isEnableTools()) {
            List<Map<String, Object>> filteredMcpTools = filterMcpTools(allMcpTools, effectiveToolSelection);
            if (!filteredMcpTools.isEmpty()) {
                List<Map<String, Object>> convertedTools = convertMcpToolsToOpenAIFormat(filteredMcpTools);
                if (!convertedTools.isEmpty()) {
                    logger.info("Adding {} formatted tools to LLM request for modelId: {} (filtered from {} MCP tools based on selection: {})",
                            convertedTools.size(), modelId, allMcpTools.size(), effectiveToolSelection.getEnabledTools());
                    requestBody.put("tools", convertedTools);
                    finalToolsForRequest = convertedTools;
                } else {
                     logger.info("No tools to include in request for modelId: {} after formatting filtered MCP tools. Filtered count: {}, All MCP: {}", modelId, filteredMcpTools.size(), allMcpTools.size());
                     finalToolsForRequest = Collections.emptyList();
                }
            } else {
                logger.info("No tools to include in request for modelId: {} due to tool selection filter on MCP tools. All MCP: {}, Selection: {}", modelId, allMcpTools.size(), effectiveToolSelection.getEnabledTools());
                finalToolsForRequest = Collections.emptyList();
            }
        } else if (allMcpTools == null || allMcpTools.isEmpty()){
             logger.debug("No MCP tools available to add to LLM request for modelId: {}", modelId);
             finalToolsForRequest = Collections.emptyList();
        } else { 
            logger.debug("Tools are disabled for modelId: {} as per ToolSelectionRequest.", modelId);
            finalToolsForRequest = Collections.emptyList();
        }

        StringBuilder assistantResponseContent = new StringBuilder();
        Map<Integer, StreamingToolCall> activeToolCalls = new HashMap<>();
        AtomicReference<String> finishReasonRef = new AtomicReference<>();
        AtomicReference<String> currentLlmRole = new AtomicReference<>(); // Stores the role from the current LLM delta
        
        String path = "/chat/completions"; // API endpoint path
        logger.info("Initiating streaming request to LLM path: {} for model: {} with {} messages", path, modelId, messagesForLlm.size());
        
        // Log the first few characters of the request body for debugging (without exposing sensitive data)
        try {
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            logger.debug("Request body size: {} characters for model: {}", requestBodyJson.length(), modelId);
            if (logger.isTraceEnabled()) {
                // Only log full request in trace mode to avoid cluttering logs
                logger.trace("Full request body for model {}: {}", modelId, requestBodyJson);
            }
        } catch (Exception e) {
            logger.warn("Failed to serialize request body for logging: {}", e.getMessage());
        }

        webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(sub -> logger.info("Started stream subscription for model: {} with {} messages", modelId, messagesForLlm.size()))
                .doOnNext(event -> logger.trace("Received raw stream event for model {}: {}", modelId, event))
                .doOnError(error -> logger.error("Stream error for model {}: {}", modelId, error.getMessage(), error))
                .takeUntil(event -> cancellationFlag.get() || sinkClosed.get())
                .doOnCancel(() -> {
                    logger.info("WebClient Flux externally cancelled for modelId: {}. Setting cancellation flag.", modelId);
                    cancellationFlag.set(true);
                })
                .subscribe(
                        rawEvent -> {
                            if (cancellationFlag.get() || sinkClosed.get()) return;

                            logger.debug("Raw streaming event for modelId {}: {}", modelId, rawEvent);
                            String jsonChunk = rawEvent.trim();
                            if (jsonChunk.startsWith("data: ")) {
                                jsonChunk = jsonChunk.substring(6).trim();
                            }
                            if ("[DONE]".equalsIgnoreCase(jsonChunk) || jsonChunk.isEmpty()) {
                                return;
                            }

                            try {
                                Map<String, Object> chunkMap = objectMapper.readValue(jsonChunk, new TypeReference<Map<String, Object>>() {});
                                
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunkMap.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Object choiceObj = choices.get(0);
                                    if (choiceObj instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> choice = (Map<String, Object>) choiceObj;
                                        
                                        // Check for content in delta
                                        Object deltaObj = choice.get("delta");
                                        if (deltaObj instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> delta = (Map<String, Object>) deltaObj;
                                            Object contentObj = delta.get("content");
                                            if (contentObj instanceof String) {
                                                String content = (String) contentObj;
                                                currentLlmRole.set("assistant");
                                                assistantResponseContent.append(content);
                                                if (!sinkClosed.get()) {
                                                    sink.next(content);
                                                }
                                            }
                                        }
                                        
                                        // Check for finish reason
                                        Object finishReasonObj = choice.get("finish_reason");
                                        if (finishReasonObj instanceof String) {
                                            finishReasonRef.set((String) finishReasonObj);
                                        }
                                        
                                        // Check for tool calls in message
                                        Object messageObj = choice.get("message");
                                        if (messageObj instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> message = (Map<String, Object>) messageObj;
                                            if (message.containsKey("tool_calls")) {
                                                @SuppressWarnings("unchecked")
                                                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
                                                if (toolCalls != null && !toolCalls.isEmpty()) {
                                                    currentLlmRole.set("tool");
                                                    // Process tool calls - this was missing in the previous implementation
                                                    for (Map<String, Object> toolCall : toolCalls) {
                                                        Integer index = toolCall.containsKey("index") ? 
                                                                            ((Number) toolCall.get("index")).intValue() : 
                                                                            activeToolCalls.size();
                                                            
                                                        if (!activeToolCalls.containsKey(index)) {
                                                            activeToolCalls.put(index, new StreamingToolCall(index));
                                                        }
                                                            
                                                        StreamingToolCall stc = activeToolCalls.get(index);
                                                            
                                                        if (toolCall.containsKey("id")) {
                                                            stc.setId((String) toolCall.get("id"));
                                                        }
                                                            
                                                        if (toolCall.containsKey("type")) {
                                                            stc.setType((String) toolCall.get("type"));
                                                        }
                                                            
                                                        // Process function data if present
                                                        if (toolCall.containsKey("function")) {
                                                            @SuppressWarnings("unchecked")
                                                            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                                                            
                                                            if (function.containsKey("name")) {
                                                                stc.appendName((String) function.get("name"));
                                                            }
                                                            
                                                            if (function.containsKey("arguments")) {
                                                                stc.appendArguments((String) function.get("arguments"));
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Check for tool calls in delta
                                        Object deltaToolCallsObj = null;
                                        if (deltaObj instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> delta = (Map<String, Object>) deltaObj;
                                            deltaToolCallsObj = delta.get("tool_calls");
                                        }
                                        
                                        if (deltaToolCallsObj instanceof List) {
                                            @SuppressWarnings("unchecked")
                                            List<Map<String, Object>> deltaToolCalls = (List<Map<String, Object>>) deltaToolCallsObj;
                                            if (!deltaToolCalls.isEmpty()) {
                                                currentLlmRole.set("tool");
                                                // Process delta tool calls
                                                for (Map<String, Object> deltaToolCall : deltaToolCalls) {
                                                    Integer index = deltaToolCall.containsKey("index") ?
                                                                      ((Number) deltaToolCall.get("index")).intValue() :
                                                                      activeToolCalls.size();
                                                
            
                                                    if (!activeToolCalls.containsKey(index)) {
                                                        activeToolCalls.put(index, new StreamingToolCall(index));
                                                    }
                                                
            
                                                    StreamingToolCall stc = activeToolCalls.get(index);
                                                
            
                                                    if (deltaToolCall.containsKey("id")) {
                                                        stc.setId((String) deltaToolCall.get("id"));
                                                    }
                                                
            
                                                    if (deltaToolCall.containsKey("type")) {
                                                        stc.setType((String) deltaToolCall.get("type"));
                                                    }
                                                
            
                                                    // Process function delta data
                                                    if (deltaToolCall.containsKey("function")) {
                                                        @SuppressWarnings("unchecked")
                                                        Map<String, Object> function = (Map<String, Object>) deltaToolCall.get("function");
                                                
            
                                                        if (function.containsKey("name")) {
                                                            stc.appendName((String) function.get("name"));
                                                        }
                                                
            
                                                        if (function.containsKey("arguments")) {
                                                            stc.appendArguments((String) function.get("arguments"));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (JsonProcessingException e) {
                                logger.error("Error parsing streaming JSON chunk for modelId {}: '{}'. Terminating stream.", modelId, jsonChunk, e);
                                tryCloseSinkWithError(sink, sinkClosed, e, modelId, cancellationFlag);
                            } catch (Exception e) { 
                                logger.error("Unexpected error processing streaming chunk for modelId {}: '{}'. Terminating stream.", modelId, jsonChunk, e);
                                tryCloseSinkWithError(sink, sinkClosed, e, modelId, cancellationFlag);
                            }
                        },
                        error -> { 
                            if (cancellationFlag.get() && !(error instanceof CancellationException)) {
                                logger.info("WebClient Flux error for modelId {} during cancellation: {}", modelId, error.getMessage());
                                tryCloseSinkWithError(sink, sinkClosed, new CancellationException("Stream cancelled, underlying error: " + error.getMessage()), modelId, cancellationFlag);
                            } else {
                                logger.error("Error in streaming response from WebClient for modelId {}:", modelId, error);
                                tryCloseSinkWithError(sink, sinkClosed, error, modelId, cancellationFlag);
                            }
                        },
                        () -> { 
                            if (cancellationFlag.get()) {
                                logger.info("WebClient Flux completed for modelId {}, but operation was cancelled.", modelId);
                                tryCloseSinkWithError(sink, sinkClosed, new CancellationException("Stream cancelled before natural completion."), modelId, cancellationFlag);
                                return;
                            }
                            if (sinkClosed.get()) { 
                                logger.info("WebClient Flux completed for modelId {}, but sink was already closed.", modelId);
                                return;
                            }

                            String finishReason = finishReasonRef.get();
                            logger.info("LLM stream completed for modelId {}. Final finish_reason: {}", modelId, finishReason);

                            com.steffenhebestreit.ai_research.Model.ChatMessage assistantMessage = new com.steffenhebestreit.ai_research.Model.ChatMessage();
                            assistantMessage.setRole(currentLlmRole.get() != null ? currentLlmRole.get() : "assistant"); // Use observed role or default to assistant
                            String accumulatedContent = assistantResponseContent.toString();
                            assistantMessage.setContent(accumulatedContent.isEmpty() ? null : accumulatedContent);
                            
                            List<Map<String, Object>> completedToolCallMaps = new ArrayList<>();
                            activeToolCalls.values().forEach(stc -> {
                                if (stc.isComplete()) {
                                    completedToolCallMaps.add(stc.build());
                                } else {
                                    logger.warn("Dropping incomplete tool call during assembly: Index {}, ID {}, Name '{}', Args (partial) '{}'", 
                                        stc.index, stc.id, stc.functionName.toString(), stc.functionArguments.toString().substring(0, Math.min(50, stc.functionArguments.length())));
                                }
                            });

                            if (!completedToolCallMaps.isEmpty()) {
                                assistantMessage.setToolCalls(completedToolCallMaps);
                            }

                            if (assistantMessage.getContent() != null || (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty())) {
                                // Create a final copy for lambda use
                                final ChatMessage finalAssistantMessage = assistantMessage;
                                
                                // Update our mutable messages list in the AtomicReference
                                List<ChatMessage> currentMessages = messagesRef.get();
                                List<ChatMessage> updatedMessages = new ArrayList<>(currentMessages);
                                updatedMessages.add(finalAssistantMessage);
                                messagesRef.set(updatedMessages);
                            }

                            if ("tool_calls".equals(finishReason) && !completedToolCallMaps.isEmpty()) {
                                if (!sinkClosed.get()) { 
                                    sink.next("\\n[Tool calls requested by LLM. Executing tools...]\\n");
                                }
                                LlmConfiguration llmConfig = llmCapabilityService.getLlmConfiguration(modelId);
                                if (llmConfig == null) {
                                    logger.warn("LlmConfiguration not found for modelId: {}. Proceeding with tool execution but some capabilities might be unknown.", modelId);
                                    llmConfig = new LlmConfiguration(); 
                                    llmConfig.setId(modelId); 
                                }
                                
                                // Use finalToolsForRequest here
                                final LlmConfiguration finalLlmConfig = llmConfig;
                                
                                // Use the current messages from our AtomicReference
                                handleToolCallsAndContinue(completedToolCallMaps, messagesRef.get(), modelId, sink, toolSelection, 
                                    cancellationFlag, sinkClosed, finalLlmConfig, finalToolsForRequest);
                            } else {
                                if ("tool_calls".equals(finishReason) && completedToolCallMaps.isEmpty()) {
                                     logger.warn("Finish reason was 'tool_calls' but no complete tool calls were assembled for modelId {}. Treating as normal completion.", modelId);
                                }
                                tryCloseSinkWithCompletion(sink, sinkClosed, modelId, "Normal completion after conversation", cancellationFlag);
                            }
                        }
                );
    }    private void handleToolCallsAndContinue(
        List<Map<String, Object>> completedToolCalls, 
        final List<ChatMessage> originalCurrentMessages,
        String modelId,
        FluxSink<String> sink,
        ToolSelectionRequest toolSelection,
        AtomicBoolean cancellationFlag,
        AtomicBoolean sinkClosed,
        LlmConfiguration llmConfiguration, 
        List<Map<String, Object>> availableTools
    ) {
        // Create a mutable copy of the messages to work with
        final List<ChatMessage> currentMessages = new ArrayList<>(originalCurrentMessages);
        
        if (cancellationFlag.get()) {
            logger.info("handleToolCallsAndContinue: Cancellation detected for modelId: {}. Aborting tool call processing.", modelId);
            tryCloseSinkWithError(sink, sinkClosed, new CancellationException("Tool call handling cancelled for modelId: " + modelId), modelId, cancellationFlag);
            return;
        }
        if (sinkClosed.get()) {
            logger.info("handleToolCallsAndContinue: Sink already closed for modelId: {}. Aborting tool call processing.", modelId);
            return;
        }

        List<Mono<ChatMessage>> toolExecutionMonos = new ArrayList<>();

        for (Map<String, Object> toolCall : completedToolCalls) {
            if (cancellationFlag.get() || sinkClosed.get()) {
                logger.info("handleToolCallsAndContinue: Skipping further tool processing due to cancellation or closed sink for modelId: {}", modelId);
                break; 
            }

            Mono<ChatMessage> toolResponseMono = executeToolCallFromNode(toolCall, availableTools, llmConfiguration, modelId)
                .doOnSuccess(toolResponse -> { 
                    if (toolResponse == null) { 
                        logger.warn("Tool execution for modelId {} returned null response for tool call: {}", modelId, toolCall.get("id"));
                    }
                })
                .doOnError(e -> {
                    logger.error("Error during execution setup or subscription for a tool for modelId {}: {}. Tool ID: {}", modelId, e.getMessage(), toolCall.get("id"));
                });
            toolExecutionMonos.add(toolResponseMono);
        }

        if (toolExecutionMonos.isEmpty()) {
            if (!completedToolCalls.isEmpty()) {
                 logger.warn("No tool execution Monos created for modelId {} despite having {} completed tool calls. Likely due to prior cancellation/closure.", modelId, completedToolCalls.size());
            }
            tryCloseSinkWithCompletion(sink, sinkClosed, modelId, "No tools processed or all skipped, completing current step.", cancellationFlag);
            return;
        }

        // Execute tool calls sequentially, collect all results, delay errors.
        Flux.fromIterable(toolExecutionMonos)
            .concatMapDelayError(java.util.function.Function.identity()) // Process Monos sequentially, delaying errors
            .collectList() // Collects all ChatMessage results from successfully completed Monos
            .subscribe(
                successfulResponses -> { // successfulResponses is List<ChatMessage> from successfully executed tool calls
                    if (cancellationFlag.get()) {
                        logger.info("Tool executions completed for modelId {}, but operation was cancelled before continuing conversation.", modelId);
                        tryCloseSinkWithError(sink, sinkClosed, new CancellationException("Tool processing completed but stream cancelled for modelId: " + modelId), modelId, cancellationFlag);
                        return;
                    }
                    if (sinkClosed.get()) {
                        logger.info("Tool executions completed for modelId {}, but sink was already closed before continuing conversation.", modelId);
                        return;
                    }                    // Filter out null responses (from failed tool executions)
                    final List<ChatMessage> validResponses = successfulResponses.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                    
                    // Create a final reference to currentMessages for lambda use                    // Create a variable to hold our messages for the lambda
                    List<ChatMessage> effectiveMessages;
                        
                    // Safely add tool responses to the conversation context
                    try {
                        currentMessages.addAll(validResponses); // Add all successfully executed tool responses
                        effectiveMessages = currentMessages;
                    } catch (UnsupportedOperationException ex) {
                        // Handle case where currentMessages is an immutable collection
                        logger.warn("Detected immutable message collection during tool execution for modelId {}. Creating mutable copy.", modelId);
                        List<ChatMessage> mutableMessages = new ArrayList<>(currentMessages);
                        mutableMessages.addAll(validResponses);
                        // Use the mutable collection for continuing conversation
                        effectiveMessages = mutableMessages;
                    }

                    // Make this final for use in lambda expressions
                    final List<ChatMessage> finalCurrentMessages = effectiveMessages;

                    if (!validResponses.isEmpty()) {
                        if (!sinkClosed.get()) {
                             for (ChatMessage toolRespMsg : validResponses) {
                                 String toolResponseMessageContent = String.format("\\n[Tool %s executed. Result (preview): %s]\\n", 
                                                                          toolRespMsg.getName(), 
                                                                          toolRespMsg.getContent() != null ? toolRespMsg.getContent().substring(0, Math.min(100, toolRespMsg.getContent().length())) : "null");
                                 if (!sinkClosed.get()) {
                                     sink.next(toolResponseMessageContent);
                                 } else {
                                     logger.warn("Sink closed while trying to send tool execution result for modelId: {}", modelId);
                                     break; 
                                 }
                             }
                        }                        logger.info("Continuing conversation for modelId {} after {} successful tool responses.", modelId, validResponses.size());                        try {
                            // Always create a fresh mutable copy of currentMessages to avoid potential immutability issues
                            executeStreamingConversationWithToolsInternal(
                                new ArrayList<>(finalCurrentMessages), 
                                modelId, 
                                sink, 
                                toolSelection, 
                                cancellationFlag
                            );
                        } catch (UnsupportedOperationException ex) {
                            logger.warn("UnsupportedOperationException during conversation continuation for modelId {}. Attempting to recover.", modelId);
                            // This is unlikely to happen since we're already creating a new ArrayList, but just in case
                            tryCloseSinkWithError(sink, sinkClosed, 
                                new IllegalStateException("Failed to continue conversation after tool execution: " + ex.getMessage(), ex), 
                                modelId, cancellationFlag);
                        }
                    } else {
                        logger.warn("No successful tool responses to add for modelId {}. This might happen if all tool calls failed (errors delayed and caught below) or were filtered. Completing stream.", modelId);
                        tryCloseSinkWithCompletion(sink, sinkClosed, modelId, "No successful tool responses to continue conversation.", cancellationFlag);
                    }
                },
                error -> { // This catches errors from concatMapDelayError (i.e., from tool executions)
                    logger.error("Error during sequential execution of tool calls for modelId {}:", modelId, error);
                    tryCloseSinkWithError(sink, sinkClosed, error, modelId, cancellationFlag);
                }
            );
    }


    // Updated to include modelId for logging
    private Mono<ChatMessage> executeToolCallFromNode(
        Map<String, Object> toolCallToExecute, 
        List<Map<String, Object>> availableTools, 
        LlmConfiguration llmConfiguration,
        String modelId // Added modelId
    ) {
        @SuppressWarnings("unchecked")
        String toolName = (String) ((Map<String, Object>) toolCallToExecute.get("function")).get("name");
        String toolCallId = (String) toolCallToExecute.get("id");
        @SuppressWarnings("unchecked")
        String toolArgs = (String) ((Map<String, Object>) toolCallToExecute.get("function")).get("arguments");

        // Check if the requested tool is actually available/enabled
        boolean toolAvailableAndEnabled = availableTools != null && availableTools.stream()
            .anyMatch(t -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> functionMap = (Map<String, Object>) t.get("function");
                return functionMap != null && toolName.equals(functionMap.get("name"));
            });

        if (!toolAvailableAndEnabled) {
            logger.warn("Tool '{}' requested by LLM for modelId {} is not in the list of available/enabled tools. Skipping execution.", toolName, modelId);
            String errorResult = String.format("{\"error\": \"Tool '%s' is not available or not enabled for execution.\"}", toolName);
            // Return a "tool" role message indicating the error
            return Mono.just(new ChatMessage("tool", errorResult, toolCallId, toolName));
        }        logger.info("Executing tool: Name: {}, ID: {}, Args (preview): {} for modelId: {}", toolName, toolCallId, toolArgs.substring(0, Math.min(100, toolArgs.length())), modelId);

        return Mono.fromCallable(() -> {
            String resultJson;
            try {
                // Parse tool arguments from JSON string to Map
                @SuppressWarnings("unchecked")
                Map<String, Object> argumentsMap = objectMapper.readValue(toolArgs, Map.class);
                
                // Execute the tool via DynamicIntegrationService
                Map<String, Object> toolResult = dynamicIntegrationService.executeToolCall(toolName, argumentsMap);
                
                if (toolResult != null) {
                    // Tool execution was successful, serialize the result
                    resultJson = objectMapper.writeValueAsString(toolResult);
                    logger.info("Tool '{}' (ID: {}) executed successfully for modelId: {}", toolName, toolCallId, modelId);
                } else {
                    // Tool execution failed, create error response
                    logger.error("Tool execution returned null for '{}' (ID: {}) for modelId: {}", toolName, toolCallId, modelId);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "Tool execution failed - no result returned");
                    errorResult.put("tool_name", toolName);
                    errorResult.put("tool_call_id", toolCallId);
                    resultJson = objectMapper.writeValueAsString(errorResult);
                }

            } catch (Exception e) {
                logger.error("Error executing tool {} (ID: {}) for modelId {}:", toolName, toolCallId, modelId, e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                errorResult.put("tool_name", toolName);
                errorResult.put("tool_call_id", toolCallId);
                resultJson = objectMapper.writeValueAsString(errorResult);
            }
            // Create a "tool" role ChatMessage with the result
            return new ChatMessage("tool", resultJson, toolCallId, toolName);
        });
    }

    /**
     * Retrieves the list of available LLM models from the API endpoint.
     * <p>
     * This method queries the OpenAI-compatible API for available models and enhances them with
     * capability information. It first attempts to fetch models from the API endpoint. If that fails,
     * it falls back to a default model configuration based on the application properties.
     * <p>
     * The method handles various error conditions including:
     * <ul>
     *   <li>Invalid or missing API responses</li>
     *   <li>Models with missing information (like empty ID)</li>
     *   <li>Network connectivity issues</li>
     * </ul>
     * <p>
     * Each returned model contains:
     * <ul>
     *   <li>Model ID - Unique identifier for the model</li>
     *   <li>Display name - User-friendly name for UI display</li>
     *   <li>Capability flags - What features the model supports (text, vision, etc.)</li>
     *   <li>Token limits - Maximum context window size</li>
     * </ul>
     *
     * @return List of ProviderModel objects representing available models with their capabilities
     */
    public List<ProviderModel> getAvailableModels() {
        logger.info("Fetching available models from API endpoint: {}", openAIProperties.getBaseurl());
        
        try {
            // Make API call to fetch models
            String responseJson = webClient.get()
                .uri("/models")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (responseJson == null || responseJson.isEmpty()) {
                logger.warn("Received empty response from models endpoint");
                return Collections.emptyList();
            }
              // Parse response to extract model data
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode dataNode = rootNode.path("data");
            
            List<ProviderModel> models = new ArrayList<>();            // If we have a "data" array (OpenAI format)
            if (dataNode.isArray()) {
                for (JsonNode modelNode : dataNode) {
                    String id = modelNode.path("id").asText();
                    
                    // Skip non-chat models and internal models
                    if (shouldSkipModel(id)) {
                        continue;
                    }
                    
                    ProviderModel model = createProviderModel(id, modelNode);
                    models.add(model);
                }
            } else if (dataNode.isMissingNode() || dataNode.isNull()) {
                // Data field is missing or null - test case expects empty list here
                logger.warn("Data field is missing in API response");
                return Collections.emptyList();
            }
              // Sort models by name
            models.sort((m1, m2) -> {
                String name1 = ProviderModelAdapter.getDisplayName(m1);
                String name2 = ProviderModelAdapter.getDisplayName(m2);
                return name1 != null && name2 != null ? name1.compareToIgnoreCase(name2) : 0;
            });
            
            logger.info("Retrieved {} models from API", models.size());
            return models;
        } catch (Exception e) {
            logger.error("Error fetching available models", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Determines if a model should be skipped based on its ID.
     * 
     * @param id The model ID to check
     * @return True if the model should be skipped, false otherwise
     */
    private boolean shouldSkipModel(String id) {
        // Skip if ID is empty
        if (id == null || id.isEmpty()) {
            return true;
        }
        
        // Skip internal/hidden models
        if (id.startsWith("internal-") || id.contains("-internal")) {
            return true;
        }
        
        // Skip embedding models (not chat models)
        if (id.contains("embedding") || id.contains("search") || id.endsWith("-e")) {
            return true;
        }
        
        // Skip moderation models
        if (id.contains("moderation")) {
            return true;
        }
        
        return false;
    }
    
    /**
 * Creates a ProviderModel object from model data.
 * 
 * @param id The model ID
 * @param modelNode The JSON node containing model data
 * @return A ProviderModel object with appropriate metadata
 */
private ProviderModel createProviderModel(String id, JsonNode modelNode) {
    ProviderModel model = new ProviderModel();
    model.setId(id);
    
    // Set provider name (extract from ID or use default)
    String provider = determineProviderFromId(id);
    ProviderModelAdapter.setProvider(model, provider);
    
    // Check for capabilities in LlmCapabilityService
    LlmConfiguration llmConfig = llmCapabilityService.getLlmConfiguration(id);
    
    if (llmConfig != null) {
        // Use name from configuration if available
        String configName = llmConfig.getName();
        if (configName != null && !configName.isEmpty()) {
            ProviderModelAdapter.setDisplayName(model, configName);
        } else {
            // Set display name (use id if not available)
            String displayName = modelNode.path("name").asText(null);
            if (displayName == null || displayName.isEmpty()) {
                displayName = getPrettyModelName(id);
            }
            ProviderModelAdapter.setDisplayName(model, displayName);
        }
        
        // Use capabilities from configuration
        ProviderModelAdapter.setTokenLimit(model, LlmConfigurationAdapter.getMaxTokens(llmConfig));
        ProviderModelAdapter.setSupportsText(model, true); // Assume all models support text
        ProviderModelAdapter.setSupportsImage(model, llmConfig.isSupportsImage());
        ProviderModelAdapter.setSupportsPdf(model, llmConfig.isSupportsPdf());
        ProviderModelAdapter.setSupportsJson(model, LlmConfigurationAdapter.isSupportsJson(llmConfig));
        ProviderModelAdapter.setSupportsTools(model, LlmConfigurationAdapter.isSupportsTools(llmConfig));
        
        // Set description and capabilities
        StringBuilder description = new StringBuilder();
        if (llmConfig.getDescription() != null && !llmConfig.getDescription().isEmpty()) {
            description.append(llmConfig.getDescription());
        }
        if (llmConfig.getNotes() != null && !llmConfig.getNotes().isEmpty()) {
            if (description.length() > 0) {
                description.append(" ");
            }
            description.append(llmConfig.getNotes());
        }
        model.setDescription(description.toString());
        model.setCapabilities("(Configured)");
    } else {
        // Set display name (use id if not available)
        String displayName = modelNode.path("name").asText(null);
        if (displayName == null || displayName.isEmpty()) {
            displayName = getPrettyModelName(id);
        }
        ProviderModelAdapter.setDisplayName(model, displayName);
        
        // Apply fallback values
        ProviderModelAdapter.setTokenLimit(model, 4096); // Conservative default
        ProviderModelAdapter.setSupportsText(model, true);
        ProviderModelAdapter.setSupportsImage(model, detectVisionCapability(id));
        ProviderModelAdapter.setSupportsPdf(model, detectVisionCapability(id)); // Assume PDF support matches image
        ProviderModelAdapter.setSupportsJson(model, true); // Most modern models support JSON
        ProviderModelAdapter.setSupportsTools(model, detectToolCapability(id));
        
        // Set fallback description and capabilities
        model.setDescription("Basic fallback configuration for " + displayName);
        model.setCapabilities("(Fallback)");
    }
    
    return model;
}
    
    /**
     * Creates a human-readable model name from a model ID.
     * 
     * @param id The model ID
     * @return A formatted display name
     */
    private String getPrettyModelName(String id) {
        // Remove provider prefix if present
        String name = id;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }
        
        // Replace hyphens with spaces and capitalize words
        StringBuilder prettyName = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : name.toCharArray()) {
            if (c == '-' || c == '_') {
                prettyName.append(' ');
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    prettyName.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    prettyName.append(c);
                }
            }
        }
        
        return prettyName.toString();
    }
    
    /**
     * Determines the provider name from a model ID.
     * 
     * @param id The model ID
     * @return The provider name
     */    private String determineProviderFromId(String id) {
        if (id.startsWith("gpt-") || id.startsWith("dall-e")) {
            return "openai";
        } else if (id.startsWith("anthropic") || id.startsWith("claude")) {
            return "anthropic";
        } else if (id.startsWith("mistral") || id.contains("mistral")) {
            return "mistral";
        } else if (id.startsWith("gemini") || id.contains("gemini")) {
            return "google";
        } else if (id.startsWith("llama") || id.contains("llama")) {
            return "meta";
        } else if (id.contains("mixtral")) {
            return "mistral";
        } else if (id.startsWith("glm") || id.contains("chatglm")) {
            return "thudm";
        } else {
            // Check for provider prefixes
            if (id.contains(":")) {
                return id.substring(0, id.indexOf(":")).toLowerCase();
            }
            
            return "custom-provider";
        }
    }
    
    /**
     * Detects if a model likely supports vision capabilities based on its ID.
     * 
     * @param id The model ID
     * @return True if the model likely supports vision, false otherwise
     */
    private boolean detectVisionCapability(String id) {
        String lowerCaseId = id.toLowerCase();
        
        // Known vision-capable models
        return lowerCaseId.contains("vision") || 
           lowerCaseId.contains("-v") || 
           lowerCaseId.equals("gpt-4-turbo") || 
           lowerCaseId.contains("gpt-4o") || 
           lowerCaseId.equals("claude-3-opus") || 
           lowerCaseId.equals("claude-3-sonnet") || 
           lowerCaseId.equals("claude-3-haiku") || 
           lowerCaseId.contains("gemini-pro-vision") || 
           lowerCaseId.contains("gemini-1.5");
    }
      /**
     * Detects if a model likely supports tool calling based on its ID.
     * 
     * @param id The model ID
     * @return True if the model likely supports tools, false otherwise
     */
    private boolean detectToolCapability(String id) {
        String lowerCaseId = id.toLowerCase();
        
        // Models known to not support function/tool calling
        if (lowerCaseId.contains("instruct") || 
            lowerCaseId.contains("davinci") || 
            lowerCaseId.contains("babbage") || 
            lowerCaseId.contains("curie") || 
            lowerCaseId.contains("ada")) {
            return false;
        }
        
        // Return true by default for unknown models and those known to support function/tool calling
        // This matches the test expectation in testGetAvailableModels_FallbackLogic
        return true;
    }

    // Convenience method for streaming with tool execution using the default model
    public Flux<String> getChatCompletionStreamWithToolExecution(List<ChatMessage> messages) {
        return getChatCompletionStreamWithToolExecution(messages, openAIProperties.getModel());
    }

    /**
     * Gets a streaming completion from the LLM with tool execution capability.
     * <p>
     * This method processes a list of chat messages and returns a reactive stream
     * of response chunks with support for tool calls. The method handles parsing
     * of tool calls from the stream and includes them in the response format.
     *
     * @param messages The list of chat messages to process
     * @param modelId The ID of the LLM model to use
     * @return A Flux of response chunks with tool call information
     */
    public Flux<String> getChatCompletionStreamWithToolExecution(List<ChatMessage> messages, String modelId) {
        return getChatCompletionStreamWithToolExecution(messages, modelId, null);
    }
    /**
     * Gets a streaming completion from the LLM with tool execution capability and tool selection.
     * <p>
     * This method processes a list of chat messages and returns a reactive stream
     * of response chunks with support for tool calls. It allows specifying which tools
     * should be enabled for the request.
     *
     * @param messages The list of chat messages to process
     * @param modelId The ID of the LLM model to use
     * @param toolSelection Optional configuration for tool selection/filtering
     * @return A Flux of response chunks with tool call information
     */
    public Flux<String> getChatCompletionStreamWithToolExecution(
            List<ChatMessage> messages, 
            String modelId,
            ToolSelectionRequest toolSelection) {
        
        AtomicBoolean cancellationFlag = new AtomicBoolean(false);
        
        return Flux.create(sink -> {
            executeStreamingConversationWithToolsInternal(messages, modelId, sink, toolSelection, cancellationFlag);
        });
    }

    /**
     * Gets a completion from the LLM for a given prompt synchronously.
     * <p>
     * This method sends a simple text prompt to the LLM and returns the complete response
     * as a single string. It uses the default model specified in the configuration.
     * <p>
     * This is a convenience method for simple, non-streaming text completions without
     * conversation history or additional options.
     *
     * @param prompt The text prompt to send to the LLM
     * @return The LLM's response as text
     */
    /* Method commented out due to duplicate definition with getChatCompletion(String message)
    public String getChatCompletion(String prompt) {
        // Create a simple user message with the prompt
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userMessage = new ChatMessage("user", prompt);
        messages.add(userMessage);
        
        // Use the default model from configuration
        return getChatCompletion(messages, openAIProperties.getModel());
    }
    */
    
    /**
     * Gets a completion from the LLM for a conversation history synchronously.
     * <p>
     * This method processes a list of chat messages and returns the complete response
     * as a single string. It uses the default model specified in the configuration.
     *
     * @param messages The list of chat messages representing the conversation history
     * @return The LLM's response as text
     */
    public String getChatCompletion(List<ChatMessage> messages) {
        return getChatCompletion(messages, openAIProperties.getModel());
    }

    /**
     * Gets a completion from the LLM for a conversation history with a specified model.
     * <p>
     * This method processes a list of chat messages using the specified LLM model and
     * returns the complete response as a single string.
     *
     * @param messages The list of chat messages representing the conversation history
     * @param modelId The ID of the LLM model to use
     * @return The LLM's response as text
     */
    public String getChatCompletion(List<ChatMessage> messages, String modelId) {
        logger.info("Getting completion for model: {}", modelId);
        
        List<Map<String, Object>> messagesForLlm = prepareMessagesForLlm(messages);
        
        if (messagesForLlm.isEmpty() || (messagesForLlm.size() == 1 && "system".equals(messagesForLlm.get(0).get("role")) && messages.isEmpty())) {
            logger.warn("Cannot send an effectively empty message list to LLM.");
            throw new IllegalArgumentException("Cannot send an effectively empty message list to LLM.");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", messagesForLlm);
        
        // Add discovered MCP tools to request when available
        List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        if (mcpTools != null && !mcpTools.isEmpty()) {
            requestBody.put("tools", convertMcpToolsToOpenAIFormat(mcpTools));
        }

        try {
            // Make the API call
            String responseJson = webClient.post()
                               .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse the response to extract the generated text
            JsonNode responseNode = objectMapper.readTree(responseJson);
            String generatedText = responseNode
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
            
            return generatedText;
               } catch (Exception e) {
            logger.error("Error getting chat completion", e);
            return "Error processing request: " + e.getMessage();
        }
    }

    /**
     * Gets a non-streaming text completion from the LLM for the given message.
     * <p>
     * This method sends a single user message to the LLM and returns the complete response
     * as a string. It's a simple, synchronous interface for basic text generation without
     * the complexity of managing conversation history or streaming responses.
     * <p>
     * This is useful for:
     * <ul>
     *   <li>Simple, one-off queries that don't require context</li>
     *   <li>System health checks and model capability testing</li>
     *   <li>Text generation where the complete response is needed at once</li>
         * </ul>
     * 
     * @param message The text message to send to the LLM
     * @return The complete text response from the LLM
     */
    public String getChatCompletion(String message) {
        return getChatCompletion(message, openAIProperties.getModel());
    }

    /**
     * Gets a non-streaming text completion from the LLM for the given message using a specific model.
     * <p>
     * This method allows specifying which LLM model to use for the completion,
     * providing more control over the generation process.
     * 
     * @param message The text message to send to the LLM
     * @param modelId The ID of the model to use for generating the completion
     * @return The complete text response from the LLM
     */
    public String getChatCompletion(String message, String modelId) {
        logger.info("Getting chat completion with model: {}", modelId);
        
        List<ChatMessage> messages = new ArrayList<>();
        
        // Add system message if available
        if (openAIProperties.getSystemRole() != null && !openAIProperties.getSystemRole().isEmpty()) {
            messages.add(new ChatMessage("system", "text/plain", openAIProperties.getSystemRole()));
        }
        
        // Add user message
        messages.add(new ChatMessage("user", "text/plain", message));
        
        // Prepare the request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", prepareMessagesForLlm(messages));
    
        try {
            // Make the API call
            String responseJson = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            // Parse the response to extract the generated text
            JsonNode responseNode = objectMapper.readTree(responseJson);
            String generatedText = responseNode
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
            
            return generatedText;
        } catch (Exception e) {
            logger.error("Error getting chat completion", e);
            return "Error: " + e.getMessage();
        }    }    /**
     * Converts multimodal content to a string representation for storage in ChatMessage.
     * <p>
     * This helper method converts a complex multimodal content object (which is typically
     * an array of maps containing text and image data) into a JSON string representation.
     * This is necessary because ChatMessage.content only accepts String values, but
     * multimodal content is structured as Object[] with maps.
     * <p>
     * The conversion preserves the structure of the multimodal content while making it
     * compatible with the ChatMessage model's string-only content field.
     * 
     * @param multimodalContent The complex multimodal content object to convert
     * @return A JSON string representation of the multimodal content
     */    public String convertMultimodalContentToString(Object multimodalContent) {
        if (multimodalContent == null) {
            return null;
        }
        
        try {
            // Use the ObjectMapper to convert the multimodal content to a JSON string
            return objectMapper.writeValueAsString(multimodalContent);
        } catch (Exception e) {
            logger.error("Error converting multimodal content to JSON string: {}", e.getMessage());
            
            // Better fallback handling for different types
            if (multimodalContent instanceof Object[]) {
                Object[] contentArray = (Object[]) multimodalContent;
                try {
                    // Try to convert array elements individually
                    List<Object> contentList = Arrays.asList(contentArray);
                    return objectMapper.writeValueAsString(contentList);
                } catch (Exception e2) {
                    logger.error("Failed to convert multimodal content array to JSON: {}", e2.getMessage());
                    return "[Multimodal content - conversion failed]";
                }
            } else if (multimodalContent instanceof String) {
                return (String) multimodalContent;
            } else {
                logger.warn("Using toString() fallback for multimodal content of type: {}", multimodalContent.getClass().getSimpleName());
                return "[Multimodal content of type: " + multimodalContent.getClass().getSimpleName() + "]";
            }
        }
    }

    /**
     * Extracts only text content from multimodal content, stripping out binary data.
     * This is used for storing a readable representation in the database and for history.
     */
    private String extractTextFromMultimodalContent(Object multimodalContent) {
        if (multimodalContent == null) {
            return null;
        }
        
        try {
            if (multimodalContent instanceof Object[]) {
                Object[] contentArray = (Object[]) multimodalContent;
                StringBuilder textBuilder = new StringBuilder();
                
                for (Object item : contentArray) {
                    if (item instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) item;
                        String type = (String) itemMap.get("type");
                        
                        if ("text".equals(type)) {
                            Object text = itemMap.get("text");
                            if (text != null) {
                                if (textBuilder.length() > 0) {
                                    textBuilder.append(" ");
                                }
                                textBuilder.append(text.toString());
                            }
                        } else if ("image_url".equals(type)) {
                            // Replace image with a placeholder
                            if (textBuilder.length() > 0) {
                                textBuilder.append(" ");
                            }
                            textBuilder.append("[Image content omitted from history]");
                        }
                    }
                }
                
                return textBuilder.toString();
            } else if (multimodalContent instanceof Map) {
                Map<?, ?> contentMap = (Map<?, ?>) multimodalContent;
                if (contentMap.containsKey("text")) {
                    return contentMap.get("text").toString();
                }
            }
            
            // Fallback for any other structure
            return "[Multimodal content: " + multimodalContent.getClass().getSimpleName() + "]";
        } catch (Exception e) {
            logger.warn("Error extracting text from multimodal content: {}", e.getMessage());
            return "[Multimodal content extraction failed]";
        }
    }
    
    /**
     * Creates a token-efficient version of multimodal content for history storage.
     * This strips out large binary content (like base64-encoded images) while preserving text.
     */
    public Object createHistoryFriendlyMultimodalContent(Object multimodalContent) {
        if (multimodalContent == null) {
            return null;
        }
        
        try {
            if (multimodalContent instanceof Object[]) {
                Object[] contentArray = (Object[]) multimodalContent;
                List<Object> historyContent = new ArrayList<>();
                
                for (Object item : contentArray) {
                    if (item instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) item;
                        String type = (String) itemMap.get("type");
                        
                        if ("text".equals(type)) {
                            // Keep text content as-is
                            historyContent.add(item);
                        } else if ("image_url".equals(type)) {
                            // Replace image content with a placeholder
                            Map<String, Object> placeholder = new HashMap<>();
                            placeholder.put("type", "text");
                            placeholder.put("text", "[Image content omitted from history to save tokens]");
                            historyContent.add(placeholder);
                        }
                    }
                }
                
                return historyContent.toArray();
            } else if (multimodalContent instanceof List) {
                List<?> contentList = (List<?>) multimodalContent;
                List<Object> historyContent = new ArrayList<>();
                
                for (Object item : contentList) {
                    if (item instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) item;
                        String type = (String) itemMap.get("type");
                        
                        if ("text".equals(type)) {
                            historyContent.add(item);
                        } else if ("image_url".equals(type)) {
                            Map<String, Object> placeholder = new HashMap<>();
                            placeholder.put("type", "text");
                            placeholder.put("text", "[Image content omitted from history to save tokens]");
                            historyContent.add(placeholder);
                        }
                    }
                }
                
                return historyContent;
            }
            
            // For other types, return as-is (likely text-only)
            return multimodalContent;
        } catch (Exception e) {
            logger.warn("Error creating history-friendly multimodal content: {}", e.getMessage());
            // Return text extraction as fallback
            return extractTextFromMultimodalContent(multimodalContent);
        }    }

    /**
     * Checks if the given content is a history-friendly (stripped) version of multimodal content.
     * This helps determine whether to use the content as-is or restore full multimodal data.
     * 
     * @param content The content to check
     * @return true if this appears to be stripped content for history
     */
    private boolean isHistoryFriendlyContent(Object content) {
        if (content instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> contentList = (List<Object>) content;
            
            for (Object item : contentList) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    Object text = itemMap.get("text");
                    if (text instanceof String) {
                        String textStr = (String) text;
                        if (textStr.contains("[Image content omitted") || 
                            textStr.contains("[Multimodal content omitted")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
