# MCP Tool Execution Workflow

## Overview

This document describes the complete Model Context Protocol (MCP) tool execution workflow implemented in version 1.2.0. The system provides full integration between LLM streaming responses and external MCP server tool execution.

## Tool Execution Architecture

### Core Components

1. **OpenAIService** - Enhanced with tool-aware streaming
2. **DynamicIntegrationService** - Tool discovery and execution
3. **ChatController/TaskController** - User-facing streaming endpoints

### Workflow Sequence

```mermaid
sequenceDiagram
    participant Frontend as Frontend Client
    participant Controller as ChatController
    participant OpenAI as OpenAIService  
    participant LLM as LLM Server
    participant DIS as DynamicIntegrationService
    participant MCP as MCP Server

    Frontend->>Controller: POST /stream-chat
    Controller->>OpenAI: getChatCompletionStreamWithToolExecution()
    OpenAI->>DIS: getDiscoveredMcpTools()
    DIS-->>OpenAI: Available tools list
    OpenAI->>LLM: Streaming request with tools
    
    loop Streaming Response
        LLM-->>OpenAI: Content delta
        OpenAI-->>Controller: Stream content
        Controller-->>Frontend: Real-time response
    end
    
    LLM-->>OpenAI: Tool call detected (finish_reason: "tool_calls")
    OpenAI->>Frontend: [Calling tool: toolName]
    OpenAI->>DIS: executeToolCall(toolName, params)
    DIS->>MCP: JSON-RPC tool execution
    MCP-->>DIS: Tool result
    DIS-->>OpenAI: Formatted result
    OpenAI->>OpenAI: Add tool result to conversation
    OpenAI->>LLM: Continue conversation with tool results
    
    loop Continuation Response
        LLM-->>OpenAI: Response with tool context
        OpenAI-->>Controller: Stream final response
        Controller-->>Frontend: Complete response
    end
```

## Key Features

### Real-time Tool Feedback
- Users see tool execution progress: `[Calling tool: webcrawl]`
- Tool completion notifications: `[Tool completed successfully]`
- Error handling: `[Tool execution failed: reason]`

### Tool Discovery Integration
- Automatic inclusion of MCP tools in LLM requests
- Format conversion from MCP to OpenAI API format
- Server source tracking for tool attribution

### Reactive Streaming Architecture
- Non-blocking tool execution workflows
- Flux-based streaming with proper completion management
- Thread-safe concurrent execution prevention

### Error Resilience
- Graceful handling of tool execution failures
- Fallback responses for streaming failures
- Comprehensive logging for debugging

## Implementation Details

### OpenAIService Enhancements

#### New Methods
- `getChatCompletionStreamWithToolExecution()` - Main tool-enabled streaming
- `executeStreamingConversationWithTools()` - Core workflow handler
- `convertMcpToolsToOpenAIFormat()` - Tool format conversion
- `executeToolCallFromNode()` - Individual tool execution
- `continueConversationAfterTools()` - Conversation continuation

#### Tool Call Detection
```java
// Detects tool calls in streaming delta
JsonNode toolCallsNode = deltaNode.path("tool_calls");
if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
    // Process tool call and execute via MCP
}
```

### DynamicIntegrationService Tool Execution

#### New Methods
- `executeToolCall()` - Execute tools via MCP servers
- `findServerForTool()` - Locate tool provider server
- `findServerConfigByName()` - Server configuration lookup

#### Tool Execution Flow
1. **Tool Discovery**: Find which MCP server provides the requested tool
2. **Session Management**: Ensure valid MCP session exists
3. **JSON-RPC Call**: Execute tool with proper parameters
4. **Result Processing**: Format and return execution results

### Controller Updates

Both `ChatController` and `TaskController` now use the enhanced streaming:

```java
// Updated streaming endpoint
return openAIService.getChatCompletionStreamWithToolExecution(conversationHistory, modelToUse)
    .doOnError(e -> logger.error("Error during AI stream: {}", e.getMessage()))
    .onErrorResume(e -> Flux.just("{\"error\": \"Error: " + e.getMessage() + "\"}"));
```

## Configuration

### MCP Server Setup
```properties
# External MCP Servers
agent.integrations.mcp-servers[0].name=webcrawl-mcp
agent.integrations.mcp-servers[0].url=http://localhost:3001
```

### Tool Format Example
```json
{
  "type": "function",
  "function": {
    "name": "webcrawl",
    "description": "Crawl web pages for content",
    "parameters": {
      "type": "object",
      "properties": {
        "url": {
          "type": "string",
          "description": "URL to crawl"
        }
      }
    }
  }
}
```

## Testing

### Test Coverage
- **OpenAIServiceMcpToolsTest** - Comprehensive tool integration tests
- Tool workflow testing with mock MCP servers
- WebClient interaction testing
- Error scenario validation

### Mock Tool Execution
```java
@Test
public void testToolExecutionWorkflow() {
    // Setup mock tools and conversation
    List<ChatMessage> messages = createSampleConversation();
    when(dynamicIntegrationService.getDiscoveredMcpTools()).thenReturn(mcpTools);
    
    // Test tool execution streaming
    var flux = openAIService.getChatCompletionStreamWithToolExecution(messages);
    assertNotNull(flux);
}
```

