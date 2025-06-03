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
