# Changelog

## [1.2.0] - 2025-01-28

### Added - MCP Tool Integration and Enhanced Streaming

#### 🚀 **MCP Tool Execution Integration**

**Core Tool Execution:**
- **Complete Tool Workflow** - Full MCP tool calling workflow from LLM to tool execution and back
- **Reactive Tool Execution** - Enhanced streaming with tool call detection and execution 
- **Tool Discovery Integration** - Automatic tool discovery from MCP servers and inclusion in LLM requests
- **Tool Result Processing** - Proper handling and formatting of tool execution results

**OpenAIService Enhancements:**
- `getChatCompletionStreamWithToolExecution()` - New streaming method with full tool support
- `executeStreamingConversationWithTools()` - Core tool execution workflow handler
- `convertMcpToolsToOpenAIFormat()` - Tool format conversion for API compatibility
- `executeToolCallFromNode()` - Individual tool call execution and result processing
- `continueConversationAfterTools()` - Conversation continuation after tool completion

**DynamicIntegrationService Tool Execution:**
- `executeToolCall()` - Execute specific tools via MCP servers
- `findServerForTool()` - Locate which MCP server provides a given tool
- `findServerConfigByName()` - Server configuration lookup by name

#### 🎯 **Streaming and Controller Updates**

**Enhanced Streaming Support:**
- **Tool-aware Streaming** - ChatController and TaskController now use tool-enabled streaming
- **Real-time Tool Feedback** - Users see tool execution progress in real-time
- **Error Resilience** - Robust error handling during tool execution workflows

**Controller Updates:**
- **ChatController** - Updated to use `getChatCompletionStreamWithToolExecution`
- **TaskController** - Enhanced streaming chat with tool execution support
- **Tool Execution Feedback** - Real-time progress indicators for tool calls

#### 🔧 **Service Refactoring and Improvements**

**ChatService Simplification:**
- **Removed OpenAIService Dependency** - Eliminated circular dependency concerns
- **Removed Message Summarization** - Simplified message handling workflow
- **Constructor Simplification** - Cleaner dependency injection pattern

**Testing Infrastructure:**
- **OpenAIServiceMcpToolsTest** - Comprehensive test suite for MCP tool integration
- **Tool Workflow Testing** - Test coverage for complete tool execution workflows
- **Mock Integration Testing** - WebClient and service interaction testing

#### 🏗️ **Architecture Improvements**

**Reactive Architecture:**
- **Flux-based Tool Execution** - Non-blocking tool execution workflows
- **Stream Completion Management** - Proper stream lifecycle management with tool calls
- **Concurrent Execution Prevention** - Thread-safe tool execution handling

**Error Handling:**
- **Tool Execution Errors** - Graceful handling of tool execution failures
- **Stream Error Recovery** - Fallback responses for streaming failures
- **Logging Enhancements** - Comprehensive logging for debugging tool workflows

---

## [1.1.0] - 2025-06-03

### Added - MCP Compliance Implementation

#### 🎯 **100% Model Context Protocol (MCP) Compliance**

**Core Implementation:**
- **Full MCP Protocol Handshake** - Complete `initialize` → `notifications/initialized` sequence
- **Session Management** - Automatic session ID generation and `Mcp-Session-Id` header handling
- **JSON-RPC 2.0 Structure** - Proper request format with required `jsonrpc`, `method`, `id`, `params` fields
- **MCP Specification 2024-11-05** - Latest protocol version support

**DynamicIntegrationService Enhancements:**
- `initializeMcpSession()` - Performs required MCP protocol handshake
- `sendInitializedNotification()` - Completes initialization sequence  
- `extractSessionId()` - Session management helper method
- `fetchToolsFromMcpServer()` - **Updated to be fully MCP-compliant**
- `TokenWrapper` inner class - Authentication token caching with expiration

#### 🔧 **Technical Improvements**

**Protocol Compliance Fixes:**
- ✅ Changed from REST-style endpoints (`/tools/list`) to proper MCP endpoints (`/mcp`)
- ✅ Added required `params` field to all JSON-RPC requests (was missing)
- ✅ Implemented proper session management with headers
- ✅ Added capability negotiation with tools and resources support
- ✅ Fixed JSON-RPC structure to include all required fields
- ✅ Ensured robust session ID extraction from MCP `initialize` response body by correctly parsing JSON, resolving previous compilation and runtime issues.

**Authentication Enhancements:**
- **TokenWrapper Class** - Thread-safe token caching with 30-second expiry buffer
- **Multi-method Auth Support** - Bearer tokens and Keycloak OAuth2 client credentials
- **Automatic Token Refresh** - Seamless token renewal on expiry
- **Cache Key Strategy** - Composite keys for multi-tenant scenarios

**Error Handling Improvements:**
- **Graceful Degradation** - Failed MCP initialization prevents invalid tool requests
- **Comprehensive Logging** - Full protocol flow logging with context
- **JSON-RPC Error Codes** - Standard error code implementation
- **Connection Resilience** - Timeout handling and retry logic

