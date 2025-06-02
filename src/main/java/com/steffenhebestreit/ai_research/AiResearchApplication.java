package com.steffenhebestreit.ai_research;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the AI Research Agent system.
 * 
 * <p>This application provides a comprehensive AI research agent with Agent-to-Agent (A2A)
 * protocol compliance, enabling integration with other AI systems, MCP servers, and
 * multimodal content processing capabilities. The system is designed for research task
 * automation, content analysis, and intelligent information synthesis.</p>
 * 
 * <h3>Core System Capabilities:</h3>
 * <ul>
 * <li><strong>A2A Protocol Compliance:</strong> Full implementation of Agent-to-Agent communication standards</li>
 * <li><strong>Task Management:</strong> Comprehensive lifecycle management for research tasks</li>
 * <li><strong>MCP Integration:</strong> Dynamic tool discovery from Model Context Protocol servers</li>
 * <li><strong>LLM Integration:</strong> OpenAI-compatible API support with streaming responses</li>
 * <li><strong>Multimodal Processing:</strong> Support for text, images, and document analysis</li>
 * <li><strong>Real-time Communication:</strong> Server-Sent Events for live updates</li>
 * </ul>
 * 
 * <h3>Technical Architecture:</h3>
 * <ul>
 * <li><strong>Spring Boot Framework:</strong> Enterprise-grade application foundation</li>
 * <li><strong>Reactive Streams:</strong> Non-blocking I/O with WebFlux integration</li>
 * <li><strong>JPA Integration:</strong> Persistent data management with Hibernate</li>
 * <li><strong>REST APIs:</strong> RESTful service interfaces with OpenAPI documentation</li>
 * <li><strong>WebSocket Support:</strong> Real-time bidirectional communication</li>
 * <li><strong>Configuration Management:</strong> Externalized configuration with Spring profiles</li>
 * </ul>
 * 
 * <h3>Integration Ecosystem:</h3>
 * <ul>
 * <li><strong>Agent Discovery:</strong> Automatic peer agent discovery via /.well-known/agent.json</li>
 * <li><strong>Capability Exchange:</strong> Dynamic capability sharing with other agents</li>
 * <li><strong>Tool Integration:</strong> MCP server tool discovery and execution</li>
 * <li><strong>Authentication:</strong> Multi-protocol authentication including Keycloak</li>
 * </ul>
 * 
 * <h3>Research Features:</h3>
 * <ul>
 * <li><strong>Literature Analysis:</strong> Academic paper processing and synthesis</li>
 * <li><strong>Data Processing:</strong> Statistical analysis and visualization</li>
 * <li><strong>Content Generation:</strong> AI-powered research report creation</li>
 * <li><strong>Knowledge Synthesis:</strong> Multi-source information integration</li>
 * </ul>
 * 
 * <h3>Deployment Characteristics:</h3>
 * <ul>
 * <li><strong>Containerization:</strong> Docker-ready with embedded Tomcat</li>
 * <li><strong>Scalability:</strong> Horizontal scaling support with stateless design</li>
 * <li><strong>Monitoring:</strong> Built-in metrics and health check endpoints</li>
 * <li><strong>Security:</strong> Comprehensive authentication and authorization</li>
 * </ul>
 * 
 * <h3>Configuration Requirements:</h3>
 * <ul>
 * <li><strong>LLM Configuration:</strong> OpenAI-compatible API endpoint and credentials</li>
 * <li><strong>Database Setup:</strong> JPA-compatible database configuration</li>
 * <li><strong>Integration Config:</strong> MCP servers and A2A peer configurations</li>
 * <li><strong>Security Config:</strong> Authentication provider setup</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <ul>
 * <li><strong>Research Automation:</strong> Automated literature reviews and analysis</li>
 * <li><strong>Multi-Agent Workflows:</strong> Collaborative task processing with peer agents</li>
 * <li><strong>Content Processing:</strong> Document analysis and information extraction</li>
 * <li><strong>Knowledge Integration:</strong> Cross-domain information synthesis</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see com.steffenhebestreit.ai_research.Service.TaskService
 * @see com.steffenhebestreit.ai_research.Service.OpenAIService
 * @see com.steffenhebestreit.ai_research.Service.DynamicIntegrationService
 * @see com.steffenhebestreit.ai_research.Controller.TaskStreamingController
 */
@SpringBootApplication
public class AiResearchApplication {

    /**
     * Main entry point for the AI Research Agent application.
     * 
     * <p>Initializes the Spring Boot application context and starts the embedded
     * web server. The application automatically configures all necessary components
     * including database connections, MCP integrations, and A2A protocol handlers.</p>
     * 
     * <h3>Startup Process:</h3>
     * <ul>
     * <li><strong>Configuration Loading:</strong> Reads application properties and YAML files</li>
     * <li><strong>Component Scanning:</strong> Discovers and initializes Spring components</li>
     * <li><strong>Database Initialization:</strong> Sets up JPA entities and database schema</li>
     * <li><strong>Integration Setup:</strong> Initializes MCP and A2A integrations</li>
     * <li><strong>Server Startup:</strong> Starts embedded Tomcat server</li>
     * <li><strong>Endpoint Registration:</strong> Registers REST and WebSocket endpoints</li>
     * </ul>
     * 
     * <h3>Runtime Environment:</h3>
     * <p>The application supports multiple Spring profiles for different deployment
     * environments (development, staging, production) with environment-specific
     * configurations and security settings.</p>
     * 
     * @param args Command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(AiResearchApplication.class, args);
    }
}
