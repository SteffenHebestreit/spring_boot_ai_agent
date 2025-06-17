# Tool Selection and Listing - Usage Guide

## Overview

The AI Research Backend now provides complete tool management capabilities, allowing the frontend to:
1. **Discover Available Tools** - Fetch the list of all available MCP tools
2. **Select Tools Per Request** - Choose which tools to enable for each chat request
3. **Manage Tool Discovery** - Refresh tool discovery when needed

## Available Endpoints

### 1. List Available Tools
```http
GET /research-agent/api/tools
```

**Purpose:** Retrieve all available MCP tools for frontend display and selection.

**Response Format:**
```json
[
  {
    "name": "webcrawl",
    "description": "Crawl and extract content from web pages",
    "sourceMcpServerName": "webcrawl-mcp",
    "sourceMcpServerUrl": "http://localhost:3001",
    "function": {
      "name": "webcrawl",
      "description": "Crawl and extract content from web pages",
      "parameters": {
        "type": "object",
        "properties": {
          "url": {
            "type": "string",
            "description": "The URL to crawl"
          }
        },
        "required": ["url"]
      }
    }
  },
  {
    "name": "search",
    "description": "Search for information across various sources",
    "sourceMcpServerName": "search-mcp",
    "sourceMcpServerUrl": "http://localhost:3002",
    "function": {
      "name": "search",
      "description": "Search for information",
      "parameters": {
        "type": "object",
        "properties": {
          "query": {
            "type": "string",
            "description": "Search query"
          }
        },
        "required": ["query"]
      }
    }
  }
]
```

### 2. Refresh Tool Discovery
```http
POST /research-agent/api/tools/refresh
```

**Purpose:** Force re-discovery of tools from all MCP servers.

**Response Format:**
```json
{
  "status": "success",
  "message": "Tools refreshed successfully", 
  "toolCount": 5,
  "timestamp": 1718654400000
}
```

### 3. Tool System Status
```http
GET /research-agent/api/tools/status
```

**Purpose:** Check the health and status of the tool management system.

**Response Format:**
```json
{
  "status": "healthy",
  "availableTools": 5,
  "timestamp": 1718654400000,
  "version": "1.1.0"
}
```

## Tool Selection in Chat Requests

### ToolSelectionRequest Model

Use the `ToolSelectionRequest` object to control which tools are enabled for a specific chat request:

```javascript
const toolSelectionRequest = {
  enableTools: true,           // Global enable/disable flag
  enabledTools: ["webcrawl"]   // List of specific tools to enable (null = all tools)
};
```

**Field Descriptions:**
- **`enableTools`** (boolean): Master switch for tool usage
  - `true`: Tools can be used (subject to `enabledTools` filter)
  - `false`: No tools will be used regardless of `enabledTools`
- **`enabledTools`** (List<String>): Specific tools to enable
  - `null` or `[]`: All available tools are enabled (when `enableTools` is true)
  - `["tool1", "tool2"]`: Only specified tools are enabled

### Chat Request Examples

#### 1. Enable All Available Tools
```javascript
const chatRequest = {
  message: "Please search for information about AI and crawl relevant websites",
  toolSelection: {
    enableTools: true,
    enabledTools: null  // or []
  }
};

fetch('/research-agent/api/stream-chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(chatRequest)
});
```

#### 2. Enable Specific Tools Only
```javascript
const chatRequest = {
  message: "Please crawl this website for information",
  toolSelection: {
    enableTools: true,
    enabledTools: ["webcrawl"]  // Only webcrawl tool enabled
  }
};

fetch('/research-agent/api/stream-chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(chatRequest)
});
```

#### 3. Disable All Tools
```javascript
const chatRequest = {
  message: "Just answer from your knowledge, don't use any tools",
  toolSelection: {
    enableTools: false,
    enabledTools: null  // Ignored when enableTools is false
  }
};

fetch('/research-agent/api/stream-chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(chatRequest)
});
```

#### 4. No Tool Selection (Default Behavior)
```javascript
// If toolSelection is omitted, all available tools are enabled by default
const chatRequest = {
  message: "Please help me with this task"
  // toolSelection omitted = enableTools: true, enabledTools: null
};

fetch('/research-agent/api/stream-chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(chatRequest)
});
```

## Frontend Implementation Examples

### 1. Tool Discovery and Selection UI

```javascript
class ToolManager {
  constructor() {
    this.availableTools = [];
    this.selectedTools = [];
  }

  // Fetch available tools from backend
  async loadAvailableTools() {
    try {
      const response = await fetch('/research-agent/api/tools');
      this.availableTools = await response.json();
      this.renderToolSelection();
    } catch (error) {
      console.error('Failed to load tools:', error);
    }
  }

  // Render tool selection UI
  renderToolSelection() {
    const container = document.getElementById('tool-selection');
    container.innerHTML = this.availableTools.map(tool => `
      <div class="tool-option">
        <input type="checkbox" 
               id="tool-${tool.name}" 
               value="${tool.name}"
               onchange="toolManager.toggleTool('${tool.name}')">
        <label for="tool-${tool.name}">
          <strong>${tool.name}</strong>
          <span class="tool-description">${tool.description}</span>
          <span class="tool-server">From: ${tool.sourceMcpServerName}</span>
        </label>
      </div>
    `).join('');
  }

  // Toggle tool selection
  toggleTool(toolName) {
    const index = this.selectedTools.indexOf(toolName);
    if (index > -1) {
      this.selectedTools.splice(index, 1);
    } else {
      this.selectedTools.push(toolName);
    }
  }

  // Get current tool selection for requests
  getToolSelection() {
    return {
      enableTools: this.selectedTools.length > 0,
      enabledTools: this.selectedTools.length > 0 ? this.selectedTools : null
    };
  }

  // Refresh tools from backend
  async refreshTools() {
    try {
      const response = await fetch('/research-agent/api/tools/refresh', {
        method: 'POST'
      });
      const result = await response.json();
      console.log('Tools refreshed:', result);
      await this.loadAvailableTools(); // Reload the updated tool list
    } catch (error) {
      console.error('Failed to refresh tools:', error);
    }
  }
}

// Initialize tool manager
const toolManager = new ToolManager();
toolManager.loadAvailableTools();
```

