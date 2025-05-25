package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Model.AgentCard;
import com.steffenhebestreit.ai_research.Model.AgentProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Test-specific implementation of the AgentCardController
 * This controller returns a fixed agent card for testing
 */
@RestController
@Profile("test")
public class TestAgentCardController {
    
    /**
     * Provides a fixed AgentCard for tests
     * 
     * @return A test-specific agent card with fixed values
     */
    @GetMapping("/.well-known/agent.json")
    public AgentCard getAgentCard() throws IOException {
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
}
