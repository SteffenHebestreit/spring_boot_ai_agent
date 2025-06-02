package com.steffenhebestreit.ai_research.Configuration;

/**
 * Configuration class for Agent-to-Agent (A2A) peer connections and discovery.
 * 
 * <p>This class defines the configuration structure for connecting to peer agents
 * in the A2A ecosystem. It encapsulates all necessary information for agent discovery,
 * capability enumeration, and secure communication with other AI agents that follow
 * the A2A protocol specifications.</p>
 * 
 * <h3>Core Configuration Elements:</h3>
 * <ul>
 * <li><strong>Peer Identification:</strong> Unique name and URL for agent discovery</li>
 * <li><strong>Authentication Setup:</strong> Security configuration for protected endpoints</li>
 * <li><strong>Discovery Protocol:</strong> Standard A2A agent card retrieval configuration</li>
 * <li><strong>Communication Endpoints:</strong> Base URLs for inter-agent communication</li>
 * </ul>
 * 
 * <h3>A2A Protocol Integration:</h3>
 * <ul>
 * <li><strong>Agent Discovery:</strong> Automatic retrieval of agent cards from /.well-known/agent.json</li>
 * <li><strong>Skill Enumeration:</strong> Discovery of peer agent capabilities and services</li>
 * <li><strong>Secure Communication:</strong> Authentication-aware peer interactions</li>
 * <li><strong>Capability Matching:</strong> Integration with peer agent skill sets</li>
 * </ul>
 * 
 * <h3>Authentication Support:</h3>
 * <ul>
 * <li><strong>Public Agents:</strong> No authentication for open discovery endpoints</li>
 * <li><strong>Protected Agents:</strong> Bearer token, API key, or OAuth2 authentication</li>
 * <li><strong>Extended Cards:</strong> Authentication for detailed capability information</li>
 * <li><strong>Secure Operations:</strong> Authenticated task delegation and communication</li>
 * </ul>
 * 
 * <h3>Usage Patterns:</h3>
 * <ul>
 * <li><strong>Peer Discovery:</strong> Automatic detection of available agents in network</li>
 * <li><strong>Capability Aggregation:</strong> Collecting skills from multiple peer agents</li>
 * <li><strong>Task Delegation:</strong> Routing tasks to specialized peer agents</li>
 * <li><strong>Collaborative Processing:</strong> Multi-agent workflows and coordination</li>
 * </ul>
 * 
 * <h3>Configuration Example:</h3>
 * <pre>
 * A2aPeerConfig peer = new A2aPeerConfig();
 * peer.setName("analysis-agent");
 * peer.setUrl("https://analysis-agent.example.com");
 * peer.setAuth(authConfig); // Optional authentication
 * </pre>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see AuthConfig
 * @see DynamicIntegrationService
 * @see IntegrationProperties
 */
public class A2aPeerConfig {
    /**
     * Human-readable name identifier for the peer agent.
     * 
     * <p>Provides a descriptive name for the peer agent that is used in logging,
     * debugging, and user interfaces. Should be unique within the configuration
     * to enable clear identification of different peer agents during discovery
     * and communication operations.</p>
     * 
     * <h3>Naming Guidelines:</h3>
     * <ul>
     * <li>Use descriptive names that indicate agent purpose</li>
     * <li>Avoid special characters that might cause parsing issues</li>
     * <li>Keep names concise but meaningful</li>
     * <li>Consider using domain-specific naming conventions</li>
     * </ul>
     */
    private String name;
    
    /**
     * Base URL for the peer agent's communication endpoint.
     * 
     * <p>Specifies the primary URL where the peer agent can be reached for
     * A2A protocol interactions. This URL serves as the base for constructing
     * specific endpoints like /.well-known/agent.json for agent discovery
     * and other A2A protocol operations.</p>
     * 
     * <h3>URL Requirements:</h3>
     * <ul>
     * <li>Must be a valid HTTP or HTTPS URL</li>
     * <li>Should be accessible from this agent's network</li>
     * <li>Must support A2A protocol endpoints</li>
     * <li>Should use HTTPS for production environments</li>
     * </ul>
     * 
     * <h3>URL Usage:</h3>
     * <ul>
     * <li>Agent discovery: {url}/.well-known/agent.json</li>
     * <li>Task communication: {url}/api/tasks</li>
     * <li>Status endpoints: {url}/status or {url}/health</li>
     * </ul>
     */
    private String url;
    
    /**
     * Authentication configuration for secure peer communication.
     * 
     * <p>Defines the authentication method and credentials required to
     * communicate with the peer agent. Support for various authentication
     * schemes enables integration with both public and protected agents
     * in diverse security environments.</p>
     * 
     * <h3>Authentication Scenarios:</h3>
     * <ul>
     * <li><strong>Public Agents:</strong> null or "none" type for open access</li>
     * <li><strong>API Key Protected:</strong> "bearer" type with static tokens</li>
     * <li><strong>OAuth2 Protected:</strong> "keycloak_client_credentials" with full OAuth2 setup</li>
     * <li><strong>Basic Auth Protected:</strong> "basic" type with username/password</li>
     * </ul>
     * 
     * <h3>Security Considerations:</h3>
     * <ul>
     * <li>Use secure credential storage for sensitive authentication data</li>
     * <li>Prefer OAuth2 for production inter-agent communication</li>
     * <li>Consider token rotation and expiry policies</li>
     * <li>Implement proper error handling for authentication failures</li>
     * </ul>
     * 
     * @see AuthConfig
     */
    private AuthConfig auth;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }
}