#### 📚 **Documentation Updates**

**New Documentation:**
- `docs/mcp-compliance.md` - Complete MCP implementation guide
- **README.md Updates** - MCP compliance section with protocol flow diagrams
- **JavaDoc Enhancements** - Updated service documentation with MCP details

**Documentation Highlights:**
- **Protocol Flow Diagrams** - Visual representation of MCP handshake sequence
- **Configuration Examples** - Complete YAML configuration samples
- **Authentication Methods** - Bearer token and Keycloak setup guides
- **Troubleshooting Guide** - Common issues and solutions
- **Compliance Verification** - Reference implementation testing details

#### 🧪 **Verification & Testing**

**Compliance Testing:**
- **Reference Implementation** - Tested against `SteffenHebestreit/webcrawl-mcp`
- **Protocol Validation** - All MCP 2024-11-05 requirements verified
- **Error Scenario Testing** - Connection failures and protocol errors handled
- **Authentication Testing** - Multiple auth methods validated

**Code Quality:**
- **Zero Compilation Errors** - All type safety issues resolved
- **Thread Safety** - Concurrent access patterns implemented
- **Performance Optimization** - Token caching and connection reuse

### Changed

#### 🔄 **Breaking Changes**

**MCP Integration Behavior:**
- **Initialization Required** - MCP servers now require proper protocol handshake before tool discovery
- **Session Headers** - All MCP requests now include `Mcp-Session-Id` headers
- **JSON-RPC Structure** - Requests now include required `params` field

**Configuration Impact:**
- **No Configuration Changes Required** - Existing MCP server configurations remain compatible
- **Authentication Enhanced** - Additional OAuth2 options available but existing configs work unchanged

#### 📈 **Improvements**

**Performance Enhancements:**
- **Token Caching** - Reduces authentication overhead for repeated MCP operations
- **Connection Reuse** - Optimized HTTP client configuration
- **Session Persistence** - Single session per MCP server during capability refresh

**Reliability Improvements:**
- **Error Recovery** - Better handling of network and protocol errors
- **Timeout Configuration** - Configurable connection and read timeouts
- **Logging Enhancement** - More detailed debugging information

### Technical Details

#### 🏗️ **Architecture Changes**

**Service Layer:**
```java
// New MCP-compliant initialization flow
String sessionId = initializeMcpSession(mcpConfig);
if (sessionId != null) {
    // Tool discovery with proper session management
    fetchToolsFromMcpServer(mcpConfig, sessionId);
}
```

**Protocol Implementation:**
```json
// Proper MCP initialization request
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "id": "1",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {"listChanged": true},
      "resources": {"listChanged": true}
    },
    "clientInfo": {
      "name": "AI-Research-Application",
      "version": "1.0.0"
    }
  }
}
```

#### 🔍 **Compatibility Matrix**

| **MCP Feature** | **Support Level** | **Implementation** |
|-----------------|------------------|-------------------|
| Protocol Version 2024-11-05 | ✅ Full | Complete implementation |
| Initialize Handshake | ✅ Full | Required before tool operations |
| Session Management | ✅ Full | Automatic session ID tracking |
| Tools Discovery | ✅ Full | JSON-RPC tools/list method |
| Tool Execution | 🔄 Planned | Future enhancement |
| Resources Support | ✅ Full | Capability announced |
| Authentication | ✅ Full | Bearer + OAuth2 support |

#### 🚀 **Migration Guide**

**For Existing Deployments:**
1. **No Action Required** - Changes are backward compatible
2. **Enhanced Logging** - More detailed MCP protocol logs available
3. **Performance Improvement** - Automatic token caching benefit

**For New MCP Servers:**
1. **Ensure MCP 2024-11-05 Compliance** - Server must support current specification
2. **Verify Endpoints** - Server should expose `/mcp` endpoint for JSON-RPC
3. **Authentication Setup** - Configure Bearer tokens or Keycloak as needed

---

## [1.0.0] - Previous Release

### Features
- Basic MCP server integration (non-compliant)
- A2A peer communication
- Multimodal content support
- OpenAI API integration
- Task management system

### Known Issues (Resolved in 1.1.0)
- ❌ MCP integration used REST-style endpoints instead of JSON-RPC
- ❌ Missing required MCP initialization handshake
- ❌ No session management implementation
- ❌ Incomplete JSON-RPC 2.0 structure
- ❌ TokenWrapper class not defined causing compilation errors

---

## Upcoming Features

### [1.2.0] - Planned
- **MCP Tool Execution** - Direct tool invocation through MCP protocol
- **Resource Discovery** - MCP resource enumeration and access
- **Session Persistence** - Cross-restart session management
- **Connection Pooling** - Improved performance for multiple MCP operations
- **Metrics Collection** - MCP server performance monitoring