## Best Practices

### Tool Development
1. **Proper Error Handling**: Always return structured error responses
2. **Parameter Validation**: Validate all tool parameters before execution
3. **Result Formatting**: Return JSON-serializable results
4. **Performance**: Keep tool execution under 30 seconds

### Frontend Integration
1. **Stream Processing**: Handle streaming tool feedback in UI
2. **Error Display**: Show tool errors to users appropriately
3. **Progress Indicators**: Use tool execution messages for UX
4. **Fallback Handling**: Graceful degradation when tools fail

## HTTP Status Code Handling

### HTTP 304 "Not Modified" Support

The system now properly handles HTTP 304 "Not Modified" responses from MCP servers:

- **Behavior**: HTTP 304 is treated as a successful tool execution
- **Response Handling**: Attempts to parse response body if available
- **Fallback**: Creates success response indicating cached result
- **Logging**: Clear distinction between actual errors and cache hits

```java
// Example HTTP 304 handling in DynamicIntegrationService
if (httpException.getStatusCode() == HttpStatus.NOT_MODIFIED) {
    logger.info("Tool '{}' on server {} returned HTTP 304 (Not Modified) - treating as successful", 
               toolName, serverConfig.getName());
    // Parse response body or create cache success response
}
```

### Status Code Categories

- **2xx Success**: Standard successful tool execution
- **304 Not Modified**: Cached content, treated as successful
- **4xx Client Error**: Authentication or parameter errors
- **5xx Server Error**: MCP server internal errors

## Troubleshooting

### Common Issues

1. **Tool Not Found**: Check MCP server configuration and tool discovery
2. **Session Errors**: Verify MCP server initialization
3. **Streaming Interruption**: Check WebClient configuration and timeouts
4. **Tool Execution Timeout**: Review MCP server performance
5. **HTTP 304 Issues**: Previously caused "Tool execution failed" - now handled correctly

### Debug Logging
```properties
logging.level.com.steffenhebestreit.ai_research.Service.OpenAIService=DEBUG
logging.level.com.steffenhebestreit.ai_research.Service.DynamicIntegrationService=DEBUG
```

## Future Enhancements

- **Parallel Tool Execution**: Execute multiple tools concurrently
- **Tool Result Caching**: Cache frequently used tool results
- **Custom Tool Timeout**: Configurable timeout per tool type
- **Tool Usage Analytics**: Track tool usage and performance metrics

## Enhanced Error Handling for Tool-Only Responses

The system now provides better error handling for cases where an AI response consists solely of tool-related content:

### Problem Statement

When an AI response contains only tool calls or tool execution status messages (e.g., `[Calling tool: search]`), and no actual textual content, the client would receive an empty message after filtering. This could lead to confusion, where the client sees tool execution happening but no final response appears.

### Implementation

The ChatController's streaming endpoint now:

1. Collects the full response during streaming
2. After streaming completes, filters out tool-related content
3. Checks if the filtered response is empty
4. If empty (but originally contained content), sends an error message:
   ```json
   {"error": "AI response was empty after filtering tool-related content."}
   ```
5. The frontend can catch this error and display an appropriate message to the user

### Benefits

This enhancement:
- Provides explicit feedback to the frontend about empty responses
- Helps distinguish between actual empty responses and responses that became empty after filtering
- Allows the frontend to present a clearer message to users about what happened
- Prevents silent failures where the user is left wondering why no response appeared

### Integration with Tool Execution Workflow

This enhancement fits into the existing tool execution workflow:

```mermaid
sequenceDiagram
    participant Frontend as Frontend Client
    participant Controller as ChatController
    participant OpenAI as OpenAIService  
    participant LLM as LLM Server
    participant DIS as DynamicIntegrationService
    participant MCP as MCP Server

    Frontend->>Controller: POST /stream-chat
    Controller->>OpenAI: getChatCompletionStreamWithToolExecution()
    OpenAI->>LLM: Streaming request with tools
    
    loop Streaming Response
        LLM-->>OpenAI: Tool execution content
        OpenAI-->>Controller: Stream raw content
        Controller-->>Frontend: [Tool execution messages]
    end
    
    OpenAI->>DIS: executeToolCall(toolName, params)
    DIS->>MCP: JSON-RPC tool execution
    MCP-->>DIS: Tool result
    DIS-->>OpenAI: Formatted result
    OpenAI->>LLM: Continue conversation with tool results
    
    alt Tool-only response
        LLM-->>OpenAI: Only tool-related content
        OpenAI-->>Controller: Raw content
        Controller->>Controller: Filter tool content
        Controller->>Controller: Detect empty result
        Controller-->>Frontend: {"error": "AI response was empty after filtering tool-related content."}
    else Response with actual content
        LLM-->>OpenAI: Response with actual content
        OpenAI-->>Controller: Raw content
        Controller->>Controller: Filter tool content
        Controller->>Controller: Save filtered response
        Controller-->>Frontend: Final filtered response
    end
```

This improvement completes the tool execution cycle with proper error handling, ensuring users always receive appropriate feedback.
