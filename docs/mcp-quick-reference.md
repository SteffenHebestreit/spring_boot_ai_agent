# MCP Integration Quick Reference

## 🚀 Quick Start

### Configuration Example
```yaml
agent:
  integrations:
    mcp-servers:
      - name: "webcrawl-mcp"
        url: "https://your-mcp-server.com"
        auth:
          type: "bearer"
          token: "${MCP_TOKEN}"
```

### Environment Variables
```bash
# Required for MCP server authentication
export MCP_TOKEN="your-bearer-token"

# Optional: Keycloak authentication
export KEYCLOAK_SECRET="your-client-secret"
```

## 🔧 Implementation Flow

### 1. Session Initialization
```java
// Automatic MCP handshake
String sessionId = initializeMcpSession(mcpConfig);
```

### 2. Tool Discovery
```java
// MCP-compliant tool listing
List<Map<String, Object>> tools = fetchToolsFromMcpServer(mcpConfig);
```

### 3. Authentication
```java
// Cached token retrieval
Optional<String> token = getAuthToken(authConfig, serviceName);
```

## 📋 Protocol Checklist

- [x] **Initialize Request** - `method: "initialize"`
- [x] **Protocol Version** - `"2024-11-05"`
- [x] **Capabilities Declaration** - Tools & resources support
- [x] **Initialized Notification** - `method: "notifications/initialized"`
- [x] **Session Headers** - `Mcp-Session-Id` in all requests
- [x] **JSON-RPC Structure** - All required fields present
- [x] **Modern Methods** - `tools/list` (not legacy `mcp.*`)

## 🛠️ Debugging

### Enable Debug Logging
```yaml
logging:
  level:
    com.steffenhebestreit.ai_research.Service.DynamicIntegrationService: DEBUG
```

### Common Issues
| **Issue** | **Solution** |
|-----------|-------------|
| `initializeMcpSession` returns `null` | Check MCP server `/mcp` endpoint availability |
| Authentication failures | Verify token/credentials in configuration |
| Empty tools list | Ensure server supports MCP 2024-11-05 specification |
| Connection timeouts | Adjust timeout values in `DynamicIntegrationService` |

## 📚 Documentation References

- **Full Documentation**: `docs/mcp-compliance.md`
- **JavaDoc**: `DynamicIntegrationService` class documentation
- **Configuration**: `README.md` MCP section
- **Changelog**: `CHANGELOG.md` version 1.1.0

## 🧪 Testing

### Verify MCP Compliance
```bash
# Compile and test
mvn clean test

# Check for MCP integration logs
mvn spring-boot:run | grep "MCP"
```

### Expected Log Output
```
INFO  - Initializing MCP session with server: webcrawl-mcp at URL: https://server.com/mcp
INFO  - Successfully initialized MCP session session_1735909876543 with server: webcrawl-mcp  
INFO  - Fetching tools from MCP server: webcrawl-mcp with session: session_1735909876543
```
