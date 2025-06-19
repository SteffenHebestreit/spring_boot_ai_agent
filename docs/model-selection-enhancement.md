# Model Selection Enhancement

## Overview

Enhanced the TaskController to support model selection for regular text chat endpoints, providing the same flexibility that was previously only available for multimodal endpoints.

## Updated Endpoints

### 1. Regular Chat Endpoint (Synchronous)

**Before:**
```
POST /research-agent/api/chat
Content-Type: text/plain

Hello, world!
```

**After (with optional model selection):**
```
POST /research-agent/api/chat?llmId=google/gemma-3-27b
Content-Type: text/plain

Hello, world!
```

### 2. Streaming Chat Endpoint

**Before:**
```
POST /research-agent/api/chat-stream
Content-Type: text/plain

Hello, world!
```

**After (with optional model selection):**
```
POST /research-agent/api/chat-stream?llmId=google/gemma-3-27b
Content-Type: text/plain

Hello, world!
```

## API Parameters

### New Parameter: `llmId` (optional)
- **Type**: Query parameter
- **Required**: No
- **Default**: Uses `openai.api.model` from application.properties (`qwen/qwen3-14b`)
- **Description**: ID of the specific LLM model to use for the request
- **Available Models**: Get list from `/research-agent/api/llms/capabilities`

## Example Usage

### Using Default Model
```bash
curl -X POST "http://localhost:8080/research-agent/api/chat" \
  -H "Content-Type: text/plain" \
  -d "What is the capital of France?"
```

### Using Specific Model
```bash
curl -X POST "http://localhost:8080/research-agent/api/chat?llmId=google/gemma-3-27b" \
  -H "Content-Type: text/plain" \
  -d "What is the capital of France?"
```

### Streaming with Specific Model
```bash
curl -X POST "http://localhost:8080/research-agent/api/chat-stream?llmId=google/gemma-3-27b" \
  -H "Content-Type: text/plain" \
  -d "Tell me a story about AI"
```

## Available Models

To get the list of available models and their capabilities:
```bash
curl "http://localhost:8080/research-agent/api/llms/capabilities"
```

Sample response:
```json
[
  {
    "id": "qwen/qwen3-14b",
    "name": "Qwen 3 14B",
    "supportsText": true,
    "supportsImage": false,
    "supportsPdf": false,
    "supportsFunctionCalling": true,
    "supportsJsonMode": true
  },
  {
    "id": "google/gemma-3-27b",
    "name": "Google/gemma 3 27b",
    "supportsText": true,
    "supportsImage": true,
    "supportsPdf": true,
    "supportsFunctionCalling": true,
    "supportsJsonMode": true
  }
]
```

## Backward Compatibility

✅ **Fully backward compatible**: Existing clients that don't specify `llmId` will continue to work with the default model.

## Implementation Details

- Added optional `@RequestParam(value = "llmId", required = false) String llmId` to both endpoints
- Model selection logic: `String modelId = (llmId != null && !llmId.isEmpty()) ? llmId : defaultLlmId;`
- Updated all `openAIService.getChatCompletion()` calls to pass the selected model ID
- Updated JavaDoc documentation to reflect the new parameter

## Benefits

1. **Consistent API**: Regular text chat now has the same model selection flexibility as multimodal chat
2. **Frontend Flexibility**: Frontend can now choose optimal models for different use cases
3. **Model-Specific Optimization**: Can use vision models for text when planning multimodal interactions
4. **Future-Proof**: Easy to add new models without API changes
