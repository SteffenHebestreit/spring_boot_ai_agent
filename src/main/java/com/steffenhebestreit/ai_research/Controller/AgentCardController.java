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
 * REST Controller for Agent-to-Agent (A2A) protocol compliance and agent card management.
 * 
 * <p>This controller implements the Agent-to-Agent protocol requirements by exposing
 * standardized agent metadata at the well-known endpoint. It manages agent card
 * configuration, template processing, and runtime property injection to provide
 * comprehensive agent discovery capabilities.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>A2A Protocol Compliance:</strong> Serves agent cards at /.well-known/agent.json</li>
 * <li><strong>Template Processing:</strong> Loads base templates from classpath resources</li>
 * <li><strong>Configuration Override:</strong> Applies runtime properties to agent metadata</li>
 * <li><strong>Environment Awareness:</strong> Provides test-specific configurations</li>
 * </ul>
 * 
 * <h3>Agent Card Structure:</h3>
 * <ul>
 * <li><strong>Identification:</strong> Unique agent ID, name, and description</li>
 * <li><strong>Connectivity:</strong> URL endpoints and contact information</li>
 * <li><strong>Provider Details:</strong> Organization and provider URL information</li>
 * <li><strong>Capabilities:</strong> Supported protocols and service offerings</li>
 * </ul>
 * 
 * <h3>Configuration Management:</h3>
 * <ul>
 * <li><strong>Template Loading:</strong> Base agentCard.json from classpath resources</li>
 * <li><strong>Property Injection:</strong> Runtime override via AgentCardProperties</li>
 * <li><strong>Profile Support:</strong> Different configurations for test environments</li>
 * <li><strong>Dynamic Updates:</strong> Properties applied at request time</li>
 * </ul>
 * 
 * <h3>A2A Protocol Integration:</h3>
 * <p>Enables agent discovery and communication by providing standardized metadata
 * that other agents can consume to understand capabilities, endpoints, and interaction
 * protocols supported by this AI research service.</p>
 * 
 * <h3>Environment Handling:</h3>
 * <p>Automatically detects test environments and provides appropriate test configurations
 * to prevent interference with production agent discovery processes.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see AgentCardProperties
 * @see AgentCard
 * @see AgentProvider
 */
@RestController()
public class AgentCardController {

    private final AgentCardProperties agentCardProperties;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes AgentCardController with configuration and environment dependencies.
     * 
     * <p>Sets up the controller with required dependencies for agent card generation,
     * including property configuration for runtime overrides and environment detection
     * for profile-specific behavior.</p>
     * 
     * <h3>Dependencies:</h3>
     * <ul>
     * <li><strong>AgentCardProperties:</strong> Configuration properties for agent metadata overrides</li>
     * <li><strong>Environment:</strong> Spring environment for active profile detection</li>
     * <li><strong>ObjectMapper:</strong> JSON processing for template loading and serialization</li>
     * </ul>
     * 
     * @param agentCardProperties Configuration properties for overriding agent card values
     * @param environment Spring environment for profile and property access
     */
    public AgentCardController(AgentCardProperties agentCardProperties, Environment environment) {
        this.agentCardProperties = agentCardProperties;
        this.environment = environment;
    }    /**
     * Serves the agent card JSON at the standard A2A protocol endpoint.
     * 
     * <p>Implements the Agent-to-Agent protocol by providing agent metadata at the
     * well-known endpoint /.well-known/agent.json. Loads base template from classpath
     * resources and applies runtime configuration overrides based on active profiles
     * and application properties.</p>
     * 
     * <h3>Processing Flow:</h3>
     * <ul>
     * <li>Detects active Spring profiles for environment-specific behavior</li>
     * <li>Returns test-specific agent card for test environments</li>
     * <li>Loads base template from agentCard.json classpath resource</li>
     * <li>Applies configuration overrides from AgentCardProperties</li>
     * <li>Returns fully configured agent card for A2A discovery</li>
     * </ul>
     * 
     * <h3>Environment Handling:</h3>
     * <ul>
     * <li><strong>Test Profile:</strong> Returns pre-configured test agent card</li>
     * <li><strong>Production Profile:</strong> Uses template with property overrides</li>
     * </ul>
     * 
     * <h3>Template Processing:</h3>
     * <p>Base template provides default values that can be overridden by:
     * application properties, environment variables, or configuration classes.</p>
     * 
     * <h3>A2A Compliance:</h3>
     * <p>The returned agent card conforms to A2A protocol specifications,
     * enabling other agents to discover and interact with this service.</p>
     * 
     * @return AgentCard object containing complete agent metadata and capabilities
     * @throws IOException if base template file cannot be read from classpath
     * @see AgentCardProperties
     * @see #createTestAgentCard()
     * @see #applyAgentCardProperties(AgentCard)
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
     * Creates a test-specific agent card for testing environments.
     * 
     * <p>Generates a pre-configured agent card with test-specific values that prevent
     * interference with production agent discovery processes. Used automatically when
     * the "test" profile is active in the Spring environment.</p>
     * 
     * <h3>Test Configuration:</h3>
     * <ul>
     * <li><strong>Identification:</strong> Test-specific ID, name, and description</li>
     * <li><strong>URLs:</strong> Test endpoints that don't conflict with production</li>
     * <li><strong>Contact Info:</strong> Test email addresses for validation</li>
     * <li><strong>Provider:</strong> Test organization and provider details</li>
     * </ul>
     * 
     * <h3>Isolation Benefits:</h3>
     * <ul>
     * <li>Prevents test runs from affecting production agent discovery</li>
     * <li>Provides consistent test data for automated testing</li>
     * <li>Eliminates dependency on external configuration files during tests</li>
     * <li>Ensures predictable behavior in CI/CD environments</li>
     * </ul>
     * 
     * @return AgentCard configured with test-specific values
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
     * Applies runtime configuration properties to the agent card template.
     * 
     * <p>Performs selective property override on the loaded agent card template,
     * replacing default values with those specified in AgentCardProperties.
     * Only non-null property values are applied, preserving template defaults
     * for unspecified configuration items.</p>
     * 
     * <h3>Override Strategy:</h3>
     * <ul>
     * <li><strong>Selective Override:</strong> Only applies non-null property values</li>
     * <li><strong>Template Preservation:</strong> Maintains template defaults for unset properties</li>
     * <li><strong>Provider Handling:</strong> Creates provider object if needed for property application</li>
     * <li><strong>Nested Properties:</strong> Handles complex object property structures</li>
     * </ul>
     * 
     * <h3>Supported Overrides:</h3>
     * <ul>
     * <li><strong>Basic Properties:</strong> ID, name, description, URL, contact email</li>
     * <li><strong>Provider Properties:</strong> Organization name and provider URL</li>
     * <li><strong>Nested Structures:</strong> Provider object creation and property injection</li>
     * </ul>
     * 
     * <h3>Configuration Sources:</h3>
     * <p>Properties can be sourced from application.properties, environment variables,
     * or any Spring-supported configuration mechanism via AgentCardProperties.</p>
     * 
     * @param agentCard The agent card template to update with configuration properties
     * @see AgentCardProperties
     * @see AgentProvider
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
