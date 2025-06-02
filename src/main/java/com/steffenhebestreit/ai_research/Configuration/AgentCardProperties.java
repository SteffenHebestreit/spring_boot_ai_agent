package com.steffenhebestreit.ai_research.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Agent Card customization and runtime override.
 * 
 * <p>This configuration class enables runtime customization of agent card metadata
 * through application properties, environment variables, or other Spring-supported
 * configuration sources. It provides override capabilities for the base agent card
 * template served at the A2A protocol endpoint /.well-known/agent.json.</p>
 * 
 * <h3>Core Functionality:</h3>
 * <ul>
 * <li><strong>Runtime Override:</strong> Dynamic agent card customization without code changes</li>
 * <li><strong>Template Enhancement:</strong> Selective property override while preserving defaults</li>
 * <li><strong>Environment Adaptation:</strong> Different configurations for dev/staging/production</li>
 * <li><strong>A2A Compliance:</strong> Maintains Agent-to-Agent protocol compatibility</li>
 * </ul>
 * 
 * <h3>Configuration Hierarchy:</h3>
 * <ul>
 * <li><strong>Base Template:</strong> agentCard.json from classpath resources</li>
 * <li><strong>Property Override:</strong> Values from this configuration class</li>
 * <li><strong>Environment Variables:</strong> OS-level configuration support</li>
 * <li><strong>Profile-specific:</strong> Different values per Spring profile</li>
 * </ul>
 * 
 * <h3>Override Strategy:</h3>
 * <ul>
 * <li><strong>Selective Override:</strong> Only non-null properties replace template values</li>
 * <li><strong>Nested Object Support:</strong> Provider information can be partially overridden</li>
 * <li><strong>Null Preservation:</strong> Null values preserve template defaults</li>
 * <li><strong>Type Safety:</strong> Strong typing for configuration validation</li>
 * </ul>
 * 
 * <h3>Configuration Example:</h3>
 * <pre>
 * agent:
 *   card:
 *     id: "research-agent-prod"
 *     name: "Production Research Agent"
 *     url: "https://research-agent.example.com"
 *     contact-email: "support@example.com"
 *     provider:
 *       organization: "Example Research Inc"
 *       url: "https://example.com"
 * </pre>
 * 
 * <h3>Use Cases:</h3>
 * <ul>
 * <li><strong>Environment Deployment:</strong> Different URLs and contact info per environment</li>
 * <li><strong>Branding Customization:</strong> Organization-specific agent identification</li>
 * <li><strong>Testing Isolation:</strong> Test-specific agent cards for development</li>
 * <li><strong>Multi-tenant Deployment:</strong> Tenant-specific agent configurations</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see AgentCardController
 * @see AgentCard
 * @see AgentProvider
 */
@Configuration
@ConfigurationProperties(prefix = "agent.card")
public class AgentCardProperties {

    /**
     * Override value for the agent's unique identifier.
     * 
     * <p>When set, this value replaces the ID from the base agent card template.
     * The ID should be globally unique within the A2A ecosystem to prevent
     * conflicts with other agents during discovery and communication.</p>
     */
    private String id;
    
    /**
     * Override value for the agent's display name.
     * 
     * <p>When set, this value replaces the name from the base agent card template.
     * The name should be descriptive and help users understand the agent's
     * purpose and capabilities.</p>
     */
    private String name;
    
    /**
     * Override value for the agent's description.
     * 
     * <p>When set, this value replaces the description from the base agent card template.
     * The description should provide comprehensive information about the agent's
     * capabilities, use cases, and intended audience.</p>
     */
    private String description;
    
    /**
     * Override value for the agent's base URL endpoint.
     * 
     * <p>When set, this value replaces the URL from the base agent card template.
     * The URL should be the primary communication endpoint where other agents
     * can reach this agent for A2A protocol interactions.</p>
     */
    private String url;
    
    /**
     * Override values for the agent's provider information.
     * 
     * <p>When set, individual properties within this provider object replace
     * corresponding values in the base template's provider section. Supports
     * partial override where only specified properties are changed.</p>
     */
    private Provider provider;
    
    /**
     * Override value for the agent's contact email address.
     * 
     * <p>When set, this value replaces the contact email from the base agent card template.
     * The email should be monitored for administrative inquiries, support requests,
     * and operational communications related to the agent.</p>
     */
    private String contactEmail;

    /**
     * Nested configuration class for provider information override.
     * 
     * <p>Enables selective override of provider-related fields in the agent card
     * while preserving other provider information from the base template. Supports
     * partial updates where only specific provider properties are customized.</p>
     * 
     * <h3>Override Behavior:</h3>
     * <ul>
     * <li>Non-null values replace corresponding template values</li>
     * <li>Null values preserve template defaults</li>
     * <li>Creates provider object if template doesn't have one</li>
     * <li>Maintains nested object structure integrity</li>
     * </ul>
     */
    public static class Provider {
        /**
         * Override value for the provider organization name.
         * 
         * <p>When set, this value replaces the organization name in the base
         * template's provider section. Should identify the organization or
         * entity responsible for the agent's development and maintenance.</p>
         */
        private String organization;
        
        /**
         * Override value for the provider's URL.
         * 
         * <p>When set, this value replaces the provider URL in the base
         * template's provider section. Should point to the provider's
         * main website or information page about the agent service.</p>
         */
        private String url;

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    // Getters and setters for main properties

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
