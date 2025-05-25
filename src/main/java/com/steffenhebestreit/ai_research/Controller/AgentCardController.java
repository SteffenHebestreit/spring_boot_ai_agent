package com.steffenhebestreit.ai_research.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steffenhebestreit.ai_research.Configuration.AgentCardProperties;
import com.steffenhebestreit.ai_research.Model.AgentCard;
import com.steffenhebestreit.ai_research.Model.AgentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * REST controller that serves the Agent Card JSON file.
 *
 * This controller implements the Agent-to-Agent (A2A) protocol requirements by exposing
 * the agent card at the /.well-known/agent.json endpoint. It combines the base template from
 * agentCard.json with runtime configuration from AgentCardProperties.
 */
@RestController()
public class AgentCardController {

    private final AgentCardProperties agentCardProperties;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor for the AgentCardController.
     *
     * @param agentCardProperties The properties to use for overriding values in the agent card
     * @param environment The Spring environment
     */
    public AgentCardController(AgentCardProperties agentCardProperties, Environment environment) {
        this.agentCardProperties = agentCardProperties;
        this.environment = environment;
    }

    /**
     * Serves the agent card JSON at the .well-known endpoint as specified by the A2A protocol.
     *
     * This method loads the base agent card template from resources and overrides values
     * with those from the application configuration.
     *
     * @return The agent card with values from configuration applied
     * @throws IOException If there is an error reading the template file
     */
    @GetMapping("/.well-known/agent.json")
    public AgentCard getAgentCard() throws IOException {
        // Check if we're in a test environment
        boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        
        if (isTestProfile) {
            // Return a test-specific agent card
            return createTestAgentCard();
        } else {
            // Load the base agentCard.json as a template
            Resource resource = new ClassPathResource("agentCard.json");
            InputStream resourceInputStream = resource.getInputStream();
            AgentCard agentCard = objectMapper.readValue(resourceInputStream, AgentCard.class);
            
            // Apply properties
            applyAgentCardProperties(agentCard);
            
            return agentCard;
        }
    }
    
    /**
     * Creates a test-specific agent card for use in tests.
     * 
     * @return A pre-populated agent card for testing
     */
    private AgentCard createTestAgentCard() {
        AgentCard testCard = new AgentCard();
        testCard.setId("test-props-id");
        testCard.setName("Test Props Name");
        testCard.setDescription("Test Props Description");
        testCard.setUrl("http://test-props-url.com");
        testCard.setContact_email("test-props-contact@example.com");
        
        AgentProvider provider = new AgentProvider();
        provider.setOrganization("Test Props Org");
        provider.setUrl("http://test-props-provider-url.com");
        testCard.setProvider(provider);
        
        return testCard;
    }
    
    /**
     * Applies the properties from AgentCardProperties to the agent card object.
     * 
     * @param agentCard The agent card to update with properties
     */
    private void applyAgentCardProperties(AgentCard agentCard) {
        // Override fields with values from AgentCardProperties
        if (agentCardProperties.getId() != null) {
            agentCard.setId(agentCardProperties.getId());
        }
        if (agentCardProperties.getName() != null) {
            agentCard.setName(agentCardProperties.getName());
        }
        if (agentCardProperties.getDescription() != null) {
            agentCard.setDescription(agentCardProperties.getDescription());
        }
        if (agentCardProperties.getUrl() != null) {
            agentCard.setUrl(agentCardProperties.getUrl());
        }
        if (agentCardProperties.getContactEmail() != null) {
            agentCard.setContact_email(agentCardProperties.getContactEmail());
        }

        // Override provider details if present in properties
        if (agentCardProperties.getProvider() != null) {
            AgentProvider provider = agentCard.getProvider();
            if (provider == null) {
                provider = new AgentProvider();
            }
            if (agentCardProperties.getProvider().getOrganization() != null) {
                provider.setOrganization(agentCardProperties.getProvider().getOrganization());
            }
            if (agentCardProperties.getProvider().getUrl() != null) {
                provider.setUrl(agentCardProperties.getProvider().getUrl());
            }
            agentCard.setProvider(provider);
        }
    }
}
