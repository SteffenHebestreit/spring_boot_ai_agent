package com.steffenhebestreit.ai_research.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Map;

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
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see LlmCapabilityService
 * @see MultimodalContentService
 */
@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private final OpenAIProperties openAIProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final DynamicIntegrationService dynamicIntegrationService;
    private final LlmCapabilityService llmCapabilityService;

    public OpenAIService(OpenAIProperties openAIProperties, WebClient.Builder webClientBuilder, 
                         ObjectMapper objectMapper, DynamicIntegrationService dynamicIntegrationService,
                         LlmCapabilityService llmCapabilityService) {
        this.openAIProperties = openAIProperties;
        this.dynamicIntegrationService = dynamicIntegrationService;
        this.llmCapabilityService = llmCapabilityService;
        // Ensure the base URL does not end with a slash if the request URIs start with a slash
        String baseUrl = openAIProperties.getBaseurl();
        if (baseUrl == null) {
            baseUrl = "http://localhost:1234"; // Default fallback if null
            logger.warn("Base URL is null, using default fallback: {}", baseUrl);
        } else if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    private List<Map<String, Object>> prepareMessagesForLlm(List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages) {
        List<Map<String, Object>> messagesForLlm = new ArrayList<>();

        // Prepend the system role message
        String systemRole = openAIProperties.getSystemRole();
        if (systemRole != null && !systemRole.isEmpty()) {
            Map<String, Object> systemMessageMap = new HashMap<>();
            systemMessageMap.put("role", "system");
            // Append current time to the system role
            String timeAppendedSystemRole = systemRole + " Current time: " + java.time.Instant.now().toString() + ".";
            systemMessageMap.put("content", timeAppendedSystemRole);
            messagesForLlm.add(systemMessageMap);
            logger.debug("Prepended system message to LLM request: {}", 
                timeAppendedSystemRole.substring(0, Math.min(100, timeAppendedSystemRole.length())) + "...");
        }

        if (conversationMessages != null) {
            for (com.steffenhebestreit.ai_research.Model.ChatMessage msg : conversationMessages) {
                Map<String, Object> llmMessage = new HashMap<>();
                String role = msg.getRole();
                if ("agent".equalsIgnoreCase(role)) {
                    role = "assistant"; // Map internal 'agent' role to 'assistant' for LLM
                }
                llmMessage.put("role", role);
                llmMessage.put("content", msg.getContent());
                messagesForLlm.add(llmMessage);
            }
        }
        return messagesForLlm;
    }

    public Flux<String> getChatCompletionStream(List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages) {
        return getChatCompletionStream(conversationMessages, openAIProperties.getModel());
    }
    
    public Flux<String> getChatCompletionStream(List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages, String modelId) {
        List<Map<String, Object>> messagesForLlm = prepareMessagesForLlm(conversationMessages);

        if (messagesForLlm.isEmpty() || (messagesForLlm.size() == 1 && "system".equals(messagesForLlm.get(0).get("role")) && conversationMessages.isEmpty())) {
            logger.warn("Conversation messages list is effectively empty (only system message or completely empty). Cannot make request to LLM.");
            return Flux.error(new IllegalArgumentException("Cannot send an effectively empty message list to LLM."));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", messagesForLlm); // Use the full conversation history
        requestBody.put("stream", true);
        
        // Add discovered MCP tools to request
        List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        if (mcpTools != null && !mcpTools.isEmpty()) {
            List<Map<String, Object>> formattedTools = convertMcpToolsToOpenAIFormat(mcpTools);
            logger.info("Adding {} MCP tools to LLM request", formattedTools.size());
            requestBody.put("tools", formattedTools);
            
            // Log first few tools for debugging if available
            if (logger.isDebugEnabled() && formattedTools.size() > 0) {
                int toolsToLog = Math.min(2, formattedTools.size());
                for (int i = 0; i < toolsToLog; i++) {
                    Map<String, Object> tool = formattedTools.get(i);
                    Object type = tool.get("type");
                    Object name = "unknown";
                    
                    // Safely extract function name if available
                    if (tool.get("function") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> functionMap = (Map<String, Object>) tool.get("function");
                        name = functionMap.getOrDefault("name", "unnamed");
                    }
                    
                    logger.debug("Tool #{}: Type: {}, Name: {}", i+1, type, name);
                }
            }
        } else {
            logger.debug("No MCP tools available to add to LLM request");
        }

        String path = "/chat/completions";

        logger.info("Sending streaming request to LLM: {} with model: {} and {} messages.", openAIProperties.getBaseurl() + path, modelId, messagesForLlm.size());
        if (messagesForLlm.size() <= 2) { // Log first few messages for debugging if history is short
            messagesForLlm.forEach(m -> logger.debug("Message to LLM: Role: {}, Content: {}", m.get("role"), ((String)m.get("content")).substring(0, Math.min(100, ((String)m.get("content")).length()))));
        }


        return webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(rawEvent -> logger.info("Raw event from LLM: {}", rawEvent))
                .map(String::trim)
                .filter(trimmedEvent -> !"[DONE]".equalsIgnoreCase(trimmedEvent))
                .map(jsonChunk -> {
                    try {
                        // Handle the [DONE] marker
                        if ("[DONE]".equalsIgnoreCase(jsonChunk.trim())) {
                            return "";
                        }
                        
                        JsonNode rootNode = objectMapper.readTree(jsonChunk);
                        JsonNode choicesNode = rootNode.path("choices");
                        if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                            JsonNode firstChoice = choicesNode.get(0);
                            JsonNode deltaNode = firstChoice.path("delta");
                            JsonNode contentNode = deltaNode.path("content");
                            if (contentNode.isTextual()) {
                                // Just return the raw content
                                return contentNode.asText();
                            }
                        }
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing JSON chunk from LLM stream: '{}'", jsonChunk, e);
                    }
                    return "";
                })
                .filter(content -> content != null && !content.isEmpty())
                .doOnError(error -> {
                    logger.error("Error during LLM stream processing or request. Error Type: {}", error.getClass().getName());
                    // Log the full stack trace for the error
                    logger.error("Full Error Details: ", error); 
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException wcre = (org.springframework.web.reactive.function.client.WebClientResponseException) error;
                        logger.error("LLM API Error: Status Code: {}, Headers: {}, Response Body: '{}'",
                                wcre.getStatusCode(), wcre.getHeaders(), wcre.getResponseBodyAsString());
                    }
                })
                .doOnComplete(() -> logger.info("LLM stream completed."));
    }

    @SuppressWarnings("unchecked")
    public String getChatCompletion(String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAIProperties.getKey());

        // Create a dummy ChatMessage list to reuse prepareMessagesForLlm
        com.steffenhebestreit.ai_research.Model.ChatMessage userChatMessage = new com.steffenhebestreit.ai_research.Model.ChatMessage();
        userChatMessage.setRole("user");
        userChatMessage.setContent(userMessage);
        List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages = Collections.singletonList(userChatMessage);
        
        List<Map<String, Object>> messagesForLlm = prepareMessagesForLlm(conversationMessages);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", openAIProperties.getModel());
        requestBodyMap.put("messages", messagesForLlm); // Use the prepared messages
        requestBodyMap.put("max_tokens", 150);
        
        // Add discovered MCP tools to request
        List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        if (mcpTools != null && !mcpTools.isEmpty()) {
            List<Map<String, Object>> formattedTools = convertMcpToolsToOpenAIFormat(mcpTools);
            logger.info("Adding {} MCP tools to non-streaming LLM request", formattedTools.size());
            requestBodyMap.put("tools", formattedTools);
        }

        org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBodyMap, headers);
        String fullUrl = openAIProperties.getBaseurl() + "/chat/completions";
        RestTemplate restTemplate = new RestTemplate();

        try {
            logger.info("Sending non-streaming request to LLM: {} with model: {}", fullUrl, openAIProperties.getModel());
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(fullUrl, entity, (Class<Map<String,Object>>)(Class<?>)Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, String> messageContent = (Map<String, String>) firstChoice.get("message");
                        if (messageContent != null) {
                            return messageContent.get("content");
                        }
                    }
                }
            } else {
                logger.error("Error from LLM API (non-streaming): {} - {}", response.getStatusCode(), response.getBody());
                return "Error: Could not get a response from AI.";
            }
        } catch (Exception e) {
            logger.error("Exception while calling LLM API (non-streaming)", e);
            return "Error: Exception occurred while contacting AI service.";
        }
        return "Error: No response from AI.";
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
     */
    public String getMultimodalCompletion(Object multimodalContent, String llmId) {
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
        
        // Add discovered MCP tools to request
        List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        if (mcpTools != null && !mcpTools.isEmpty()) {
            List<Map<String, Object>> formattedTools = convertMcpToolsToOpenAIFormat(mcpTools);
            logger.info("Adding {} MCP tools to multimodal LLM request", formattedTools.size());
            requestBody.put("tools", formattedTools);
        }

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
     */
    public Flux<String> getMultimodalCompletionStream(Object multimodalContent, String modelId) {
        logger.info("Processing streaming multimodal content with model: {}", modelId);
        
        // Prepare the request body for the OpenAI API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("stream", true);
        
        // Convert the multimodal content to the format expected by the API
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Add system message only if model supports multimodal content and content is actually multimodal
        Map<String, Object> systemMessage = createMultimodalSystemMessage(modelId, multimodalContent);
        if (systemMessage != null) {
            messages.add(systemMessage);
            logger.debug("Added multimodal system message for streaming model: {}", modelId);
        }
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", multimodalContent);
        messages.add(userMessage);
        requestBody.put("messages", messages);
        
        // For safety, set a reasonable maximum token limit
        requestBody.put("max_tokens", 2000);
        
        // Add discovered MCP tools to request
        List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        if (mcpTools != null && !mcpTools.isEmpty()) {
            List<Map<String, Object>> formattedTools = convertMcpToolsToOpenAIFormat(mcpTools);
            logger.info("Adding {} MCP tools to streaming multimodal LLM request", formattedTools.size());
            requestBody.put("tools", formattedTools);
        }

        try {
            // Convert request body to JSON string for logging
            String requestJson = objectMapper.writeValueAsString(requestBody);
            logger.debug("Multimodal stream request: {}", requestJson);
            
            // Make the streaming API call
            return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractContentFromStreamResponse)
                .filter(content -> content != null && !content.isEmpty());
        } catch (Exception e) {
            logger.error("Error in multimodal streaming", e);
            return Flux.error(new RuntimeException("Error processing multimodal streaming content: " + e.getMessage()));
        }
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
    private String extractContentFromStreamResponse(String jsonChunk) {
        try {
            if (jsonChunk.startsWith("data: ")) {
                jsonChunk = jsonChunk.substring(6);
            }
            if ("[DONE]".equals(jsonChunk)) {
                return "";
            }
            
            JsonNode jsonNode = objectMapper.readTree(jsonChunk);
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray() && 
                jsonNode.get("choices").size() > 0) {
                
                JsonNode firstChoice = jsonNode.get("choices").get(0);
                if (firstChoice.has("delta") && firstChoice.get("delta").has("content")) {
                    return firstChoice.get("delta").get("content").asText();
                }
            }
        } catch (JsonProcessingException e) {
            logger.warn("Error parsing LLM API stream chunk: {}", e.getMessage());
        }
        return "";
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
                    return "You are a vision-enabled AI assistant. When provided with images, focus on analyzing and describing the visual content directly. Provide detailed descriptions of what you can see in the image, including objects, people, text, scenes, colors, and any other relevant visual details. Only use tools if the user explicitly requests web crawling or additional information beyond what's visible in the image.";
                }
                break;
                
            case "pdf":
                if (llmConfig.isSupportsPdf()) {
                    return "You are an AI assistant capable of analyzing PDF documents. When provided with PDF content, focus on understanding and analyzing the document structure, text content, and any visual elements within the PDF. Provide comprehensive analysis of the document content. Only use tools if the user explicitly requests web crawling or additional information beyond what's in the document.";
                }
                break;
                
            case "mixed":
                if (llmConfig.isSupportsImage() || llmConfig.isSupportsPdf()) {
                    return "You are a multimodal AI assistant capable of analyzing both images and documents. When provided with visual or document content, focus on analyzing and describing the content directly. Provide detailed analysis of all provided materials. Only use tools if the user explicitly requests web crawling or additional information beyond what's provided in the content.";
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
     */
    private List<Map<String, Object>> convertMcpToolsToOpenAIFormat(List<Map<String, Object>> mcpTools) {
        if (mcpTools == null || mcpTools.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Map<String, Object>> openAiTools = new ArrayList<>();
        
        for (Map<String, Object> mcpTool : mcpTools) {
            Map<String, Object> openAiTool = new HashMap<>();
            
            // Ensure the tool has the required "type": "function" field
            openAiTool.put("type", "function");
            
            // If the MCP tool already has a function field, use it
            if (mcpTool.containsKey("function")) {
                openAiTool.put("function", mcpTool.get("function"));
            } else {
                // Create a function structure from the MCP tool properties
                Map<String, Object> function = new HashMap<>();
                
                // Use the tool name as function name, or a default if not available
                function.put("name", mcpTool.getOrDefault("name", 
                        mcpTool.getOrDefault("id", "mcp_tool_" + openAiTools.size())));
                
                // Add description if available
                if (mcpTool.containsKey("description")) {
                    function.put("description", mcpTool.get("description"));
                }
                
                // Add parameters if available
                if (mcpTool.containsKey("parameters")) {
                    function.put("parameters", mcpTool.get("parameters"));
                }
                
                openAiTool.put("function", function);
            }
            
            openAiTools.add(openAiTool);
        }
        
        return openAiTools;
    }
    
    /**
     * Enhanced streaming method that handles tool calls and execution.
     * 
     * <p>This method extends the basic streaming functionality to support the complete
     * tool calling workflow: detecting tool calls from LLM responses, executing them
     * via MCP servers, and feeding results back to continue the conversation.</p>
     * 
     * @param conversationMessages The conversation history
     * @param modelId The model to use for completions
     * @return A Flux of strings containing the complete conversation including tool execution results
     */
    public Flux<String> getChatCompletionStreamWithToolExecution(
            List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages, String modelId) {
        
        return Flux.create(sink -> {
            List<com.steffenhebestreit.ai_research.Model.ChatMessage> workingMessages = new ArrayList<>(conversationMessages);
            executeStreamingConversationWithTools(workingMessages, modelId, sink);
        }, reactor.core.publisher.FluxSink.OverflowStrategy.BUFFER);
    }
    
    /**
     * Enhanced streaming method with tool execution using default model.
     * 
     * <p>Convenience method that uses the default model configured in properties.
     * Equivalent to calling getChatCompletionStreamWithToolExecution(conversationMessages, defaultModel).</p>
     * 
     * @param conversationMessages The conversation history
     * @return A Flux of strings containing the complete conversation including tool execution results
     */
    public Flux<String> getChatCompletionStreamWithToolExecution(
            List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages) {
        return getChatCompletionStreamWithToolExecution(conversationMessages, openAIProperties.getModel());
    }

    /**
     * Executes a complete streaming conversation with tool support.
     * 
     * @param messages The current conversation messages
     * @param modelId The model to use
     * @param sink The flux sink to emit results to
     */
    private void executeStreamingConversationWithTools(
            List<com.steffenhebestreit.ai_research.Model.ChatMessage> messages, 
            String modelId, 
            reactor.core.publisher.FluxSink<String> sink) {
        
        // Track whether tool calls were detected to manage completion properly
        final java.util.concurrent.atomic.AtomicBoolean toolCallsDetected = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicBoolean streamCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicBoolean toolExecutionInProgress = new java.util.concurrent.atomic.AtomicBoolean(false);
        
        // Build messages for LLM using the helper method
        List<Map<String, Object>> messagesForLlm = prepareMessagesForLlm(messages);

        if (messagesForLlm.isEmpty() || (messagesForLlm.size() == 1 && "system".equals(messagesForLlm.get(0).get("role")) && messages.isEmpty())) {
            sink.error(new IllegalArgumentException("Cannot send an effectively empty message list to LLM."));
            return;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", messagesForLlm);
        requestBody.put("stream", true);
        
        // Add MCP tools
        List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        if (mcpTools != null && !mcpTools.isEmpty()) {
            List<Map<String, Object>> formattedTools = convertMcpToolsToOpenAIFormat(mcpTools);
            logger.info("Adding {} MCP tools to LLM request for tool execution stream", formattedTools.size());
            requestBody.put("tools", formattedTools);
        }

        String path = "/chat/completions";

        webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(rawEvent -> {
                    logger.debug("Raw streaming event: {}", rawEvent);
                    // Also log if it contains tool_calls for easier debugging
                    if (rawEvent.contains("tool_calls")) {
                        logger.info("Raw event contains tool_calls: {}", rawEvent);
                    }
                })
                .map(String::trim)
                .filter(trimmedEvent -> !"[DONE]".equalsIgnoreCase(trimmedEvent))
                .subscribe(
                    jsonChunk -> {
                        try {
                            if ("[DONE]".equalsIgnoreCase(jsonChunk.trim())) {
                                return;
                            }
                            
                            JsonNode rootNode = objectMapper.readTree(jsonChunk);
                            JsonNode choicesNode = rootNode.path("choices");
                            if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                                JsonNode firstChoice = choicesNode.get(0);
                                JsonNode deltaNode = firstChoice.path("delta");
                                JsonNode finishReasonNode = firstChoice.path("finish_reason");
                                
                                // Check for tool calls in delta (for name detection)
                                JsonNode toolCallsNode = deltaNode.path("tool_calls");
                                if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                                    logger.info("Found tool calls in streaming delta: {}", toolCallsNode.toString());
                                    for (JsonNode toolCallNode : toolCallsNode) {
                                        JsonNode functionNode = toolCallNode.path("function");
                                        if (!functionNode.isMissingNode()) {
                                            JsonNode nameNode = functionNode.path("name");
                                            if (nameNode.isTextual()) {
                                                String toolName = nameNode.asText();
                                                sink.next("[Calling tool: " + toolName + "] ");
                                                logger.info("Detected tool call in stream: {}", toolName);
                                            }
                                        }
                                    }
                                } else {
                                    // Regular content
                                    JsonNode contentNode = deltaNode.path("content");
                                    if (contentNode.isTextual()) {
                                        sink.next(contentNode.asText());
                                    }
                                }
                                
                                // Check finish reason
                                if (finishReasonNode.isTextual()) {
                                    String finishReason = finishReasonNode.asText();
                                    logger.info("Stream finished with reason: {}", finishReason);
                                    
                                    if ("tool_calls".equals(finishReason)) {
                                        // Set flag and handle tool calls
                                        toolCallsDetected.set(true);
                                        streamCompleted.set(true);
                                        
                                        // Prevent concurrent tool execution
                                        if (toolExecutionInProgress.compareAndSet(false, true)) {
                                            sink.next("[Executing tools...]");
                                            handleToolCallsCompletion(messages, modelId, sink, toolExecutionInProgress);
                                        } else {
                                            logger.warn("Tool execution already in progress, ignoring duplicate tool_calls finish reason");
                                        }
                                    } else if (!toolCallsDetected.get() && streamCompleted.compareAndSet(false, true)) {
                                        // Only complete if no tool calls were detected and not already completed
                                        logger.info("Completing sink after normal conversation");
                                        sink.complete();
                                    }
                                }
                            }
                        } catch (JsonProcessingException e) {
                            logger.error("Error parsing streaming JSON chunk: '{}'", jsonChunk, e);
                        } catch (Exception e) {
                            logger.error("Unexpected error processing streaming chunk: '{}'", jsonChunk, e);
                        }
                    },
                    error -> {
                        logger.error("Error in streaming response", error);
                        sink.error(error);
                    },
                    () -> {
                        // Only complete if no tool calls were detected and not already completed
                        if (!toolCallsDetected.get() && streamCompleted.compareAndSet(false, true)) {
                            logger.info("Streaming completed without tool calls");
                            sink.complete();
                        } else {
                            logger.debug("Streaming completed - tool calls handler will manage completion or already completed");
                        }
                    }
                );
    }    
    /**
     * Processes tool call delta from streaming response.
     */
    private void processToolCallDelta(JsonNode toolCallsNode, reactor.core.publisher.FluxSink<String> sink) {
        for (JsonNode toolCallNode : toolCallsNode) {
            JsonNode functionNode = toolCallNode.path("function");
            if (!functionNode.isMissingNode()) {
                JsonNode nameNode = functionNode.path("name");
                JsonNode argsNode = functionNode.path("arguments");
                
                if (nameNode.isTextual()) {
                    String toolName = nameNode.asText();
                    sink.next("\n[Calling tool: " + toolName + "]");
                }
                
                if (argsNode.isTextual()) {
                    // Tool arguments are being streamed
                    // For now, just indicate that tool call is in progress
                    // We'll collect the full arguments when the stream completes
                }
            }
        }
    }
    
    /**
     * Handles completion of tool calls and continues the conversation.
     */    private void handleToolCallsCompletion(
        List<com.steffenhebestreit.ai_research.Model.ChatMessage> messages, 
        String modelId, 
        reactor.core.publisher.FluxSink<String> sink,
        java.util.concurrent.atomic.AtomicBoolean toolExecutionInProgress) {
        
        // Use reactive approach to avoid blocking in reactive context
        getChatCompletionForToolCallsReactive(messages, modelId)
            .subscribe(
                toolCallsResponse -> {
                    try {
                        // Parse the response to extract tool calls
                        JsonNode responseNode = objectMapper.readTree(toolCallsResponse);
                        JsonNode choicesNode = responseNode.path("choices");
                        if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                            JsonNode firstChoice = choicesNode.get(0);
                            JsonNode messageNode = firstChoice.path("message");
                            JsonNode toolCallsNode = messageNode.path("tool_calls");
                            
                            if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                                // Track success/failure for logging
                                int totalTools = toolCallsNode.size();
                                int successCount = 0;
                                int failureCount = 0;
                                
                                // Execute each tool call independently
                                for (JsonNode toolCallNode : toolCallsNode) {
                                    try {
                                        boolean success = executeToolCallFromNode(toolCallNode, messages, sink);
                                        if (success) {
                                            successCount++;
                                        } else {
                                            failureCount++;
                                            logger.warn("Tool execution failed but continuing with other tools");
                                        }
                                    } catch (Exception e) {
                                        failureCount++;
                                        logger.error("Exception during tool execution but continuing with other tools", e);
                                        
                                        // Add error message to conversation for this specific tool
                                        try {
                                            String toolName = toolCallNode.path("function").path("name").asText("unknown-tool");
                                            String toolId = toolCallNode.path("id").asText("unknown-id");
                                              sink.next("\n[Tool '" + toolName + "' execution error: " + e.getMessage() + "]\n");
                                            
                                            // Add a tool message with the error for the LLM to understand what happened
                                            com.steffenhebestreit.ai_research.Model.ChatMessage errorMessage = 
                                                new com.steffenhebestreit.ai_research.Model.ChatMessage();
                                            errorMessage.setRole("tool");
                                            errorMessage.setToolCallId(toolId);
                                            
                                            // Create a more detailed error message with guidance
                                            StringBuilder errorContent = new StringBuilder();
                                            errorContent.append("Error executing tool '").append(toolName).append("' (ID: ").append(toolId).append("): ")
                                                      .append(e.getMessage());
                                            
                                            // Add helpful instructions for the LLM
                                            errorContent.append("\n\nPlease check your parameters and try again with a corrected tool call. ")
                                                      .append("Make sure all required parameters are provided and all parameter types match the expected format.");
                                            
                                            errorMessage.setContent(errorContent.toString());
                                            messages.add(errorMessage);
                                        } catch (Exception ex) {
                                            logger.error("Error creating tool error message", ex);
                                        }
                                    }
                                }
                                  // Log the summary of tool executions
                                logger.info("Tool execution summary: {} total, {} succeeded, {} failed", 
                                           totalTools, successCount, failureCount);
                                
                                // Continue conversation with tool results (whether successful or failed)
                                String continuationMessage = "\n[Continuing conversation with " + successCount + " successful and " + 
                                         failureCount + " failed tool results";
                                
                                // Add instructions for the LLM on how to handle failed tools
                                if (failureCount > 0) {
                                    continuationMessage += ". Please review the tool error messages and try again with corrected parameters if needed";
                                }
                                continuationMessage += "]\n";
                                
                                sink.next(continuationMessage);
                                
                                // Continue the conversation within the same reactive context
                                continueConversationAfterTools(messages, modelId, sink, toolExecutionInProgress);
                                return;
                            }
                        }
                        
                        // No tool calls found, complete the conversation with a small delay
                        logger.info("No tool calls found in response, completing stream with delay");
                        // Reset tool execution flag since we're completing
                        toolExecutionInProgress.set(false);
                        reactor.core.scheduler.Schedulers.single().schedule(() -> {
                            sink.complete();
                        }, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
                        
                    } catch (Exception e) {
                        logger.error("Error parsing tool calls response", e);
                        // Reset tool execution flag since we're completing with error
                        toolExecutionInProgress.set(false);
                        sink.error(e);
                    }
                },
                error -> {
                    logger.error("Error handling tool calls completion", error);
                    // Reset tool execution flag since we're completing with error
                    toolExecutionInProgress.set(false);
                    sink.error(error);
                }
            );
    }
      /**
     * Executes a single tool call and adds the result to the conversation.
     * @return true if execution was successful, false if it failed
     */    private boolean executeToolCallFromNode(JsonNode toolCallNode, 
                                List<com.steffenhebestreit.ai_research.Model.ChatMessage> messages,
                                reactor.core.publisher.FluxSink<String> sink) {        try {
            String toolCallId = toolCallNode.path("id").asText();
            JsonNode functionNode = toolCallNode.path("function");
            final String toolName = functionNode.path("name").asText();
            String argumentsJson = functionNode.path("arguments").asText();
            
            sink.next("\n[Executing: " + toolName + "]");
            logger.info("Executing tool call: {} with ID: {}", toolName, toolCallId);
            
            // Parse arguments
            final Map<String, Object> arguments = new HashMap<>();
            if (argumentsJson != null && !argumentsJson.trim().isEmpty()) {
                try {
                    arguments.putAll(objectMapper.readValue(argumentsJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
                } catch (JsonProcessingException e) {
                    logger.error("Error parsing tool arguments: {}", argumentsJson, e);
                    sink.next("\n[Error parsing tool arguments: " + e.getMessage() + "]");
                }
            }
            
            // Send status update for long-running tools
            sink.next("\n[Tool execution started - this may take some time...]");
            
            // Execute the tool via MCP with timeout handling
            CompletableFuture<Map<String, Object>> toolResultFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return dynamicIntegrationService.executeToolCall(toolName, arguments);
                } catch (Exception e) {
                    logger.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", e.getMessage());
                    return errorResult;
                }
            });
            
            // Wait for the tool execution with status updates for long-running operations
            Map<String, Object> toolResult = null;
            boolean isTimeout = false;
            
            try {
                // Send status updates every 5 seconds for long-running tools
                for (int i = 0; i < 60 && !toolResultFuture.isDone(); i++) { // Up to 5 minutes (60 * 5 seconds)
                    try {
                        toolResult = toolResultFuture.get(5, TimeUnit.SECONDS);
                        break; // If we get here, the future completed successfully
                    } catch (TimeoutException e) {
                        // Send updates every 5 seconds
                        if (i > 0 && i % 2 == 0) { // Every 10 seconds
                            sink.next("\n[Tool execution still in progress - please wait...]");
                            logger.info("Tool {} still executing after {} seconds", toolName, i * 5);
                        }
                    }
                }
                  // If still not done after loop, try one more time with a longer timeout
                if (toolResult == null) {
                    try {
                        toolResult = toolResultFuture.get(10, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        // Final timeout - but don't consider this a failure
                        isTimeout = true;
                        logger.warn("Tool execution for {} timed out after 5 minutes, but continues in the background", toolName);
                        
                        // Don't cancel the future - let it continue running in the background
                        // toolResultFuture.cancel(true);
                        
                        Map<String, Object> timeoutResult = new HashMap<>();
                        timeoutResult.put("status", "running");
                        timeoutResult.put("message", "Tool execution is taking longer than the maximum wait time of 5 minutes. The operation continues to run in the background and may complete successfully.");
                        toolResult = timeoutResult;
                        
                        sink.next("\n[Tool execution continues in background beyond maximum wait time]");
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error while waiting for tool execution: {}", e.getMessage(), e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Error waiting for tool execution: " + e.getMessage());
                toolResult = errorResult;
            }                if (toolResult == null) {
                toolResult = new HashMap<>();
                if (isTimeout) {
                    // Provide a more informative message for timeout case
                    toolResult.put("status", "running");
                    toolResult.put("message", "The tool execution is taking longer than the maximum wait time of 5 minutes. It continues to run in the background on the server and may complete successfully.");
                } else {
                    toolResult.put("error", "Unknown error executing tool - null result");
                }
            }
            
            // Add assistant message with tool call to conversation
            com.steffenhebestreit.ai_research.Model.ChatMessage assistantMessage = 
                new com.steffenhebestreit.ai_research.Model.ChatMessage();
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(null); // No text content for tool call message
            // We add tool call data via tool_calls field in a real implementation
            messages.add(assistantMessage);
            
            // Add tool result as a tool message
            com.steffenhebestreit.ai_research.Model.ChatMessage toolMessage = 
                new com.steffenhebestreit.ai_research.Model.ChatMessage();
            toolMessage.setRole("tool");
            toolMessage.setToolCallId(toolCallId); // Link this result to the specific tool call
              String resultContent;
            boolean toolSuccess = false;            
            if (toolResult != null) {
                if (toolResult.containsKey("error")) {
                    // Extract error details for better LLM feedback
                    String errorMsg = toolResult.get("error").toString();
                    sink.next("\n[Tool error: " + errorMsg + "]");
                    logger.error("Tool execution error: {}", errorMsg);
                    
                    // Structure the error message to help LLM learn from it
                    StringBuilder formattedError = new StringBuilder();
                    formattedError.append("Error executing tool '").append(toolName).append("': ").append(errorMsg);
                    
                    // Parse error for parameter validation issues to provide more helpful feedback
                    if (errorMsg.contains("Invalid parameters") || errorMsg.contains("is not allowed")) {
                        formattedError.append("\n\nThis appears to be a parameter validation error. ");
                        
                        // Add the arguments that were sent for context
                        formattedError.append("The arguments provided were: ");
                        try {
                            formattedError.append(objectMapper.writeValueAsString(arguments));
                        } catch (Exception e) {
                            formattedError.append(arguments.toString());
                        }
                        
                        // Try to extract parameter details from error message
                        if (toolResult.get("error") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> errorDetails = (Map<String, Object>) toolResult.get("error");
                            if (errorDetails.containsKey("data")) {
                                formattedError.append("\n\nDetails: ").append(errorDetails.get("data"));
                            }
                        }
                        
                        // Get parameter schema for the tool if possible
                        formattedError.append("\n\nPlease try again with valid parameters for this tool.");
                        
                        // Suggest corrective action based on error message
                        if (errorMsg.contains("is not allowed")) {
                            String param = extractParameterNameFromError(errorMsg);
                            if (param != null) {
                                formattedError.append("\n\nThe parameter '").append(param)
                                    .append("' is not valid for this tool. Please remove it and try again.");
                            }
                        }
                    }
                    
                    resultContent = formattedError.toString();
                    
                    // Consider partial results as partial success
                    toolSuccess = toolResult.containsKey("result");
                    
                    // Add more context to the error message if we have partial results
                    if (toolSuccess && toolResult.containsKey("result")) {
                        resultContent += "\n\nPartial results were obtained and may be useful: " + 
                                         objectMapper.writeValueAsString(toolResult.get("result"));
                    }
                } else if (toolResult.containsKey("status") && "running".equals(toolResult.get("status"))) {
                    // Handle the case of long-running tools
                    resultContent = toolResult.get("message").toString();
                    sink.next("\n[Tool execution continues in background]");
                    logger.info("Tool '{}' execution continues in background beyond the maximum wait time", toolName);
                    toolSuccess = true; // Consider this a success since the tool is running as expected
                } else if (toolResult.containsKey("result")) {
                    resultContent = objectMapper.writeValueAsString(toolResult.get("result"));
                    sink.next("\n[Tool completed successfully]");
                    logger.info("Tool executed successfully, result size: {} chars", resultContent.length());
                    toolSuccess = true;
                } else {
                    resultContent = objectMapper.writeValueAsString(toolResult);
                    sink.next("\n[Tool completed]");
                    logger.info("Tool completed, result size: {} chars", resultContent.length());
                    toolSuccess = true;
                }
            } else {
                resultContent = "Tool execution encountered an issue. The server returned no result. This might indicate a server-side error.";
                sink.next("\n[Tool execution issue: no result returned]");
                logger.error("Tool execution returned null result for tool: {}", toolName);
                toolSuccess = false;
            }
            
            toolMessage.setContent(resultContent);
            messages.add(toolMessage);
            
            logger.info("Added tool result to conversation for tool '{}'. Message count now: {}", toolName, messages.size());
            return toolSuccess;
              } catch (Exception e) {
            logger.error("Error executing tool call", e);
            sink.next("\n[Tool execution error: " + e.getMessage() + "]\n");
            
            // Extract tool information from node if possible for better error reporting
            String toolName = "unknown-tool";
            String toolId = "unknown-id";
            try {
                toolName = toolCallNode.path("function").path("name").asText("unknown-tool");
                toolId = toolCallNode.path("id").asText("unknown-id");
            } catch (Exception ex) {
                logger.error("Error extracting tool info for error message", ex);
            }
            
            // Add a tool message with the error for the LLM to understand what happened
            com.steffenhebestreit.ai_research.Model.ChatMessage errorMessage = 
                new com.steffenhebestreit.ai_research.Model.ChatMessage();
            errorMessage.setRole("tool");
            errorMessage.setToolCallId(toolId); // Link the error to the specific tool call ID
            
            // Create a more informative error message
            String errorContent = "Error executing tool '" + toolName + "' (ID: " + toolId + "): " + e.getMessage();
            
            // Add root cause if available
            if (e.getCause() != null) {
                errorContent += "\nRoot cause: " + e.getCause().getMessage();
            }
              // Add a user-friendly message to help understand the impact
            errorContent += "\n\nThe system encountered an error while executing this tool. " +
                           "Other tools may still have succeeded. Please consider the partial " +
                           "information available and adjust your approach accordingly.";
            
            // Add guidance for retrying with corrected parameters
            errorContent += "\n\nPlease try again with corrected parameters. Make sure that:\n" +
                           "1. You're only using parameters defined in the tool's schema\n" +
                           "2. Parameter types match what the tool expects\n" +
                           "3. Required parameters are provided\n" +
                           "4. Parameter values are within expected ranges or formats";
            
            errorMessage.setContent(errorContent);
            messages.add(errorMessage);
            
            return false;
        }
    }
    
    /**
     * Reactive version to get complete response including tool calls.
     */
    private reactor.core.publisher.Mono<String> getChatCompletionForToolCallsReactive(List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages, String modelId) {
        List<Map<String, Object>> messagesForLlm = new ArrayList<>();
        for (com.steffenhebestreit.ai_research.Model.ChatMessage msg : conversationMessages) {
            Map<String, Object> llmMessage = new HashMap<>();
            String role = msg.getRole();
            if ("agent".equalsIgnoreCase(role)) {
                role = "assistant";
            }
            llmMessage.put("role", role);
            llmMessage.put("content", msg.getContent());
            messagesForLlm.add(llmMessage);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", messagesForLlm);
        requestBody.put("stream", false);
        
        // Add MCP tools
        List<Map<String, Object>> mcpTools = dynamicIntegrationService.getDiscoveredMcpTools();
        if (mcpTools != null && !mcpTools.isEmpty()) {
            List<Map<String, Object>> formattedTools = convertMcpToolsToOpenAIFormat(mcpTools);
            requestBody.put("tools", formattedTools);
        }

        String path = "/chat/completions";
        
        return webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> logger.error("Error making reactive non-streaming completion request", e));
    }
    
    /**
     * Continues the conversation after tool execution within the same reactive context.
     */    private void continueConversationAfterTools(
        List<com.steffenhebestreit.ai_research.Model.ChatMessage> messages, 
        String modelId, 
        reactor.core.publisher.FluxSink<String> sink,
        java.util.concurrent.atomic.AtomicBoolean toolExecutionInProgress) {
        
        // Build messages for LLM including the tool results
        List<Map<String, Object>> messagesForLlm = new ArrayList<>();
        for (com.steffenhebestreit.ai_research.Model.ChatMessage msg : messages) {
            Map<String, Object> llmMessage = new HashMap<>();
            String role = msg.getRole();
            if ("agent".equalsIgnoreCase(role)) {
                role = "assistant";
            }
            llmMessage.put("role", role);
            
            // Handle content - ensure it's not null and reasonable
            String content = msg.getContent();
            if (content == null) {
                if ("assistant".equals(role)) {
                    // Skip assistant messages with null content as they likely had tool calls
                    // that we can't properly represent in this simplified format
                    logger.debug("Skipping assistant message with null content");
                    continue;
                } else {
                    content = "";
                }
            }
            
            // Limit content size for tool messages to prevent overwhelming the LLM
            if ("tool".equals(role) && content.length() > 5000) {
                content = content.substring(0, 5000) + "... [content truncated for length]";
                logger.debug("Truncated tool result content from {} to 5000 chars", content.length());
            }
            
            llmMessage.put("content", content);
            
            // Add tool_call_id for tool messages if available
            if ("tool".equals(role) && msg.getToolCallId() != null) {
                llmMessage.put("tool_call_id", msg.getToolCallId());
                logger.debug("Added tool_call_id to message: {}", msg.getToolCallId());
            }
            
            messagesForLlm.add(llmMessage);
        }
        
        logger.info("Built {} messages for continuation request (after filtering)", messagesForLlm.size());
        
        // Debug log the message structure
        if (logger.isDebugEnabled()) {
            for (int i = 0; i < messagesForLlm.size(); i++) {
                Map<String, Object> msg = messagesForLlm.get(i);
                String content = (String) msg.get("content");
                int contentLength = content != null ? content.length() : 0;
                logger.debug("Continuation message {}: role={}, content_length={}, tool_call_id={}", 
                    i, msg.get("role"), contentLength, msg.get("tool_call_id"));
            }
        }
        
        // Ensure we have valid messages for continuation
        if (messagesForLlm.isEmpty()) {
            logger.warn("No valid messages for continuation request after filtering. Completing stream.");
            sink.next("\n[Tool execution completed successfully]");
            sink.complete();
            return;
        }        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("messages", messagesForLlm);
        requestBody.put("stream", true);
        
        // DO NOT add tools to continuation requests to prevent recursive tool calls
        // Tools should only be available in the initial request, not in continuations
        logger.info("Continuation request without tools to prevent recursive tool execution");

        String path = "/chat/completions";

        // Track if stream has been completed to prevent multiple completion calls
        final java.util.concurrent.atomic.AtomicBoolean streamCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
        // Track the subscription so we can cancel it if needed
        final java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable> subscriptionRef = new java.util.concurrent.atomic.AtomicReference<>();
        
        // Make a new streaming request for the continuation and properly connect it to the sink
        reactor.core.Disposable subscription = webClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIProperties.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(rawEvent -> logger.debug("Raw continuation event: {}", rawEvent))
                .map(String::trim)
                .filter(trimmedEvent -> !"[DONE]".equalsIgnoreCase(trimmedEvent))
                .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(1))
                        .doBeforeRetry(retrySignal -> 
                            logger.warn("Retrying continuation request (attempt {}): {}", 
                                retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())))
                // Add delay before completion to ensure content is fully transmitted
                .doOnComplete(() -> {
                    logger.info("CONTINUATION: Raw stream completed, scheduling sink completion after delay");
                    // Use a scheduler to add a small delay before completing
                    reactor.core.scheduler.Schedulers.single().schedule(() -> {
                        if (streamCompleted.compareAndSet(false, true)) {
                            logger.info("CONTINUATION: Completing sink after stream completion delay");
                            // Reset tool execution flag only when actually completing the sink
                            toolExecutionInProgress.set(false);
                            sink.complete();
                        }
                    }, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
                })
                .subscribe(
                    jsonChunk -> {
                        try {
                            if ("[DONE]".equalsIgnoreCase(jsonChunk.trim())) {
                                logger.info("CONTINUATION: Received [DONE] marker");
                                return;
                            }
                            
                            logger.debug("CONTINUATION: Processing chunk: {}", jsonChunk);
                            
                            JsonNode rootNode = objectMapper.readTree(jsonChunk);
                            JsonNode choicesNode = rootNode.path("choices");
                            if (choicesNode.isArray() && !choicesNode.isEmpty()) {
                                JsonNode firstChoice = choicesNode.get(0);
                                JsonNode deltaNode = firstChoice.path("delta");
                                JsonNode finishReasonNode = firstChoice.path("finish_reason");
                                
                                logger.debug("CONTINUATION: Delta: {}, Finish reason: {}", 
                                    deltaNode.toString(), 
                                    finishReasonNode.isTextual() ? finishReasonNode.asText() : "null");
                                
                                // Check for tool calls in delta (potential recursive tool calls)
                                JsonNode toolCallsNode = deltaNode.path("tool_calls");
                                if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                                    // Process tool call data
                                    logger.info("CONTINUATION: Found recursive tool calls");
                                    processToolCallDelta(toolCallsNode, sink);
                                } else {
                                    // Regular content - this is what we want to stream to frontend
                                    JsonNode contentNode = deltaNode.path("content");
                                    if (contentNode.isTextual()) {
                                        String content = contentNode.asText();
                                        logger.info("CONTINUATION: Emitting content to frontend: '{}'", content);
                                        sink.next(content);
                                    } else {
                                        // Log when there's no content in the delta
                                        logger.debug("CONTINUATION: Delta has no content node. Delta: {}", deltaNode);
                                    }
                                }
                                
                                // Check finish reason - but don't complete immediately
                                if (finishReasonNode.isTextual()) {
                                    String finishReason = finishReasonNode.asText();
                                    logger.info("CONTINUATION: Stream finished with reason: '{}'", finishReason);
                                      if ("tool_calls".equals(finishReason)) {
                                        // Handle tool calls - but only if no tool execution is currently in progress
                                        logger.info("CONTINUATION: Tool calls detected in continuation stream");
                                        
                                        // Count existing tool calls to prevent infinite loops
                                        long toolCallCount = messages.stream()
                                            .filter(msg -> "tool".equals(msg.getRole()))
                                            .count();
                                              if (toolCallCount >= 5) {
                                            logger.warn("CONTINUATION: Maximum tool call limit reached ({}), preventing potential infinite loop", toolCallCount);
                                            if (streamCompleted.compareAndSet(false, true)) {
                                                reactor.core.Disposable currentSub = subscriptionRef.get();
                                                if (currentSub != null && !currentSub.isDisposed()) {
                                                    currentSub.dispose();
                                                }
                                                sink.next("\n[Maximum tool call limit reached. Completing conversation.]\n");
                                                sink.complete();
                                            }
                                            return;
                                        }
                                        
                                        // Only allow tool execution if none is currently in progress
                                        if (toolExecutionInProgress.compareAndSet(false, true)) {
                                            logger.info("CONTINUATION: Starting tool execution ({}/5 tool calls used)", toolCallCount + 1);
                                            if (streamCompleted.compareAndSet(false, true)) {
                                                // Cancel the current subscription to prevent completion handlers from running
                                                reactor.core.Disposable currentSub = subscriptionRef.get();
                                                if (currentSub != null && !currentSub.isDisposed()) {
                                                    currentSub.dispose();
                                                }
                                                handleToolCallsCompletion(messages, modelId, sink, toolExecutionInProgress);
                                            }
                                        } else {
                                            // Tool execution already in progress - ignore this tool call request
                                            logger.warn("CONTINUATION: Tool execution already in progress, ignoring duplicate tool call request");
                                            return;
                                        }
                                        return;
                                    }
                                    // For normal completion, let the doOnComplete handler manage it with delay
                                } else {
                                    logger.debug("CONTINUATION: No finish reason in this chunk");
                                }
                            }
                        } catch (JsonProcessingException e) {
                            logger.error("Error parsing continuation JSON chunk: '{}'", jsonChunk, e);
                            if (streamCompleted.compareAndSet(false, true)) {
                                sink.error(e);
                            }
                        } catch (Exception e) {
                            logger.error("Unexpected error processing continuation chunk: '{}'", jsonChunk, e);                            if (streamCompleted.compareAndSet(false, true)) {
                                sink.error(e);
                            }
                        }
                    },
                    error -> {
                        logger.error("Error in continuation streaming response after retries", error);
                        
                        if (streamCompleted.compareAndSet(false, true)) {
                            // Reset tool execution flag since we're completing with error
                            toolExecutionInProgress.set(false);
                            
                            // Provide a fallback response instead of failing completely
                            sink.next("\n\n[Tool execution completed successfully, but continuation response failed. ");
                            sink.next("Tool results were processed. ");
                            sink.next("Connection issue with LLM server: " + error.getMessage() + "]");
                            sink.complete();
                        }
                    }
                );
        
        // Store the subscription reference so we can cancel it if needed
        subscriptionRef.set(subscription);
    }
    
    /**
     * Extracts parameter name from error messages related to invalid parameters.
     * 
     * @param errorMessage The error message to parse
     * @return The parameter name if it can be extracted, null otherwise
     */
    private String extractParameterNameFromError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return null;
        }
        
        // Try to extract parameter name from common error patterns
        try {
            // Pattern: "X is not allowed" - commonly used by JSON Schema validation errors
            if (errorMessage.contains("is not allowed")) {
                int start = errorMessage.indexOf("\"");
                int end = errorMessage.lastIndexOf("\"");
                if (start >= 0 && end > start) {
                    return errorMessage.substring(start + 1, end);
                }
                
                // Try without quotes
                start = errorMessage.indexOf("[");
                end = errorMessage.indexOf("]");
                if (start >= 0 && end > start) {
                    return errorMessage.substring(start + 1, end);
                }
            }
            
            // Pattern: "Missing required parameter: X"
            if (errorMessage.contains("Missing required parameter")) {
                int colonIndex = errorMessage.indexOf(":");
                if (colonIndex >= 0 && colonIndex < errorMessage.length() - 1) {
                    return errorMessage.substring(colonIndex + 1).trim();
                }
            }
            
            // Pattern from JSON-RPC errors: "Invalid parameters for tool X, data={details=[...]}""
            if (errorMessage.contains("Invalid parameters for tool") && errorMessage.contains("details=")) {
                int detailsStart = errorMessage.indexOf("details=");
                if (detailsStart >= 0) {
                    String details = errorMessage.substring(detailsStart + 8); // "details=".length()
                    
                    // Extract parameter name from JSON-like structure [{"param":"X"}] or ["X is not allowed"]
                    int quotedParamStart = details.indexOf("\"");
                    if (quotedParamStart >= 0) {
                        int quotedParamEnd = details.indexOf("\"", quotedParamStart + 1);
                        if (quotedParamEnd > quotedParamStart) {
                            String potentialParam = details.substring(quotedParamStart + 1, quotedParamEnd);
                            // Check if this is a parameter name or a full message
                            if (!potentialParam.contains(" ")) { // Simple parameter name shouldn't contain spaces
                                return potentialParam;
                            } else if (potentialParam.contains("\" is not allowed")) {
                                return potentialParam.substring(0, potentialParam.indexOf("\" is not allowed"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting parameter name from error message: {}", e.getMessage());
        }
        
        return null;
    }

    // ...existing code...
}
