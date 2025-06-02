package com.steffenhebestreit.ai_research.Configuration;

/**
 * Configuration class for Model Context Protocol (MCP) server connections and tool discovery.
 * 
 * <p>This class defines the configuration structure for connecting to MCP servers
 * that provide tools, capabilities, and services following the Model Context Protocol
 * specification. It enables dynamic tool discovery and integration with external
 * systems that expose their functionality through MCP-compatible interfaces.</p>
 * 
 * <h3>MCP Protocol Overview:</h3>
 * <ul>
 * <li><strong>Tool Discovery:</strong> Automatic enumeration of available tools via JSON-RPC</li>
 * <li><strong>Capability Exchange:</strong> Structured tool metadata and parameter information</li>
 * <li><strong>Standardized Interface:</strong> Consistent tool invocation and response handling</li>
 * <li><strong>Dynamic Integration:</strong> Runtime discovery and configuration of external tools</li>
 * </ul>
 * 
 * <h3>Configuration Elements:</h3>
 * <ul>
 * <li><strong>Server Identification:</strong> Unique name and URL for MCP server discovery</li>
 * <li><strong>Authentication Setup:</strong> Security configuration for protected MCP endpoints</li>
 * <li><strong>Tool Discovery:</strong> Automatic tool enumeration via tools/list method</li>
 * <li><strong>Communication Protocol:</strong> JSON-RPC 2.0 based tool interaction</li>
 * </ul>
 * 
 * <h3>Tool Integration Features:</h3>
 * <ul>
 * <li><strong>Metadata Discovery:</strong> Tool names, descriptions, and parameter schemas</li>
 * <li><strong>Dynamic Invocation:</strong> Runtime tool execution with parameter validation</li>
 * <li><strong>Result Processing:</strong> Structured response handling and data transformation</li>
 * <li><strong>Error Handling:</strong> Graceful handling of tool execution failures</li>
 * </ul>
 * 
 * <h3>Security and Authentication:</h3>
 * <ul>
 * <li><strong>Public MCP Servers:</strong> Open tool discovery without authentication</li>
 * <li><strong>Protected MCP Servers:</strong> API key, bearer token, or OAuth2 authentication</li>
 * <li><strong>Secure Tool Execution:</strong> Authenticated tool invocation and result retrieval</li>
 * <li><strong>Access Control:</strong> Fine-grained permissions for tool usage</li>
 * </ul>
 * 
 * <h3>MCP Server Types:</h3>
 * <ul>
 * <li><strong>Research Tools:</strong> Academic databases, literature search, citation tools</li>
 * <li><strong>Data Processing:</strong> Analytics, transformation, and visualization tools</li>
 * <li><strong>External APIs:</strong> Weather, news, financial data, and other external services</li>
 * <li><strong>Specialized Services:</strong> Domain-specific tools and custom functionality</li>
 * </ul>
 * 
 * <h3>Configuration Example:</h3>
 * <pre>
 * McpServerConfig server = new McpServerConfig();
 * server.setName("research-tools");
 * server.setUrl("https://mcp-server.example.com");
 * server.setAuth(authConfig); // Optional authentication
 * </pre>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see AuthConfig
 * @see DynamicIntegrationService
 * @see IntegrationProperties
 */
public class McpServerConfig {
    /**
     * Human-readable name identifier for the MCP server.
     * 
     * <p>Provides a descriptive name for the MCP server that is used in logging,
     * debugging, tool attribution, and user interfaces. Should be unique within
     * the configuration to enable clear identification of tools and their sources
     * during discovery and execution operations.</p>
     * 
     * <h3>Naming Guidelines:</h3>
     * <ul>
     * <li>Use descriptive names that indicate server purpose or tool domain</li>
     * <li>Avoid special characters that might cause parsing or logging issues</li>
     * <li>Keep names concise but meaningful for easy identification</li>
     * <li>Consider using domain-specific naming conventions (e.g., "research-tools", "data-analytics")</li>
     * </ul>
     * 
     * <h3>Usage in System:</h3>
     * <ul>
     * <li>Tool attribution in discovery results</li>
     * <li>Logging and debugging information</li>
     * <li>User interface display for tool sources</li>
     * <li>Error reporting and troubleshooting</li>
     * </ul>
     */
    private String name;
    
    /**
     * Base URL for the MCP server's JSON-RPC endpoint.
     * 
     * <p>Specifies the primary URL where the MCP server can be reached for
     * tool discovery and execution operations. This URL serves as the base
     * for constructing MCP protocol endpoints like /tools/list for tool
     * discovery and /tools/call for tool execution.</p>
     * 
     * <h3>URL Requirements:</h3>
     * <ul>
     * <li>Must be a valid HTTP or HTTPS URL</li>
     * <li>Should be accessible from this agent's network environment</li>
     * <li>Must support MCP protocol endpoints and JSON-RPC 2.0</li>
     * <li>Should use HTTPS for production environments and sensitive tools</li>
     * </ul>
     * 
     * <h3>MCP Endpoint Construction:</h3>
     * <ul>
     * <li>Tool discovery: {url}/tools/list</li>
     * <li>Tool execution: {url}/tools/call</li>
     * <li>Tool schemas: {url}/tools/get_schema</li>
     * <li>Server capabilities: {url}/initialize</li>
     * </ul>
     * 
     * <h3>URL Format Examples:</h3>
     * <ul>
     * <li>Local development: http://localhost:8080</li>
     * <li>Production service: https://mcp-server.example.com</li>
     * <li>Internal service: http://internal-mcp.company.local</li>
     * </ul>
     */
    private String url;
    
    /**
     * Authentication configuration for secure MCP server communication.
     * 
     * <p>Defines the authentication method and credentials required to
     * communicate with the MCP server. Support for various authentication
     * schemes enables integration with both public and protected MCP servers
     * in diverse security environments and enterprise deployments.</p>
     * 
     * <h3>Authentication Scenarios:</h3>
     * <ul>
     * <li><strong>Public MCP Servers:</strong> null or "none" type for open tool discovery</li>
     * <li><strong>API Key Protected:</strong> "bearer" type with static API keys or tokens</li>
     * <li><strong>OAuth2 Protected:</strong> "keycloak_client_credentials" with full OAuth2 setup</li>
     * <li><strong>Custom Auth:</strong> "api_key" type with custom header-based authentication</li>
     * </ul>
     * 
     * <h3>Security Considerations:</h3>
     * <ul>
     * <li>Use secure credential storage for sensitive authentication data</li>
     * <li>Prefer OAuth2 for enterprise MCP server integration</li>
     * <li>Consider token rotation and expiry policies for long-running deployments</li>
     * <li>Implement proper error handling for authentication failures</li>
     * <li>Monitor and log authentication events for security auditing</li>
     * </ul>
     * 
     * <h3>Tool Access Control:</h3>
     * <p>Authentication may control not only server access but also which tools
     * are available for discovery and execution, enabling fine-grained access
     * control based on client credentials and permissions.</p>
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
