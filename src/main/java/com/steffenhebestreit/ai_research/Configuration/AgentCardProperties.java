package com.steffenhebestreit.ai_research.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Agent Card.
 * 
 * This class maps properties from the application configuration with the 'agent.card' prefix
 * to Java properties. These values are used to override the default values in the agentCard.json
 * template when serving the agent card via the AgentCardController.
 */
@Configuration
@ConfigurationProperties(prefix = "agent.card")
public class AgentCardProperties {

    private String id;
    private String name;
    private String description;
    private String url;
    private Provider provider;
    private String contactEmail;

    /**
     * Provider information for the agent card.
     */
    public static class Provider {
        private String organization;
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

    // Getters and Setters

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
