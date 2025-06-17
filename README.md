# AI Research Agent

A comprehensive Spring Boot application providing advanced AI research capabilities with multimodal content support, dynamic tool integration, and Model Context Protocol (MCP) compliance.

## Features

### Core Capabilities
- **Chat Management**: Persistent chat sessions with message history and real-time streaming responses
- **Multimodal Content**: Full support for text, images, and PDF processing with vision-enabled LLMs
- **Dynamic Tool Integration**: Runtime tool discovery and execution via MCP-compliant servers
- **Task Management**: Research task creation, progress tracking, and artifact management
- **Agent Discovery**: Standards-compliant agent card for automatic discoverability

### Advanced Features
- **Tool Selection**: Frontend-controlled tool selection for fine-grained request customization
- **Real-time Streaming**: Server-sent events with tool execution feedback
- **Raw Content Storage**: Dual-storage system preserving both filtered and unfiltered LLM output
- **MCP Compliance**: 100% Model Context Protocol integration with session management
- **Connection Resilience**: Enhanced WebClient configuration with keep-alive and error recovery

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- OpenAI-compatible API endpoint or local LLM server

### Installation

1. **Clone and build:**
   ```bash
   git clone <repository-url>
   cd ai_research/backend
   mvn clean install
   ```

2. **Set API key:**
   ```bash
   # Windows PowerShell
   $env:OPENAI_API_KEY="your_api_key"
   
   # Linux/macOS
   export OPENAI_API_KEY="your_api_key"
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application:**
   - API Base: `http://localhost:8080/research-agent/api`
   - H2 Console: `http://localhost:8080/h2-console`
   - Agent Card: `http://localhost:8080/.well-known/agent.json`

## Configuration

Key configuration properties in `application.properties`:

### Database
```properties
spring.datasource.url=jdbc:h2:./data/airesearch
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
```

### OpenAI API
```properties
openai.api.baseurl=http://localhost:1234/v1
openai.api.model=gpt-4o
# Set OPENAI_API_KEY environment variable
```

### Agent Identity
```properties
agent.card.id=ai-research-agent-2025
agent.card.name=Research Assistant Agent
agent.card.url=http://localhost:8080/research-agent/api
```

### MCP Integration
```properties
integration.mcp-servers[0].name=webcrawl-mcp
integration.mcp-servers[0].url=https://your-mcp-server.com
integration.mcp-servers[0].auth.type=bearer
# Set MCP_TOKEN environment variable
```

## Core APIs

### Chat Management
- `GET /api/chats` - List all chats
- `POST /api/chats` - Create new chat
- `POST /api/chats/{id}/message/stream` - Stream AI response
- `GET /api/chats/{id}/messages` - Get chat history

### Multimodal Content
- `POST /api/multimodal/upload` - Upload files (images, PDFs)
- `POST /api/multimodal/process` - Process multimodal content
- `POST /api/create-stream-multimodal-chat` - Create and stream multimodal chat in one call

### Tool Management
- `GET /api/tools` - List all available MCP tools
- `POST /api/tools/refresh` - Refresh tool discovery from MCP servers
- `GET /api/tools/status` - Check tool system health

### Task Management
- `POST /api/tasks` - Create research task
- `GET /api/tasks/{id}/stream` - Stream task updates
- `GET /api/tasks/{id}/artifacts` - Get task results

### LLM Configuration
- `GET /api/llms/capabilities` - Available LLM configurations
- `GET /api/llms/models` - Provider models with capabilities

## Documentation

### Essential Guides
- **[MCP Compliance](docs/mcp-compliance.md)** - Complete Model Context Protocol implementation details
- **[MCP Quick Reference](docs/mcp-quick-reference.md)** - Quick start guide for MCP integration
- **[MCP Tool Execution](docs/mcp-tool-execution.md)** - Tool execution workflow documentation
- **[Multimodal Content Support](docs/multimodal-content-support.md)** - Image and PDF processing capabilities
- **[Raw Content Storage](docs/raw-content-storage.md)** - Dual-storage system for LLM outputs

### Architecture

The application follows a layered architecture:

- **Controllers**: REST API endpoints with streaming support
- **Services**: Business logic including AI integration and task management
- **Models**: JPA entities with proper relationships
- **Repositories**: Data access layer with Spring Data JPA
- **Configuration**: Environment-aware configuration management

### Key Components

- **ChatService**: Manages chat sessions and message persistence
- **OpenAIService**: LLM integration with streaming and tool support
- **DynamicIntegrationService**: MCP server discovery and tool execution
- **TaskService**: Research task management with real-time updates
- **MultimodalContentService**: File processing for images and PDFs

## Development

### Building
```bash
mvn clean install
```

### Testing
```bash
mvn test
```

### Running with profiles
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Production Deployment

1. **Database**: Configure PostgreSQL or MySQL for production
2. **Security**: Set up proper authentication and HTTPS
3. **Monitoring**: Enable actuator endpoints for health checks
4. **Scaling**: Configure connection pooling and caching

## License

This project is licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
