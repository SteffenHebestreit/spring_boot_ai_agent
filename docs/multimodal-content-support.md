# Multimodal Content Support

This document describes the implementation of multimodal content support in the AI Research application, enabling the system to process and analyze both text and files (images, PDFs) using vision-enabled LLMs.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Components](#components)
  - [Enhanced Message Object](#enhanced-message-object)
  - [LLM Configuration System](#llm-configuration-system)
  - [Capability Detection](#capability-detection)
  - [Content Processing](#content-processing)
  - [API Endpoints](#api-endpoints)
- [Usage Examples](#usage-examples)
- [Testing](#testing)

## Overview

Modern AI models like GPT-4o and Claude 3 have gained the ability to process and understand images and documents alongside text. This multimodal capability allows for much richer interactions, where users can ask questions about visuals, get documents analyzed, or have AI extract information from charts or tables.

Our implementation adds support for:
- Uploading and processing images (JPEG, PNG, etc.)
- Uploading and processing PDF documents
- Automatically detecting which LLM models support which content types
- Warning users when they try to use unsupported content types
- Streaming responses for multimodal content to provide real-time feedback

## Architecture

The multimodal system follows these high-level steps:

1. Frontend allows uploading files alongside text
2. Backend validates the file type and size
3. Backend checks if the selected LLM supports the file type
4. Files are converted to data URIs (base64 encoded)
5. Content is structured into a format the LLM can understand
6. Response from the LLM is processed and returned to the user

## Components

### Enhanced Message Object

The core `Message` class has been enhanced to handle multimodal content:

```java
public class Message {
    private String id;
    private String role;
    private String contentType;
    private Object content; // Can be String or structured multimodal content
    
    // Methods to handle different content types
    public String getContentAsString() {
        if (content instanceof String) {
            return (String) content;
        }
        return String.valueOf(content);
    }
    
    // Other methods and properties...
}
```

For multimodal messages, the `content` field can be structured as an array of content blocks, where each block specifies its type and corresponding data.

### LLM Configuration System

The application includes a configuration system to track LLM capabilities:

```java
public class LlmConfiguration {
    private String id;
    private String name;
    private boolean supportsText = true;
    private boolean supportsImage = false;
    private boolean supportsPdf = false;
    private String notes;
    
    // Builder pattern, getters, setters, etc.
}
```

These configurations are loaded from `application.properties` entries:

```properties
llm.configurations[0].id=gpt-4o
llm.configurations[0].name=OpenAI GPT-4o
llm.configurations[0].supportsText=true
llm.configurations[0].supportsImage=true
llm.configurations[0].supportsPdf=true
llm.configurations[0].notes=Full multimodal support
```

### Capability Detection

The `LlmCapabilityService` provides methods to detect LLM capabilities:

```java
@Service
public class LlmCapabilityService {
    // Methods to retrieve LLM configurations
    public LlmConfiguration getLlmConfiguration(String llmId) {...}
    
    // Methods to check data type support
    public boolean supportsDataType(String llmId, String dataType) {...}
    
    // Methods to get default configurations
    public LlmConfiguration getDefaultLlmConfiguration() {...}
}
```

### Content Processing

The `MultimodalContentService` handles file validation and processing:

```java
@Service
public class MultimodalContentService {
    // Methods to validate files
    public Map<String, Object> validateFile(MultipartFile file, String llmId) {...}
    
    // Methods to convert files to data URIs
    public String fileToDataUri(MultipartFile file) throws IOException {...}
    
    // Methods to create structured content for LLMs
    public Object createMultimodalContent(String prompt, MultipartFile file, String llmId) throws IOException {...}
}
```

### API Endpoints

The multimodal functionality is exposed through dedicated endpoints in the `MultimodalController`:

```java
@RestController
@RequestMapping("/research-agent/api")
public class MultimodalController {
    // Endpoint for non-streaming multimodal processing
    @PostMapping(value = "/chat-multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> chatMultimodal(...) {...}
    
    // Endpoint for streaming multimodal processing
    @PostMapping(value = "/chat-stream-multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<String> chatStreamMultimodal(...) {...}
}
```

### Dynamic LLM Selection

In addition to the multimodal-specific endpoints, the standard chat streaming endpoint now supports dynamic LLM selection:

```java
@PostMapping(value = "/{chatId}/message/stream", 
            consumes = MediaType.TEXT_PLAIN_VALUE, 
            produces = MediaType.APPLICATION_NDJSON_VALUE)
public Flux<String> streamMessage(
        @PathVariable String chatId, 
        @RequestBody String userMessageContent,
        @RequestParam(value = "llmId", required = false) String llmId) {
    // Use specified model or fall back to default
    String modelToUse = (llmId != null && !llmId.isEmpty()) ? 
        llmId : openAIProperties.getModel();
    
    // Process with the selected model
    // ...
}
```

This allows clients to specify which LLM to use for each request, providing more flexibility in model selection based on the task requirements or content type. For complete details, see the [Chat Controller Updates](chat-controller-updates.md) documentation.

## Usage Examples

### Sending a Multimodal Request

**Using cURL:**

```bash
curl -X POST http://localhost:8080/research-agent/api/chat-multimodal \
  -H "Content-Type: multipart/form-data" \
  -F "prompt=What's in this image?" \
  -F "file=@/path/to/image.jpg" \
  -F "llmId=gpt-4o"
```

**Using JavaScript Fetch API:**

```javascript
const formData = new FormData();
formData.append('prompt', 'What does this chart show?');
formData.append('file', fileInput.files[0]);
formData.append('llmId', 'gpt-4o');

fetch('http://localhost:8080/research-agent/api/chat-multimodal', {
  method: 'POST',
  body: formData
})
.then(response => response.text())
.then(result => console.log(result))
.catch(error => console.error('Error:', error));
```

### Checking LLM Capabilities

```javascript
// Check if a model supports images before attempting upload
fetch('http://localhost:8080/research-agent/api/llms/gpt-4o/supports/image')
  .then(response => response.json())
  .then(result => {
    if (result.supported) {
      // Enable image upload
    } else {
      // Show warning or disable upload
    }
  });
```

## Testing

The multimodal functionality is covered by comprehensive tests:

- `LlmConfigurationTest`: Tests for the configuration system
- `MessageTest`: Tests for multimodal content in Message objects
- `LlmCapabilityServiceTest`: Tests for capability detection
- `MultimodalContentServiceTest`: Tests for file validation and processing
- `MultimodalControllerTest`: Tests for the API endpoints

Run the tests with:

```bash
mvn test
```
