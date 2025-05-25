# AI Research Project (!!!UNDER DEVELOPMENT!!!)

A Spring Boot project for AI research.

## Table of Contents

- [About](#about)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
- [Usage](#usage)
  - [Running the application](#running-the-application)
  - [API Endpoints](#api-endpoints)
- [Building](#building)
- [Testing](#testing)
- [Database](#database)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## About

This project, "AI Research Project," is a Spring Boot application designed for AI research purposes. It provides a backend system with capabilities for managing research tasks, interacting with AI models (e.g., OpenAI), and potentially integrating with other AI agents or services.

(Further details can be added based on specific project goals and functionalities.)

## Features

(This section will be more detailed once controller information is available. Based on current context, potential features include:)
- Task management for AI research.
- Interaction with Large Language Models (e.g., OpenAI).
- Agent information display (Agent Card).
- Integration with external Model Context Protocol (MCP) servers.
- Peer-to-peer communication with other A2A (Agent-to-Agent) agents.
- RESTful APIs for interaction.
- Streaming capabilities for real-time data/task updates.

## Getting Started

### Prerequisites

- Java 11 (as specified in `pom.xml`)
- Maven (for building and dependency management)
- Access to an OpenAI compatible API endpoint or a running instance of a local LLM (if using the OpenAI integration).

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/SteffenHebestreit/spring_boot_ai_agent.git # Replace with your actual repository URL
    cd ai_research/backend
    ```

2.  **Build the project:**
    This command compiles the code, runs tests, and packages the application.
    ```bash
    mvn clean install
    ```

### Configuration

The primary configuration file is `src/main/resources/application.properties`. Environment variables can override these properties.

**Key properties to configure:**

-   **Application Name:**
    -   `spring.application.name` (Default: `ai_research`)

-   **Agent Card Details:**
    These properties define the identity and metadata of the AI agent.
    -   `agent.card.id` (Default: `ai-research-agent-2025`)
    -   `agent.card.name` (Default: `Research Assistant Agent`)
    -   `agent.card.description`
    -   `agent.card.url` (Default API base: `http://localhost:8080/research-agent/api`)
    -   `agent.card.provider.organization`
    -   `agent.card.provider.url`
    -   `agent.card.contact_email`

-   **Database (H2 File-based by default):**
    -   `spring.datasource.url` (Default: `jdbc:h2:./data/airesearch`)
    -   `spring.datasource.driverClassName` (Default: `org.h2.Driver`)
    -   `spring.datasource.username` (Default: `sa`)
    -   `spring.datasource.password` (Default: `password`)
    -   `spring.jpa.database-platform` (Default: `org.hibernate.dialect.H2Dialect`)
    -   `spring.jpa.hibernate.ddl-auto` (Default: `update`) - Manages schema updates.
    -   `spring.h2.console.enabled` (Default: `true`) - Enables H2 web console.
    -   `spring.h2.console.path` (Default: `/h2-console`) - Path to H2 console.
    *For production, it is strongly recommended to configure a more robust database system (e.g., PostgreSQL, MySQL) and update these properties accordingly.*

-   **OpenAI API Integration:**
    -   `openai.api.baseurl` (Default: `http://192.168.12.10:1234/v1`) - Base URL for the OpenAI compatible API.
    -   `openai.api.key`: **CRITICAL! This MUST be set as an environment variable `OPENAI_API_KEY` for security reasons. Do not hardcode it in `application.properties`.**
        Example (Linux/macOS): `export OPENAI_API_KEY="your_actual_api_key"`
        Example (Windows PowerShell): `$env:OPENAI_API_KEY="your_actual_api_key"`
    -   `openai.api.model` (Default: `google_gemma-3-12b-it`) - The default model to use.

-   **External MCP Server Integrations:**
    Configure if the agent needs to interact with external MCP servers.
    -   `agent.integrations.mcp-servers[0].name`
    -   `agent.integrations.mcp-servers[0].url`
    -   (Add more servers by incrementing the index)

-   **External A2A Peer Integrations:**
    Configure if the agent needs to communicate with other peer agents.
    -   `agent.integrations.a2a-peers[0].name`
    -   `agent.integrations.a2a-peers[0].url`
    -   (Add more peers by incrementing the index)

**Environment Variable Override:**
Any property in `application.properties` can be overridden by setting an environment variable with a corresponding name. For example, `SPRING_DATASOURCE_URL` overrides `spring.datasource.url`.

## Usage

### Running the application

There are multiple ways to run the Spring Boot application:

1.  **Using Maven Spring Boot plugin (for development):**
    ```bash
    mvn spring-boot:run
    ```

2.  **Running the packaged JAR (after `mvn clean package`):**
    ```bash
    java -jar target/ai_research-0.0.1-SNAPSHOT.jar
    ```
    You can also pass Spring Boot properties via command line:
    ```bash
    java -jar target/ai_research-0.0.1-SNAPSHOT.jar --server.port=8081 --OPENAI_API_KEY="your_key"
    ```

The application will typically start on `http://localhost:8080` unless the `server.port` property is changed.

### API Endpoints

The application exposes RESTful APIs for various functionalities.

#### Agent Card Controller (`AgentCardController.java`)

Base Path: (No specific base path, uses root)

*   **`GET /.well-known/agent.json`**
    *   Description: Serves the agent card JSON, providing metadata about the AI agent. This endpoint is specified by the Agent-to-Agent (A2A) protocol.
    *   Response: `AgentCard` JSON object.

#### Chat Controller (`ChatController.java`)

Base Path: `/research-agent/api/chats`

*   **`GET /`**
    *   Description: Get all chats.
    *   Response: JSON object containing a "result" key with a list of `Chat` objects.
*   **`GET /{chatId}`**
    *   Description: Get a specific chat by its ID.
    *   Parameters: `chatId` (Path Variable) - The ID of the chat to retrieve.
    *   Response: JSON object containing a "result" key with the `Chat` object, or a 404 error if not found.
*   **`POST /create`**
    *   Description: Create a new chat with an initial message.
    *   Request Body: `Message` JSON object (e.g., `{"role": "user", "contentType": "text/plain", "content": "Hello AI"}`)
    *   Response: JSON object containing a "result" key with the created `Chat` object (which includes the initial message).
*   **`POST /{chatId}/messages`**
    *   Description: Add a message to an existing chat. This is typically used by the frontend to save user messages and completed AI responses.
    *   Parameters: `chatId` (Path Variable) - The ID of the chat.
    *   Request Body: `Message` JSON object.
    *   Response: JSON object containing a "result" key with the updated `Chat` object.
*   **`POST /{chatId}/message/stream`**
    *   Description: Stream an AI-generated response to a new user message in a specific chat.
    *   Parameters: `chatId` (Path Variable) - The ID of the chat.
    *   Request Body: `String` (Plain text content of the user's message).
    *   Consumes: `text/plain`
    *   Produces: `application/ndjson` (Newline Delimited JSON, streaming chunks of the AI's response or an error JSON).
*   **`PUT /{chatId}/title`**
    *   Description: Update the title of a chat.
    *   Parameters: `chatId` (Path Variable) - The ID of the chat.
    *   Request Body: JSON object (e.g., `{"title": "New Chat Title"}`)
    *   Response: JSON object containing a "result" key with the updated `Chat` object.
*   **`DELETE /{chatId}`**
    *   Description: Delete a chat by its ID.
    *   Parameters: `chatId` (Path Variable) - The ID of the chat.
    *   Response: JSON object confirming success or an error.
*   **`GET /{chatId}/messages`**
    *   Description: Get all messages for a specific chat and marks the chat as read.
    *   Parameters: `chatId` (Path Variable) - The ID of the chat.
    *   Response: JSON object containing a "result" key with a list of `ChatMessage` objects.
*   **`GET /admin/test`**
    *   Description: Test endpoint restricted to users with the 'ADMIN' role.
    *   Requires: Authentication and 'ADMIN' role.
*   **`GET /user/test`**
    *   Description: Test endpoint restricted to users with the 'USER' role.
    *   Requires: Authentication and 'USER' role.

#### Task Controller (`TaskController.java`)

Base Path: `/research-agent/api`

*   **`POST /chat`**
    *   Description: Processes a single user message and returns a complete AI response (non-streaming). Creates a new chat, adds user message, gets AI response, adds AI response to chat.
    *   Request Body: `String` (Plain text content of the user's message).
    *   Consumes: `text/plain`
    *   Produces: `text/plain` (The AI's response).
*   **`POST /chat-stream`**
    *   Description: Processes a single user message and streams the AI response. Creates a new chat, adds user message, streams AI response, and saves the complete AI response to chat history upon completion.
    *   Request Body: `String` (Plain text content of the user's message).
    *   Consumes: `text/plain`
    *   Produces: `application/ndjson` (Streaming chunks of the AI's response or an error message).
*   **`POST /tasks/create`**
    *   Description: Creates a new research task with an initial message.
    *   Request Body: `Message` JSON object (e.g., `{"role": "user", "contentType": "text/plain", "content": "Start a new research task about X"}`)
    *   Response: JSON object containing a "result" key with the created `Task` object.
*   **`GET /tasks/{taskId}/get`**
    *   Description: Retrieves details of a specific task by its ID.
    *   Parameters: `taskId` (Path Variable) - The ID of the task.
    *   Response: JSON object containing a "result" key with the `Task` object, or a 404 error if not found.
*   **`POST /message/send`**
    *   Description: Adds a message to an existing task.
    *   Request Body: JSON object (e.g., `{"taskId": "task-123", "message": {"role": "user", "contentType": "text/plain", "content": "Additional instruction."}}`)
    *   Response: JSON object containing a "result" key with the updated `Task` object, or a 404 error if the task is not found.
*   **`POST /tasks/{taskId}/cancel`**
    *   Description: Cancels an existing task.
    *   Parameters: `taskId` (Path Variable) - The ID of the task.
    *   Response: JSON object containing a "result" key with the updated `Task` object (reflecting cancellation status), or a 404 error if not found.

#### Task Streaming Controller (`TaskStreamingController.java`)

Base Path: `/api` (Note: This controller uses a different base path `/api` compared to others which use `/research-agent/api`)

*   **`POST /tasks/resubscribe`**
    *   Description: Allows a client to resubscribe to Server-Sent Events (SSE) for a specific task, typically after a disconnect. Immediately sends the current task state.
    *   Request Body: JSON object (e.g., `{"taskId": "task-123", "id": "json-rpc-request-id"}`)
    *   Produces: `text/event-stream` (SSE for task updates).
*   **`POST /message/stream`**
    *   Description: Processes a message for a task and establishes an SSE stream for updates related to that task. This is used for ongoing interactions where the client expects multiple updates.
    *   Request Body: JSON-RPC 2.0 like structure (e.g., `{"jsonrpc": "2.0", "id": "req-1", "method": "message_stream", "params": {"taskId": "task-xyz", "message": {"role": "user", "content": "Analyze this data."}}}`)
        *   The `params` object should contain `taskId` and a `message` object.
    *   Produces: `text/event-stream` (SSE for task updates, including status and artifacts, formatted as JSON-RPC 2.0 responses).

*(Detailed request/response formats, parameters, and authentication requirements for each endpoint should be documented here based on the actual implementation in `*Controller.java` files.)*

## Building

To build the project and create an executable JAR file:

```bash
mvn clean package
```

The resulting JAR file (e.g., `ai_research-0.0.1-SNAPSHOT.jar`) will be located in the `target/` directory. This JAR includes all necessary dependencies and can be run directly.

## Testing

The project includes unit and integration tests.

To run all tests:
```bash
mvn test
```

Test reports are typically generated in the `target/surefire-reports/` directory.
The tests utilize a specific Spring profile named "test" (activated via `@ActiveProfiles("test")` in test classes). This profile uses an H2 in-memory database and other test-specific configurations defined in `src/test/resources/application-test.properties`.

## Database

-   **Development & Default:** The application is configured to use an H2 file-based database by default. Data is stored in the `./data/` directory relative to where the application is run (e.g., `backend/data/airesearch.mv.db`).
    -   The H2 console can be accessed at `http://localhost:8080/h2-console` (if `spring.h2.console.enabled=true`). Use the JDBC URL `jdbc:h2:./data/airesearch`, username `sa`, and password `password` to connect.
-   **Testing:** For automated tests, an H2 in-memory database is used, ensuring tests are isolated and do not persist data between runs.
-   **Production:** For a production environment, it is crucial to switch to a more robust and scalable database system such as PostgreSQL, MySQL, Oracle, or SQL Server. This involves:
    1.  Adding the appropriate JDBC driver dependency to `pom.xml`.
    2.  Updating the `spring.datasource.*` properties in `application.properties` (or via environment variables) to point to the production database instance.
    3.  Ensuring the `spring.jpa.hibernate.ddl-auto` property is set appropriately for production (e.g., `validate` or `none`, with schema managed by migration tools like Flyway or Liquibase).

## Security

The security configuration is defined in `src/main/java/com/steffenhebestreit/ai_research/Configuration/SecurityConfig.java`.

-   **Current Configuration:**
    -   CSRF (Cross-Site Request Forgery) protection is **disabled**.
    -   All HTTP requests (`.anyRequest().permitAll()`) are permitted without authentication by default within this application's security filter chain.
    -   CORS (Cross-Origin Resource Sharing) is enabled and configured to allow requests from `http://localhost:3000` by default, with common HTTP methods and headers permitted. Credentials (cookies, authorization headers) are allowed.

-   **Production Considerations:**
    -   The comments in `SecurityConfig.java` suggest that authentication is intended to be handled by an external system like Keycloak in a production environment.
    -   If Keycloak or a similar external authentication provider is used, ensure the integration is correctly configured and that the Spring Boot application properly validates tokens or assertions from the provider.
    -   If the application is to handle its own authentication or requires more granular authorization, the `SecurityConfig` will need significant enhancements (e.g., configuring authentication providers, defining access rules per endpoint, enabling CSRF with proper token handling).
    -   **HTTPS should be enforced in production** to protect data in transit. This is typically handled at a reverse proxy/load balancer level or can be configured within Spring Boot.
    -   **Sensitive Information:** As highlighted in the Configuration section, API keys (like `OPENAI_API_KEY`) and other secrets (database passwords, etc.) **MUST NOT** be hardcoded in `application.properties` or committed to version control. Use environment variables, Spring Cloud Config, HashiCorp Vault, or other secure secret management solutions.