### 2. Chat Integration with Tool Selection

```javascript
class ChatManager {
  constructor(toolManager) {
    this.toolManager = toolManager;
  }

  async sendMessage(message) {
    const toolSelection = this.toolManager.getToolSelection();
    
    const chatRequest = {
      message: message,
      toolSelection: toolSelection
    };

    try {
      const response = await fetch('/research-agent/api/stream-chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(chatRequest)
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // Handle streaming response
      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        this.handleStreamChunk(chunk);
      }
    } catch (error) {
      console.error('Chat request failed:', error);
    }
  }

  handleStreamChunk(chunk) {
    // Process streaming response chunks
    // Look for tool execution messages like "[Calling tool: webcrawl]"
    console.log('Received chunk:', chunk);
    
    // Update UI with streaming content
    this.appendToChat(chunk);
  }

  appendToChat(content) {
    const chatContainer = document.getElementById('chat-messages');
    chatContainer.innerHTML += content;
    chatContainer.scrollTop = chatContainer.scrollHeight;
  }
}

// Initialize chat manager
const chatManager = new ChatManager(toolManager);
```

### 3. Tool Status Monitoring

```javascript
class ToolStatusMonitor {
  constructor() {
    this.checkInterval = 30000; // Check every 30 seconds
    this.startMonitoring();
  }

  async checkToolStatus() {
    try {
      const response = await fetch('/research-agent/api/tools/status');
      const status = await response.json();
      
      this.updateStatusDisplay(status);
      return status;
    } catch (error) {
      console.error('Failed to check tool status:', error);
      this.updateStatusDisplay({ status: 'error', message: error.message });
      return null;
    }
  }

  updateStatusDisplay(status) {
    const statusElement = document.getElementById('tool-status');
    if (statusElement) {
      statusElement.innerHTML = `
        <span class="status-indicator ${status.status}"></span>
        <span class="status-text">
          ${status.status === 'healthy' 
            ? `${status.availableTools} tools available` 
            : `Error: ${status.message}`}
        </span>
      `;
    }
  }

  startMonitoring() {
    // Initial check
    this.checkToolStatus();
    
    // Periodic monitoring
    setInterval(() => {
      this.checkToolStatus();
    }, this.checkInterval);
  }
}

// Initialize status monitoring
const toolStatusMonitor = new ToolStatusMonitor();
```

## Backend Usage (for other services)

If you need to access tool selection functionality from other backend services:

```java
@Service
public class MyService {
    
    @Autowired
    private DynamicIntegrationService dynamicIntegrationService;
    
    @Autowired 
    private OpenAIService openAIService;
    
    public void exampleUsage() {
        // Get available tools
        List<Map<String, Object>> availableTools = dynamicIntegrationService.getDiscoveredMcpTools();
        
        // Create tool selection
        ToolSelectionRequest toolSelection = new ToolSelectionRequest(true, Arrays.asList("webcrawl"));
        
        // Use in streaming request
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("user", "Please crawl example.com", null, null)
        );
        
        Flux<String> response = openAIService.getChatCompletionStreamWithToolExecution(
            messages, "gpt-4", toolSelection);
    }
}
```

## Best Practices

### 1. Tool Selection Strategy
- **Default Approach**: Enable all tools for maximum capability
- **Specific Tasks**: Select only relevant tools to reduce noise
- **Performance**: Fewer tools = faster LLM processing
- **User Control**: Let users choose their preferred tools

### 2. Error Handling
- Always handle tool discovery failures gracefully
- Provide fallback behavior when tools are unavailable
- Show clear error messages to users
- Implement retry mechanisms for transient failures

### 3. UI/UX Considerations
- Show tool execution progress to users
- Display which tools are being used
- Allow easy tool selection/deselection
- Provide tool descriptions and capabilities

### 4. Performance Optimization
- Cache tool lists when appropriate
- Only refresh tools when necessary
- Use tool selection to optimize LLM requests
- Monitor tool execution times

## Troubleshooting

### Common Issues

1. **No tools available**: Check MCP server configuration and connectivity
2. **Tool execution fails**: Verify MCP server health and authentication
3. **Tool selection ignored**: Ensure `ToolSelectionRequest` is properly formatted
4. **Streaming interruptions**: Check WebClient configuration and timeouts

### Debug Information

Enable debug logging:
```properties
logging.level.com.steffenhebestreit.ai_research.Controller.ToolController=DEBUG
logging.level.com.steffenhebestreit.ai_research.Service.DynamicIntegrationService=DEBUG
logging.level.com.steffenhebestreit.ai_research.Service.OpenAIService=DEBUG
```

Check tool status:
```bash
curl http://localhost:8080/research-agent/api/tools/status
```

Refresh tools manually:
```bash
curl -X POST http://localhost:8080/research-agent/api/tools/refresh
```
