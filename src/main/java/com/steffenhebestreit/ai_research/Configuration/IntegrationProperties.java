package com.steffenhebestreit.ai_research.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.ArrayList;

/**
 * Configuration properties for external system integrations in the AI Research system.
 * 
 * <p>This configuration class manages connections to external systems and services
 * that extend the AI agent's capabilities through the Model Context Protocol (MCP)
 * and Agent-to-Agent (A2A) protocol. It enables dynamic discovery and integration
 * with various external tools, services, and peer agents.</p>
 * 
 * <h3>Integration Types:</h3>
 * <ul>
 * <li><strong>MCP Servers:</strong> External services providing tools and capabilities via MCP</li>
 * <li><strong>A2A Peers:</strong> Other AI agents for collaboration and task delegation</li>
 * <li><strong>Dynamic Configuration:</strong> Runtime integration management and discovery</li>
 * <li><strong>Authentication Support:</strong> Secure connections with various auth methods</li>
 * </ul>
 * 
 * <h3>MCP Integration:</h3>
 * <ul>
 * <li><strong>Tool Discovery:</strong> Automatic enumeration of available tools</li>
 * <li><strong>Capability Extension:</strong> Adding external tools to agent capabilities</li>
 * <li><strong>JSON-RPC Communication:</strong> Standard protocol for tool invocation</li>
 * <li><strong>Error Handling:</strong> Graceful handling of external service failures</li>
 * </ul>
 * 
 * <h3>A2A Integration:</h3>
 * <ul>
 * <li><strong>Peer Discovery:</strong> Finding and connecting to other agents</li>
 * <li><strong>Skill Sharing:</strong> Access to peer agent capabilities</li>
 * <li><strong>Task Delegation:</strong> Routing tasks to specialized agents</li>
 * <li><strong>Collaborative Processing:</strong> Multi-agent workflow coordination</li>
 * </ul>
 * 
 * <h3>Configuration Example:</h3>
 * <pre>
 * agent:
 *   integrations:
 *     mcp-servers:
 *       - name: "research-tools"
 *         url: "https://research-mcp.example.com"
 *         auth:
 *           type: "bearer"
 *           token: "${RESEARCH_API_KEY}"
 *     a2a-peers:
 *       - name: "analysis-agent"
 *         url: "https://analysis-agent.example.com"
 *         auth:
 *           type: "none"
 * </pre>
 * 
 * <h3>Runtime Management:</h3>
 * <p>Supports dynamic addition and removal of integrations, enabling adaptive
 * agent behavior based on available external services and peer agents.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see McpServerConfig
 * @see A2aPeerConfig
 * @see AuthConfig
 */
@Configuration
@ConfigurationProperties(prefix = "agent.integrations")
public class IntegrationProperties {
    
    /**
     * List of Model Context Protocol (MCP) server configurations.
     * 
     * <p>Contains configuration objects for external MCP servers that provide
     * tools and capabilities to extend the agent's functionality. Each server
     * configuration includes connection details, authentication settings, and
     * tool discovery endpoints.</p>
     * 
     * <h3>MCP Server Features:</h3>
     * <ul>
     * <li>Automatic tool discovery via JSON-RPC</li>
     * <li>Dynamic capability integration</li>
     * <li>Secure authentication support</li>
     * <li>Error handling and fallback strategies</li>
     * </ul>
     * 
     * @see McpServerConfig
     */
    private List<McpServerConfig> mcpServers = new ArrayList<>();
    
    /**
     * List of Agent-to-Agent (A2A) peer configurations.
     * 
     * <p>Contains configuration objects for peer AI agents that can collaborate
     * with this agent through the A2A protocol. Each peer configuration includes
     * discovery endpoints, authentication settings, and capability information.</p>
     * 
     * <h3>A2A Peer Features:</h3>
     * <ul>
     * <li>Agent discovery and capability enumeration</li>
     * <li>Task delegation and collaboration</li>
     * <li>Secure inter-agent communication</li>
     * <li>Skill sharing and composition</li>
     * </ul>
     * 
     * @see A2aPeerConfig
     */
    private List<A2aPeerConfig> a2aPeers = new ArrayList<>();

    public List<McpServerConfig> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(List<McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }

    public List<A2aPeerConfig> getA2aPeers() {
        return a2aPeers;
    }

    public void setA2aPeers(List<A2aPeerConfig> a2aPeers) {
        this.a2aPeers = a2aPeers;
    }
}
