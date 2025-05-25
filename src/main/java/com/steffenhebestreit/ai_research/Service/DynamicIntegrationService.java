package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Configuration.A2aPeerConfig;
import com.steffenhebestreit.ai_research.Configuration.AuthConfig;
import com.steffenhebestreit.ai_research.Configuration.IntegrationProperties;
import com.steffenhebestreit.ai_research.Configuration.McpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DynamicIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicIntegrationService.class);

    private final IntegrationProperties integrationProperties;
    private final RestTemplate restTemplate;

    // Store discovered tools and capabilities
    private final List<Map<String, Object>> discoveredMcpTools = new ArrayList<>();
    private final List<Map<String, Object>> discoveredA2aSkills = new ArrayList<>();

    // Simple cache for Keycloak tokens (Key: clientId@realm@authServerUrl, Value: TokenWrapper)
    private final Map<String, TokenWrapper> keycloakTokenCache = new HashMap<>();

    private static class TokenWrapper {
        String accessToken;
        long expiryTimeMillis;

        TokenWrapper(String accessToken, long expiresInSeconds) {
            this.accessToken = accessToken;
            // Set expiry time slightly before actual expiry to account for request time
            this.expiryTimeMillis = System.currentTimeMillis() + (expiresInSeconds - 30) * 1000;
        }

        boolean isValid() {
            return System.currentTimeMillis() < expiryTimeMillis;
        }
    }

    public DynamicIntegrationService(IntegrationProperties integrationProperties, RestTemplateBuilder restTemplateBuilder) {
        this.integrationProperties = integrationProperties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 5 seconds connection timeout
        requestFactory.setReadTimeout(10000);   // 10 seconds read timeout

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> requestFactory) // Use a supplier for the request factory
                .build();
    }

    @PostConstruct
    public void initializeDynamicIntegrations() {
        logger.info("Initializing dynamic integrations...");
        refreshDynamicCapabilities();
    }

    // Could be scheduled to run periodically
    // @Scheduled(fixedRateString = "${agent.integrations.refresh-rate:3600000}") // e.g., every hour
    public void refreshDynamicCapabilities() {
        logger.info("Refreshing dynamic capabilities...");
        discoveredMcpTools.clear();
        discoveredA2aSkills.clear();
        // Optionally clear token cache or let tokens expire naturally
        // keycloakTokenCache.clear();

        // Fetch from MCP Servers
        if (integrationProperties.getMcpServers() != null) {
            for (McpServerConfig mcpServer : integrationProperties.getMcpServers()) {
                logger.info("Fetching tools from MCP server: {} at {}", mcpServer.getName(), mcpServer.getUrl());
                List<Map<String, Object>> tools = fetchToolsFromMcpServer(mcpServer);
                if (tools != null && !tools.isEmpty()) {
                    discoveredMcpTools.addAll(tools);
                    logger.info("Discovered {} tools from {}", tools.size(), mcpServer.getName());
                } else {
                    logger.info("No tools discovered or error fetching from MCP server: {}", mcpServer.getName());
                }
            }
        }

        // Fetch from A2A Peers
        if (integrationProperties.getA2aPeers() != null) {
            for (A2aPeerConfig peer : integrationProperties.getA2aPeers()) {
                logger.info("Fetching capabilities from A2A peer: {} at {}", peer.getName(), peer.getUrl());
                Map<String, Object> agentCard = fetchAgentCardFromA2aPeer(peer);
                if (agentCard != null && agentCard.containsKey("skills")) {
                    Object skillsObject = agentCard.get("skills");
                    if (skillsObject instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> skills = (List<Map<String, Object>>) skillsObject;
                        if (skills != null && !skills.isEmpty()) {
                            discoveredA2aSkills.addAll(skills);
                            logger.info("Discovered {} skills from {}", skills.size(), peer.getName());
                        } else {
                            logger.info("No skills discovered in agent card from A2A peer: {}", peer.getName());
                        }
                    } else {
                        logger.warn("Skills attribute from A2A peer {} is not a List.", peer.getName());
                    }
                } else {
                    logger.info("No agent card or no skills discovered from A2A peer: {}", peer.getName());
                }
            }
        }
        logger.info("Finished refreshing dynamic capabilities. Total MCP tools: {}, Total A2A skills: {}", discoveredMcpTools.size(), discoveredA2aSkills.size());
    }

    private List<Map<String, Object>> fetchToolsFromMcpServer(McpServerConfig mcpConfig) {
        String baseUrl = sanitizeUrl(mcpConfig.getUrl());
        if (baseUrl == null || baseUrl.isEmpty()) {
            logger.warn("URL for MCP server '{}' is null, empty, or invalid after sanitization. Skipping.", 
                        mcpConfig.getName() != null ? mcpConfig.getName() : "UNKNOWN_MCP_SERVER");
            return Collections.emptyList();
        }
        String url = baseUrl + (baseUrl.endsWith("/") ? "tools/list" : "/tools/list");
        logger.info("Attempting to fetch tools from MCP server: {} at URL: {}", mcpConfig.getName(), url);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("id", "1");
        requestBody.put("method", "tools/list");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Optional<String> token = getAuthToken(mcpConfig.getAuth(), mcpConfig.getName());
        token.ifPresent(headers::setBearerAuth);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestEntity, Map.class);

            if (response != null && response.containsKey("result")) {
                Object resultObj = response.get("result");
                if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    if (resultMap.containsKey("tools")) {
                        Object toolsObj = resultMap.get("tools");
                        if (toolsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> tools = (List<Map<String, Object>>) toolsObj;
                            for (Map<String, Object> tool : tools) {
                                tool.putIfAbsent("sourceMcpServerName", mcpConfig.getName());
                                tool.putIfAbsent("sourceMcpServerUrl", mcpConfig.getUrl());
                            }
                            return tools;
                        } else {
                            logger.warn("MCP 'tools' attribute in result from {} is not a List. Found: {}", mcpConfig.getName(), toolsObj != null ? toolsObj.getClass().getName() : "null");
                        }
                    } else {
                        logger.warn("MCP 'result' from {} does not contain 'tools' attribute.", mcpConfig.getName());
                    }
                } else {
                    logger.warn("MCP 'result' from {} is not a Map. Found: {}", mcpConfig.getName(), resultObj != null ? resultObj.getClass().getName() : "null");
                }
            } else {
                logger.warn("MCP response from {} is null or does not contain 'result'. Response: {}", mcpConfig.getName(), response);
            }
        } catch (RestClientException e) {
            logger.error("RestClientException while fetching tools from MCP server {}: {}. URL: {}", mcpConfig.getName(), e.getMessage(), url, e);
        } catch (ClassCastException e) {
            logger.error("ClassCastException while parsing response from MCP server {}: {}. URL: {}", mcpConfig.getName(), e.getMessage(), url, e);
        }
        return Collections.emptyList();
    }

    private Map<String, Object> fetchAgentCardFromA2aPeer(A2aPeerConfig peerConfig) {
        String baseUrl = sanitizeUrl(peerConfig.getUrl());
        if (baseUrl == null || baseUrl.isEmpty()) {
            logger.warn("URL for A2A peer '{}' is null, empty, or invalid after sanitization. Skipping.", 
                        peerConfig.getName() != null ? peerConfig.getName() : "UNKNOWN_A2A_PEER");
            return null;
        }
        String url = baseUrl + (baseUrl.endsWith("/") ? ".well-known/agent.json" : "/.well-known/agent.json");
        logger.info("Attempting to fetch agent card from A2A peer: {} at URL: {}", peerConfig.getName(), url);
        
        HttpHeaders headers = new HttpHeaders();
        
        Optional<String> token = getAuthToken(peerConfig.getAuth(), peerConfig.getName());
        token.ifPresent(headers::setBearerAuth);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers); // No body for GET

        try {
            @SuppressWarnings("unchecked")
            // Updated to use exchange for GET with headers
            Map<String, Object> agentCard = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, requestEntity, Map.class).getBody();
            if (agentCard != null) {
                agentCard.putIfAbsent("sourceA2aPeerName", peerConfig.getName());
                agentCard.putIfAbsent("sourceA2aPeerUrl", peerConfig.getUrl());
            }
            return agentCard;
        } catch (RestClientException e) {
            logger.error("RestClientException while fetching agent card from A2A peer {}: {}. URL: {}", peerConfig.getName(), e.getMessage(), url, e);
        } catch (ClassCastException e) {
            logger.error("ClassCastException while parsing agent card from A2A peer {}: {}. URL: {}", peerConfig.getName(), e.getMessage(), e);
        }
        return null;
    }

    public List<Map<String, Object>> getDiscoveredMcpTools() {
        return new ArrayList<>(discoveredMcpTools);
    }

    public List<Map<String, Object>> getDiscoveredA2aSkills() {
        return new ArrayList<>(discoveredA2aSkills);
    }

    private Optional<String> getAuthToken(AuthConfig authConfig, String serviceName) {
        if (authConfig == null || authConfig.getType() == null || "none".equalsIgnoreCase(authConfig.getType())) {
            logger.debug("No authentication configured for {}", serviceName);
            return Optional.empty();
        }

        switch (authConfig.getType().toLowerCase()) {
            case "bearer":
                if (authConfig.getToken() != null && !authConfig.getToken().isEmpty()) {
                    logger.debug("Using static Bearer token for {}", serviceName);
                    return Optional.of(authConfig.getToken());
                } else {
                    logger.warn("Static Bearer token configured for {} but token value is missing.", serviceName);
                    return Optional.empty();
                }
            case "keycloak_client_credentials":
                return getAccessTokenFromKeycloak(authConfig, serviceName);
            default:
                logger.warn("Unsupported auth type '{}' for {}. Only 'bearer', 'keycloak_client_credentials', or 'none' is currently supported.", 
                            authConfig.getType(), serviceName);
                return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked") // For raw Map usage from RestTemplate
    private Optional<String> getAccessTokenFromKeycloak(AuthConfig authConfig, String serviceName) {
        String cacheKey = authConfig.getClientId() + "@" + authConfig.getRealm() + "@" + authConfig.getAuthServerUrl();
        TokenWrapper cachedToken = keycloakTokenCache.get(cacheKey);
        if (cachedToken != null && cachedToken.isValid()) {
            logger.debug("Using cached Keycloak token for {}", serviceName);
            return Optional.of(cachedToken.accessToken);
        }

        logger.info("Fetching new Keycloak token for {} (client: {}, realm: {})", 
                    serviceName, authConfig.getClientId(), authConfig.getRealm());

        String tokenUrl = authConfig.getAuthServerUrl()
                + (authConfig.getAuthServerUrl().endsWith("/") ? "" : "/") 
                + "realms/" + authConfig.getRealm()
                + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", authConfig.getClientId());
        body.add("client_secret", authConfig.getClientSecret());
        body.add("grant_type", authConfig.getGrantType() != null ? authConfig.getGrantType() : "client_credentials");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // Using Map<String, Object> for better type safety with the response.
            Map<String, Object> response = restTemplate.postForObject(tokenUrl, requestEntity, Map.class);
            if (response != null && response.containsKey("access_token")) {
                String accessToken = (String) response.get("access_token");
                // Ensure expires_in is treated as a Number before casting to long.
                Object expiresInObj = response.getOrDefault("expires_in", 300); // Default 5 mins (300 seconds)
                long expiresIn = 300L;
                if (expiresInObj instanceof Number) {
                    expiresIn = ((Number) expiresInObj).longValue();
                }
                
                keycloakTokenCache.put(cacheKey, new TokenWrapper(accessToken, expiresIn));
                logger.info("Successfully fetched Keycloak token for {}", serviceName);
                return Optional.of(accessToken);
            } else {
                logger.error("Failed to obtain Keycloak access token for {}. Response did not contain 'access_token'. Response: {}", serviceName, response);
                return Optional.empty();
            }
        } catch (RestClientException e) {
            logger.error("RestClientException while fetching Keycloak token for {}: {}. URL: {}", serviceName, e.getMessage(), tokenUrl, e);
            return Optional.empty();
        } catch (ClassCastException e) {
            logger.error("ClassCastException while parsing Keycloak token response for {}: {}", serviceName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String sanitizeUrl(String urlString) {
        if (urlString == null) {
            return null;
        }
        int commentIndex = urlString.indexOf('#');
        if (commentIndex != -1) {
            return urlString.substring(0, commentIndex).trim();
        }
        return urlString.trim();
    }
}
