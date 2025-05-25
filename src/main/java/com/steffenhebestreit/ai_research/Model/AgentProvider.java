package com.steffenhebestreit.ai_research.Model;

/**
 * Represents information about the provider of an AI agent.
 * 
 * This class contains details about the organization that created and maintains
 * the agent, including the organization name and URL.
 */
public class AgentProvider {
    private String name;
    private String organization;
    private String url;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
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
