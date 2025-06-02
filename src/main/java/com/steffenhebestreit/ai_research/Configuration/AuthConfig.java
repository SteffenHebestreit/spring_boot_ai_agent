package com.steffenhebestreit.ai_research.Configuration;

/**
 * Configuration class for authentication settings in external system integrations.
 * 
 * <p>This class provides comprehensive authentication configuration support for various
 * authentication schemes used when connecting to external services, MCP servers, and
 * A2A peer agents. It supports multiple authentication protocols with flexible
 * parameter configuration for different security requirements.</p>
 * 
 * <h3>Supported Authentication Types:</h3>
 * <ul>
 * <li><strong>None:</strong> No authentication required for public endpoints</li>
 * <li><strong>Bearer Token:</strong> Static API keys and bearer tokens</li>
 * <li><strong>Basic Authentication:</strong> Username and password authentication</li>
 * <li><strong>API Key:</strong> Header-based API key authentication</li>
 * <li><strong>Keycloak OAuth2:</strong> Client credentials flow with Keycloak</li>
 * </ul>
 * 
 * <h3>OAuth2 Integration:</h3>
 * <ul>
 * <li><strong>Client Credentials Flow:</strong> Machine-to-machine authentication</li>
 * <li><strong>Token Management:</strong> Automatic token refresh and caching</li>
 * <li><strong>Realm Support:</strong> Multi-tenant Keycloak configurations</li>
 * <li><strong>Custom Grant Types:</strong> Configurable OAuth2 grant types</li>
 * </ul>
 * 
 * <h3>Security Features:</h3>
 * <ul>
 * <li><strong>Credential Protection:</strong> Secure storage of sensitive authentication data</li>
 * <li><strong>Token Caching:</strong> Intelligent token lifecycle management</li>
 * <li><strong>Protocol Flexibility:</strong> Support for various authentication standards</li>
 * <li><strong>Configuration Validation:</strong> Type-specific parameter validation</li>
 * </ul>
 * 
 * <h3>Configuration Examples:</h3>
 * <ul>
 * <li><strong>Bearer Token:</strong> type: "bearer", token: "api-key-value"</li>
 * <li><strong>Basic Auth:</strong> type: "basic", username: "user", password: "pass"</li>
 * <li><strong>Keycloak:</strong> type: "keycloak_client_credentials", authServerUrl, realm, clientId, clientSecret</li>
 * </ul>
 * 
 * <h3>Integration Points:</h3>
 * <p>Used by DynamicIntegrationService for authenticating with external systems
 * and by various API clients for secure communication establishment.</p>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see DynamicIntegrationService
 * @see McpServerConfig
 * @see A2aPeerConfig
 */
public class AuthConfig {
    /**
     * The type of authentication scheme to use.
     * 
     * <p>Determines which authentication protocol to apply and which parameters
     * are required for successful authentication. Common values include:</p>
     * <ul>
     * <li><code>none</code> - No authentication required</li>
     * <li><code>bearer</code> - Bearer token authentication</li>
     * <li><code>basic</code> - Basic HTTP authentication</li>
     * <li><code>api_key</code> - API key header authentication</li>
     * <li><code>keycloak_client_credentials</code> - Keycloak OAuth2 client credentials</li>
     * </ul>
     */
    private String type; // e.g., "none", "bearer" (static), "keycloak_client_credentials"
    
    /**
     * Static bearer token or API key for simple authentication schemes.
     * 
     * <p>Used with "bearer" authentication type to provide a static token
     * that will be included in the Authorization header as "Bearer {token}".
     * Also used for API key authentication when combined with apiKeyHeaderName.</p>
     */
    private String token;
    
    /**
     * Header name for API key authentication.
     * 
     * <p>Specifies the HTTP header name where the API key should be included
     * when using API key authentication. Common values include "X-API-Key",
     * "Authorization", or custom header names required by specific services.</p>
     */
    private String apiKeyHeaderName; // For API key header name
    
    /**
     * Username for basic HTTP authentication.
     * 
     * <p>Used with "basic" authentication type to provide the username
     * component of HTTP Basic Authentication credentials.</p>
     */
    private String username;
    
    /**
     * Password for basic HTTP authentication.
     * 
     * <p>Used with "basic" authentication type to provide the password
     * component of HTTP Basic Authentication credentials.</p>
     */
    private String password;

    /**
     * Keycloak server URL for OAuth2 authentication.
     * 
     * <p>Base URL of the Keycloak authentication server used for OAuth2
     * client credentials flow. Should include protocol and domain without
     * trailing slash (e.g., "https://auth.example.com").</p>
     */
    private String authServerUrl;
    
    /**
     * Keycloak realm name for multi-tenant authentication.
     * 
     * <p>Specifies the Keycloak realm where the client is registered.
     * Realms provide isolation and multi-tenancy in Keycloak deployments.</p>
     */
    private String realm;
    
    /**
     * OAuth2 client identifier for Keycloak authentication.
     * 
     * <p>Unique identifier for the OAuth2 client registered in Keycloak.
     * Used with client credentials flow for machine-to-machine authentication.</p>
     */
    private String clientId;
    
    /**
     * OAuth2 client secret for Keycloak authentication.
     * 
     * <p>Secret key for the OAuth2 client used to authenticate the client
     * itself during the client credentials flow. Should be kept secure
     * and not exposed in logs or client-side code.</p>
     */
    private String clientSecret;
    
    /**
     * OAuth2 grant type for flexible authentication flows.
     * 
     * <p>Specifies the OAuth2 grant type to use. Typically "client_credentials"
     * for machine-to-machine authentication, but can be configured for other
     * grant types as needed by specific Keycloak configurations.</p>
     */
    private String grantType; // Typically "client_credentials"

    // Getters and setters with comprehensive documentation

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApiKeyHeaderName() {
        return apiKeyHeaderName;
    }

    public void setApiKeyHeaderName(String apiKeyHeaderName) {
        this.apiKeyHeaderName = apiKeyHeaderName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
}
