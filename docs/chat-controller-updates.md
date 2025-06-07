# Chat Controller Endpoint Updates

## Enhanced Message Streaming with LLM Selection

The chat message streaming endpoint now supports dynamic LLM selection through a query parameter:

### `POST /research-agent/api/chats/{chatId}/message/stream`

* **Description**: Streams AI responses for a message in an existing chat session. This endpoint now allows you to specify which LLM model to use for generating the response.
* **Parameters**:
  * `chatId` (Path Variable) - The ID of the chat.
  * `llmId` (Query Parameter, Optional) - The ID of the specific LLM to use. If not provided, the default model from configuration is used.
* **Request Body**: Plain text content of the user's message.
* **Produces**: NDJSON chunks of the AI's response.

**Example Usage**:

```bash
# Stream a response using the Google Gemma model
curl -X POST "http://localhost:8080/research-agent/api/chats/c37aa051-873a-467d-b602-3a94f2c9e3d9/message/stream?llmId=google_gemma-3-12b-it@q4_k_s" \
  -H "Content-Type: text/plain" \
  -d "What are the main advantages of transformer models?"
```

The frontend can now dynamically switch between different LLM models for each message by specifying the `llmId` parameter, allowing for more flexibility in model selection based on the task or content type.

## Implementation Changes

The following changes were made to support this feature:

1. Updated `ChatController.streamMessage()` method to accept an optional `llmId` query parameter
2. Modified `OpenAIService.getChatCompletionStream()` to support specifying the model ID
3. Added a model selection parameter to the streaming functionality

These changes allow the application to respect the frontend's model selection when processing streaming requests, ensuring the specified model is used rather than always defaulting to the configuration value.

## Empty Response Handling Improvement

The chat streaming functionality has been enhanced to handle empty AI responses more gracefully:

### Enhanced Error Handling for Tool-Only Responses

* **Description**: When an AI's response consists solely of tool-related content that gets filtered out, the backend now sends a clear error message to the frontend instead of silently not saving the empty message.
* **Technical Implementation**:
  * The streaming endpoint now checks if a response is empty after filtering out tool-related content
  * If empty (but originally contained content), an error message is sent to the frontend: `{"error": "AI response was empty after filtering tool-related content."}`
  * This prevents confusion when all AI content is tool-related and provides clearer feedback to users

**Example Error Response**:
```json
{"error": "AI response was empty after filtering tool-related content."}
```

This improvement helps the frontend handle cases where the AI's response contains only tool calls or tool execution status messages, providing a better user experience.

## System Message Handling Change

The system message (AI role definition) is now handled differently:

### Non-Persistent System Messages

* **Description**: The system message defining the AI's role and current time is no longer saved to the database as part of the chat history.
* **Technical Implementation**:
  * Removed from `ChatService.createChat()` to prevent storage in the database
  * Now dynamically prepended by `OpenAIService` before each LLM call
  * Includes the current timestamp for temporal context
  * This change reduces database storage needs and keeps system messages as transient instructions

This update maintains the same AI behavior while improving database efficiency by not storing identical system messages in every chat.

## Message Model Enhancement

The `Message` model has been enhanced to support tracking which LLM generated each response:

### LLM ID Tracking

* **Description**: The `Message` class now includes an `llmId` field to store the identifier of the language model that generated each agent message.
* **Technical Implementation**:
  * Added `llmId` field with getter/setter to the `Message` class
  * `ChatController` now sets this field when saving agent responses
  * Enables tracking which model generated which response for analysis and comparison

This enhancement allows for better attribution and analysis of responses from different language models.
