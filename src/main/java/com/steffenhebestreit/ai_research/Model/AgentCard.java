package com.steffenhebestreit.ai_research.Model;

import java.util.List;

/**
 * Represents an Agent Card according to the A2A protocol.
 *
 * This class encapsulates all the metadata about an AI agent according to the
 * Agent-to-Agent (A2A) protocol, including its capabilities, skills, and provider information.
 * It is served at the /.well-known/agent.json endpoint.
 */
public class AgentCard {
    private String id;
    private String name;
    private String description;
    private String url;
    private AgentProvider provider;
    private String version;
    private String documentationUrl;
    private AgentCapabilities capabilities;
    private List<String> defaultInputModes;
    private List<String> defaultOutputModes;
    private List<AgentSkill> skills;
    private boolean supportsAuthenticatedExtendedCard;
    private String contact_email;
    private Object authentication;

    // Getters and setters
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

    public AgentProvider getProvider() {
        return provider;
    }

    public void setProvider(AgentProvider provider) {
        this.provider = provider;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public AgentCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(AgentCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public List<String> getDefaultInputModes() {
        return defaultInputModes;
    }

    public void setDefaultInputModes(List<String> defaultInputModes) {
        this.defaultInputModes = defaultInputModes;
    }

    public List<String> getDefaultOutputModes() {
        return defaultOutputModes;
    }

    public void setDefaultOutputModes(List<String> defaultOutputModes) {
        this.defaultOutputModes = defaultOutputModes;
    }

    public List<AgentSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<AgentSkill> skills) {
        this.skills = skills;
    }    
    
    public boolean isSupportsAuthenticatedExtendedCard() {
        return supportsAuthenticatedExtendedCard;
    }

    public void setSupportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
        this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getContact_email() {
        return contact_email;
    }
    
    public void setContact_email(String contact_email) {
        this.contact_email = contact_email;
    }
    
    public Object getAuthentication() {
        return authentication;
    }
    
    public void setAuthentication(Object authentication) {
        this.authentication = authentication;
    }
}
