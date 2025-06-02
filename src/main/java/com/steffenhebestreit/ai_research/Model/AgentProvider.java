package com.steffenhebestreit.ai_research.Model;

/**
 * Model representing provider information for AI agents in the A2A protocol ecosystem.
 * 
 * <p>This class encapsulates organizational and contact information for the entity
 * that developed, maintains, and operates an AI agent. It provides essential
 * attribution and accountability information required by the Agent-to-Agent (A2A)
 * protocol for agent discovery and trust establishment.</p>
 * 
 * <h3>Core Information:</h3>
 * <ul>
 * <li><strong>Provider Identity:</strong> Name and organizational identification</li>
 * <li><strong>Contact Information:</strong> URLs for provider communication and support</li>
 * <li><strong>Attribution:</strong> Clear identification of agent responsibility</li>
 * <li><strong>Trust Establishment:</strong> Verifiable provider information for agent validation</li>
 * </ul>
 * 
 * <h3>A2A Protocol Integration:</h3>
 * <ul>
 * <li><strong>Agent Cards:</strong> Embedded in agent card metadata for discovery</li>
 * <li><strong>Trust Verification:</strong> Enables verification of agent authenticity</li>
 * <li><strong>Support Channels:</strong> Provides contact information for operational issues</li>
 * <li><strong>Liability:</strong> Clear attribution for agent behavior and capabilities</li>
 * </ul>
 * 
 * <h3>Provider Types:</h3>
 * <ul>
 * <li><strong>Organizations:</strong> Companies, universities, research institutions</li>
 * <li><strong>Individual Developers:</strong> Personal projects and research efforts</li>
 * <li><strong>Open Source Projects:</strong> Community-driven agent development</li>
 * <li><strong>Commercial Entities:</strong> Professional AI service providers</li>
 * </ul>
 * 
 * <h3>Information Usage:</h3>
 * <ul>
 * <li>Agent discovery and identification</li>
 * <li>Trust and reputation establishment</li>
 * <li>Support and operational contact</li>
 * <li>Legal and compliance attribution</li>
 * </ul>
 * 
 * <h3>JSON Serialization:</h3>
 * <p>This class is designed for JSON serialization as part of agent card structures,
 * enabling standardized provider information exchange in the A2A ecosystem.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see AgentCard
 * @see AgentCardProperties
 */
public class AgentProvider {
    /**
     * The name of the provider entity.
     * 
     * <p>Provides a human-readable identifier for the provider, which may be
     * different from the organization name. This could be a project name,
     * service name, or specific identifier for the agent provider.</p>
     * 
     * <h3>Usage:</h3>
     * <ul>
     * <li>Display in user interfaces</li>
     * <li>Agent attribution in logs and documentation</li>
     * <li>Provider identification in agent lists</li>
     * </ul>
     */
    private String name;
    
    /**
     * The organization responsible for the agent.
     * 
     * <p>Specifies the organizational entity that developed, maintains, or operates
     * the agent. This provides clear attribution and accountability for the agent's
     * behavior, capabilities, and operational characteristics.</p>
     * 
     * <h3>Organization Types:</h3>
     * <ul>
     * <li>Corporate entities and companies</li>
     * <li>Academic and research institutions</li>
     * <li>Government agencies and departments</li>
     * <li>Non-profit organizations</li>
     * <li>Open source communities</li>
     * </ul>
     * 
     * <h3>A2A Protocol:</h3>
     * <p>Required field for agent discovery and trust establishment in
     * multi-agent systems.</p>
     */
    private String organization;
    
    /**
     * The URL for the provider's main website or information page.
     * 
     * <p>Provides a web-accessible location where users and other agents can
     * find more information about the provider, including documentation,
     * support resources, and contact information.</p>
     * 
     * <h3>URL Content:</h3>
     * <ul>
     * <li>Provider organization information</li>
     * <li>Agent documentation and specifications</li>
     * <li>Support and contact information</li>
     * <li>Terms of service and usage policies</li>
     * <li>Technical documentation and APIs</li>
     * </ul>
     * 
     * <h3>Best Practices:</h3>
     * <ul>
     * <li>Use HTTPS URLs for security</li>
     * <li>Ensure URL is publicly accessible</li>
     * <li>Provide comprehensive agent information</li>
     * <li>Keep information current and accurate</li>
     * </ul>
     */
    private String url;

    // Getters and setters with comprehensive documentation

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
