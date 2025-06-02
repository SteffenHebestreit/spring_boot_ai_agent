/**
 * REST controllers and API endpoints for the AI Research application's web interface layer.
 * 
 * <p>This package contains comprehensive REST API controllers that provide the web interface
 * for the AI Research Agent system. The controllers implement RESTful services for Agent-to-Agent
 * (A2A) protocol compliance, multimodal content processing, conversation management, task lifecycle
 * operations, and real-time streaming capabilities.</p>
 * 
 * <h3>Core Controller Categories:</h3>
 * <ul>
 * <li><strong>Agent Protocol Controllers:</strong> A2A protocol compliance and agent discovery endpoints</li>
 * <li><strong>Conversation Management:</strong> Chat lifecycle, message handling, and conversation persistence</li>
 * <li><strong>Task Management:</strong> Research task operations with streaming and real-time updates</li>
 * <li><strong>Content Processing:</strong> Multimodal content handling with LLM capability detection</li>
 * <li><strong>Configuration Access:</strong> Dynamic LLM configuration and capability discovery</li>
 * </ul>
 * 
 * <h3>Key Controller Classes:</h3>
 * <ul>
 * <li>{@link com.steffenhebestreit.ai_research.Controller.AgentCardController} - A2A protocol compliance and agent card management</li>
 * <li>{@link com.steffenhebestreit.ai_research.Controller.ChatController} - Conversation management and message operations</li>
 * <li>{@link com.steffenhebestreit.ai_research.Controller.TaskController} - Research task management and processing</li>
 * <li>{@link com.steffenhebestreit.ai_research.Controller.TaskStreamingController} - Real-time task updates via Server-Sent Events</li>
 * <li>{@link com.steffenhebestreit.ai_research.Controller.LlmController} - LLM capability discovery and configuration access</li>
 * </ul>
 * 
 * <h3>A2A Protocol Implementation:</h3>
 * <ul>
 * <li><strong>Agent Discovery:</strong> Standard /.well-known/agent.json endpoint for agent metadata</li>
 * <li><strong>Runtime Configuration:</strong> Dynamic agent card customization via properties and environment variables</li>
 * <li><strong>Template Processing:</strong> Base template loading with runtime property override</li>
 * <li><strong>Environment Awareness:</strong> Profile-specific configurations for testing and production</li>
 * </ul>
 * 
 * <h3>Conversation Management Features:</h3>
 * <ul>
 * <li><strong>Chat Lifecycle:</strong> Complete chat session creation, management, and deletion</li>
 * <li><strong>Message Operations:</strong> Message addition, retrieval, and conversation history</li>
 * <li><strong>Streaming Responses:</strong> Real-time AI response streaming with NDJSON format</li>
 * <li><strong>Title Management:</strong> Automatic and manual chat title generation and updates</li>
 * </ul>
 * 
 * <h3>Task Management Operations:</h3>
 * <ul>
 * <li><strong>Task Creation:</strong> Research task initialization with message content</li>
 * <li><strong>Lifecycle Management:</strong> Task state tracking, cancellation, and completion</li>
 * <li><strong>Message Processing:</strong> Task-specific message handling and AI interactions</li>
 * <li><strong>Artifact Management:</strong> Generated content and deliverable tracking</li>
 * </ul>
 * 
 * <h3>Multimodal Content Processing:</h3>
 * <ul>
 * <li><strong>File Upload Support:</strong> Image and PDF upload with validation and processing</li>
 * <li><strong>Content Transformation:</strong> File-to-data URI conversion for LLM consumption</li>
 * <li><strong>Capability Matching:</strong> Automatic LLM selection based on content type support</li>
 * <li><strong>Streaming Support:</strong> Real-time multimodal content processing with progress updates</li>
 * </ul>
 * 
 * <h3>Real-time Communication:</h3>
 * <ul>
 * <li><strong>Server-Sent Events:</strong> Live task updates and progress streaming</li>
 * <li><strong>JSON-RPC Integration:</strong> Structured request/response patterns for complex operations</li>
 * <li><strong>Reconnection Support:</strong> Client reconnection and state synchronization</li>
 * <li><strong>Event Broadcasting:</strong> Real-time notification system for task status changes</li>
 * </ul>
 * 
 * <h3>LLM Configuration Management:</h3>
 * <ul>
 * <li><strong>Capability Discovery:</strong> Dynamic detection of LLM capabilities and limitations</li>
 * <li><strong>Model Selection:</strong> API endpoints for LLM selection and compatibility checking</li>
 * <li><strong>Configuration Access:</strong> Runtime access to LLM configurations and settings</li>
 * <li><strong>Validation Support:</strong> Content type validation against model capabilities</li>
 * </ul>
 * 
 * <h3>API Design Patterns:</h3>
 * <ul>
 * <li><strong>RESTful Architecture:</strong> Standard REST conventions with proper HTTP methods</li>
 * <li><strong>JSON Communication:</strong> Structured JSON request/response formats</li>
 * <li><strong>Error Handling:</strong> Comprehensive error responses with proper HTTP status codes</li>
 * <li><strong>Content Negotiation:</strong> Support for multiple content types and formats</li>
 * </ul>
 * 
 * <h3>Security and Validation:</h3>
 * <ul>
 * <li><strong>Input Validation:</strong> Comprehensive request validation and sanitization</li>
 * <li><strong>CORS Support:</strong> Cross-origin resource sharing for frontend integration</li>
 * <li><strong>Authentication Ready:</strong> Prepared for external authentication integration</li>
 * <li><strong>Rate Limiting Ready:</strong> Architecture supports rate limiting implementation</li>
 * </ul>
 * 
 * <h3>Response Formats:</h3>
 * <ul>
 * <li><strong>Standard JSON:</strong> Structured JSON responses with consistent formatting</li>
 * <li><strong>Streaming JSON:</strong> NDJSON for real-time streaming responses</li>
 * <li><strong>Server-Sent Events:</strong> SSE format for live updates and notifications</li>
 * <li><strong>Multipart Data:</strong> Support for file uploads and multimodal content</li>
 * </ul>
 * 
 * <h3>Integration Capabilities:</h3>
 * <ul>
 * <li><strong>Frontend Integration:</strong> Optimized for React and modern frontend frameworks</li>
 * <li><strong>External API Integration:</strong> Support for external service calls and integrations</li>
 * <li><strong>MCP Protocol Support:</strong> Ready for Model Context Protocol integrations</li>
 * <li><strong>Agent Communication:</strong> Inter-agent communication via A2A protocol</li>
 * </ul>
 * 
 * <h3>Performance Optimization:</h3>
 * <ul>
 * <li><strong>Streaming Responses:</strong> Non-blocking streaming for large responses</li>
 * <li><strong>Asynchronous Processing:</strong> Non-blocking operations for improved throughput</li>
 * <li><strong>Efficient Serialization:</strong> Optimized JSON serialization with null exclusion</li>
 * <li><strong>Resource Management:</strong> Proper resource cleanup and connection management</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see org.springframework.web.bind.annotation.RestController
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @see org.springframework.http.ResponseEntity
 * @see org.springframework.web.servlet.mvc.method.annotation.SseEmitter
 */
package com.steffenhebestreit.ai_research.Controller;
