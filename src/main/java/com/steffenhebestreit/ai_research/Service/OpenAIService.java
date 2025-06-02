package com.steffenhebestreit.ai_research.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
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

    public OpenAIService(OpenAIProperties openAIProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.openAIProperties = openAIProperties;
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

    public Flux<String> getChatCompletionStream(List<com.steffenhebestreit.ai_research.Model.ChatMessage> conversationMessages) {
        List<Map<String, Object>> messagesForLlm = new ArrayList<>();
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

        if (messagesForLlm.isEmpty()) {
            logger.warn("Conversation messages list is empty. Cannot make request to LLM.");
            return Flux.error(new IllegalArgumentException("Cannot send an empty message list to LLM."));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openAIProperties.getModel());
        requestBody.put("messages", messagesForLlm); // Use the full conversation history
        requestBody.put("stream", true);

        String path = "/chat/completions";

        logger.info("Sending streaming request to LLM: {} with model: {} and {} messages.", openAIProperties.getBaseurl() + path, openAIProperties.getModel(), messagesForLlm.size());
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

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("content", userMessage);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", openAIProperties.getModel());
        requestBodyMap.put("messages", Collections.singletonList(messageMap));
        requestBodyMap.put("max_tokens", 150);

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

    @SuppressWarnings("unchecked")
    public String summarizeMessage(String content) {
        // Construct a prompt for summarization
        String prompt = "Please summarize the following text for context in a longer conversation. Keep it concise, ideally under 50 words, capturing the main points:\n\n" + content;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAIProperties.getKey());

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("content", prompt);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", openAIProperties.getModel());
        requestBodyMap.put("messages", Collections.singletonList(messageMap));
        requestBodyMap.put("max_tokens", 75);
        requestBodyMap.put("temperature", 0.5);

        org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBodyMap, headers);
        String fullUrl = openAIProperties.getBaseurl() + "/chat/completions";
        RestTemplate restTemplate = new RestTemplate();

        try {
            logger.info("Sending summarization request to LLM: {} with model: {}", fullUrl, openAIProperties.getModel());
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(fullUrl, entity, (Class<Map<String,Object>>)(Class<?>)Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) { // Check if responseBody is not null
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, String> messageContent = (Map<String, String>) firstChoice.get("message");
                        if (messageContent != null && messageContent.get("content") != null) {
                            return messageContent.get("content").trim();
                        }
                    }
                } else {
                     logger.warn("LLM API (summarization) returned successful status but null body.");
                }
            } else {
                logger.error("Error from LLM API (summarization): {} - {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Exception while calling LLM API (summarization)", e);
        }
        return null; // Return null if summarization fails for any reason
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
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", multimodalContent);
        messages.add(userMessage);
        requestBody.put("messages", messages);
        
        // For safety, set a reasonable maximum token limit
        requestBody.put("max_tokens", 2000);
        
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
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", multimodalContent);
        messages.add(userMessage);
        requestBody.put("messages", messages);
        
        // For safety, set a reasonable maximum token limit
        requestBody.put("max_tokens", 2000);
        
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
}
