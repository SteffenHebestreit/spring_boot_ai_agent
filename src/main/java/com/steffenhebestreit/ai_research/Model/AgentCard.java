package com.steffenhebestreit.ai_research.Model;

import java.util.List;

/**
 * Agent Card model representing complete metadata about an AI agent according to the Agent-to-Agent (A2A) protocol.
 * This class serves as the canonical representation of agent information exposed through the
 * {@code /.well-known/agent.json} endpoint, providing comprehensive details about agent capabilities,
 * skills, and interaction methods.
 * 
 * <h3>A2A Protocol Compliance:</h3>
 * <ul>
 *   <li><strong>Discovery Endpoint</strong> - Served at /.well-known/agent.json for agent discovery</li>
 *   <li><strong>Metadata Standards</strong> - Complete agent identification and capability description</li>
 *   <li><strong>Interoperability</strong> - Standardized format for agent-to-agent communication</li>
 *   <li><strong>Authentication Support</strong> - Extended card support for authenticated access</li>
 * </ul>
 * 
 * <h3>Core Agent Information:</h3>
 * <ul>
 *   <li><strong>Identity</strong> - Unique ID, name, description, and contact details</li>
 *   <li><strong>Provider Details</strong> - Organization or service providing the agent</li>
 *   <li><strong>Version Control</strong> - Agent version and documentation references</li>
 *   <li><strong>Service Endpoints</strong> - URLs for agent interaction and documentation</li>
 * </ul>
 * 
 * <h3>Capability Declaration:</h3>
 * <ul>
 *   <li><strong>Input/Output Modes</strong> - Supported data formats and interaction methods</li>
 *   <li><strong>Skills Portfolio</strong> - Detailed enumeration of agent capabilities</li>
 *   <li><strong>Technical Capabilities</strong> - Processing abilities and limitations</li>
 *   <li><strong>Authentication Methods</strong> - Security and access control mechanisms</li>
 * </ul>
 * 
 * <h3>JSON Serialization:</h3>
 * The class is designed for direct JSON serialization to the A2A standard format,
 * enabling seamless integration with agent discovery and communication protocols.
 * All fields follow the A2A naming conventions for maximum compatibility.
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see com.steffenhebestreit.ai_research.Model.AgentCapabilities
 * @see com.steffenhebestreit.ai_research.Model.AgentSkill
 * @see com.steffenhebestreit.ai_research.Model.AgentProvider
 */
public class AgentCard {
    /**
     * Unique identifier for the agent within the A2A ecosystem.
     * This ID must be globally unique and is used for agent discovery and routing.
     * 
     * <p>Format: Typically a UUID or domain-scoped identifier</p>
     */
    private String id;
    
    /**
     * Human-readable name of the agent displayed in user interfaces.
     * This name should be descriptive and help users understand the agent's purpose.
     * 
     * <p>Examples: "Research Assistant", "Data Analyzer", "Document Processor"</p>
     */
    private String name;
    
    /**
     * Detailed description of the agent's purpose, capabilities, and intended use cases.
     * This description is used for agent discovery and helps users understand
     * when and how to interact with this agent.
     */
    private String description;
    
    /**
     * Base URL endpoint where the agent can be accessed for interactions.
     * This URL serves as the primary communication endpoint for A2A protocol messages.
     * 
     * <p>Must be a valid HTTP/HTTPS URL accessible by other agents</p>
     */
    private String url;
    
    /**
     * Provider information describing the organization or service hosting this agent.
     * Contains metadata about the agent's creator, maintainer, and hosting environment.
     * 
     * @see AgentProvider
     */
    private AgentProvider provider;
    
    /**
     * Version string indicating the current release of this agent.
     * Follows semantic versioning conventions to enable compatibility checking.
     * 
     * <p>Format: "major.minor.patch" (e.g., "1.2.3")</p>
     */
    private String version;
    
    /**
     * URL pointing to comprehensive documentation for this agent.
     * Should provide detailed usage instructions, API documentation, and examples.
     */
    private String documentationUrl;
    
    /**
     * Structured representation of the agent's technical capabilities.
     * Defines what types of operations, data formats, and interaction patterns
     * this agent supports.
     * 
     * @see AgentCapabilities
     */
    private AgentCapabilities capabilities;
    
    /**
     * Default input modes supported by this agent for incoming messages.
     * Defines the preferred data formats and communication methods for requests.
     * 
     * <p>Common modes: "text", "json", "multipart", "stream"</p>
     */
    private List<String> defaultInputModes;
    
    /**
     * Default output modes used by this agent for response messages.
     * Defines the data formats and communication methods used for responses.
     * 
     * <p>Common modes: "text", "json", "multipart", "stream"</p>
     */
    private List<String> defaultOutputModes;
    
    /**
     * Collection of specific skills and capabilities offered by this agent.
     * Each skill represents a distinct operation or service that the agent can perform.
     * 
     * @see AgentSkill
     */
    private List<AgentSkill> skills;
    
