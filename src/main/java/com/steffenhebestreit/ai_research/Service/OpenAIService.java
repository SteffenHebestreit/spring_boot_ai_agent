package com.steffenhebestreit.ai_research.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import com.steffenhebestreit.ai_research.Repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList; // Added import
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    private final OpenAIProperties openAIProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository; // Inject ChatMessageRepository

    public OpenAIService(OpenAIProperties openAIProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper, ChatMessageRepository chatMessageRepository) {
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
        this.chatMessageRepository = chatMessageRepository; // Initialize ChatMessageRepository
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
}