    /**
     * Indicates whether this agent supports authenticated extended card functionality.
     * When true, additional metadata and capabilities may be available through
     * authenticated requests to the agent card endpoint.
     */
    private boolean supportsAuthenticatedExtendedCard;
    
    /**
     * Contact email address for support, feedback, or administrative inquiries
     * related to this agent. Used for operational communication and issue resolution.
     * 
     * <p>Should follow standard email format (e.g., admin@example.com)</p>
     */
    private String contact_email;
    
    /**
     * Authentication configuration object defining security requirements and methods
     * for accessing this agent. The structure depends on the authentication scheme used.
     * 
     * <p>May contain API keys, OAuth configuration, or other security metadata</p>
     */
    private Object authentication;    // Getters and setters
    
    /**
     * Returns the unique identifier for this agent.
     * 
     * @return the agent ID used for discovery and routing
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier for this agent.
     * 
     * @param id the agent ID to set (should be globally unique)
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Returns the human-readable name of this agent.
     * 
     * @return the agent name for display purposes
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable name of this agent.
     * 
     * @param name the agent name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the detailed description of this agent's purpose and capabilities.
     * 
     * @return the agent description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the detailed description of this agent's purpose and capabilities.
     * 
     * @param description the agent description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the base URL endpoint for agent interactions.
     * 
     * @return the agent's communication endpoint URL
     */
    public String getUrl() {
        return url;
    }    /**
     * Sets the base URL endpoint for agent interactions.
     * 
     * @param url the communication endpoint URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the provider information for this agent.
     * 
     * @return the agent provider details
     */
    public AgentProvider getProvider() {
        return provider;
    }

    /**
     * Sets the provider information for this agent.
     * 
     * @param provider the agent provider details to set
     */
    public void setProvider(AgentProvider provider) {
        this.provider = provider;
    }

    /**
     * Returns the version string of this agent.
     * 
     * @return the agent version following semantic versioning
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version string of this agent.
     * 
     * @param version the agent version to set (should follow semantic versioning)
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the URL pointing to comprehensive agent documentation.
     * 
     * @return the documentation URL
     */
    public String getDocumentationUrl() {
        return documentationUrl;
    }

    /**
     * Sets the URL pointing to comprehensive agent documentation.
     * 
     * @param documentationUrl the documentation URL to set
     */
    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    /**
     * Returns the structured capabilities of this agent.
     * 
     * @return the agent capabilities configuration
     */
    public AgentCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Sets the structured capabilities of this agent.
     * 
     * @param capabilities the agent capabilities configuration to set
     */
    public void setCapabilities(AgentCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Returns the default input modes supported by this agent.
     * 
     * @return list of supported input data formats and methods
     */
    public List<String> getDefaultInputModes() {
        return defaultInputModes;
    }

    /**
     * Sets the default input modes supported by this agent.
     * 
     * @param defaultInputModes list of input modes to set
     */
    public void setDefaultInputModes(List<String> defaultInputModes) {
        this.defaultInputModes = defaultInputModes;
    }

    /**
     * Returns the default output modes used by this agent for responses.
     * 
     * @return list of output data formats and methods
     */
    public List<String> getDefaultOutputModes() {
        return defaultOutputModes;
    }

    /**
     * Sets the default output modes used by this agent for responses.
     * 
     * @param defaultOutputModes list of output modes to set
     */
    public void setDefaultOutputModes(List<String> defaultOutputModes) {
        this.defaultOutputModes = defaultOutputModes;
    }

    /**
     * Returns the collection of specific skills offered by this agent.
     * 
     * @return list of agent skills and capabilities
     */
    public List<AgentSkill> getSkills() {
        return skills;
    }

    /**
     * Sets the collection of specific skills offered by this agent.
     * 
     * @param skills list of agent skills to set
     */
    public void setSkills(List<AgentSkill> skills) {
        this.skills = skills;
    }    
    
    /**
     * Returns whether this agent supports authenticated extended card functionality.
     * 
     * @return {@code true} if extended card features are available through authentication
     */
    public boolean isSupportsAuthenticatedExtendedCard() {
        return supportsAuthenticatedExtendedCard;
    }

    /**
     * Sets whether this agent supports authenticated extended card functionality.
     * 
     * @param supportsAuthenticatedExtendedCard {@code true} to enable extended card features
     */
    public void setSupportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
        this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
    }
    
    /**
     * Returns the contact email address for this agent.
     * 
     * @return the contact email for support and administrative inquiries
     */
    public String getContact_email() {
        return contact_email;
    }
    
    /**
     * Sets the contact email address for this agent.
     * 
     * @param contact_email the contact email to set
     */
    public void setContact_email(String contact_email) {
        this.contact_email = contact_email;
    }
    
    /**
     * Returns the authentication configuration object for this agent.
     * 
     * @return the authentication configuration, or {@code null} if no authentication is required
     */
    public Object getAuthentication() {
        return authentication;
    }
    
    /**
     * Sets the authentication configuration object for this agent.
     * 
     * @param authentication the authentication configuration to set
     */
    public void setAuthentication(Object authentication) {
        this.authentication = authentication;
    }
}
