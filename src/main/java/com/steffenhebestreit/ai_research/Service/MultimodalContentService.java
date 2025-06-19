package com.steffenhebestreit.ai_research.Service;

import com.steffenhebestreit.ai_research.Model.LlmConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Service for handling multimodal content processing for LLMs.
 * This service is responsible for validating, converting, and preparing files (images, PDFs)
 * for submission to vision-enabled language models.
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Validating file types and sizes against LLM capabilities</li>
 *   <li>Converting files to data URIs (base64 encoded)</li>
 *   <li>Creating properly structured content objects for LLM APIs</li>
 *   <li>Ensuring compatibility between content types and LLM models</li>
 *   <li>Routing PDFs through MCP file converter tools when available</li>
 * </ul>
 * <p>
 * This service works closely with {@link LlmCapabilityService} to determine
 * which files can be processed by which models, and with {@link DynamicIntegrationService}
 * to route PDF content through file converter tools when tool-calling models are used.
 *
 * @see LlmCapabilityService
 * @see DynamicIntegrationService
 * @see com.steffenhebestreit.ai_research.Controller.MultimodalController
 */
@Service
public class MultimodalContentService {
    private static final Logger logger = LoggerFactory.getLogger(MultimodalContentService.class);
      @Value("${multimodal.max-image-size:10MB}")
    private DataSize maxImageSize;
    
    @Value("${multimodal.max-pdf-size:20MB}")
    private DataSize maxPdfSize;

    @Autowired
    private LlmCapabilityService llmCapabilityService;

    @Autowired
    private DynamicIntegrationService dynamicIntegrationService;/**
     * Validates that the provided file can be processed by the specified LLM.
     * <p>
     * This method performs comprehensive validation including:
     * <ul>
     *   <li>File existence and non-empty content</li>
     *   <li>Content type detection and validation</li>
     *   <li>LLM capability checking for the specific file type</li>
     *   <li>File size limits enforcement</li>
     * </ul>
     * <p>
     * The validation considers both the technical limits (file size, supported formats)
     * and the capabilities of the target LLM model.
     * 
     * @param file The multipart file to validate (images: JPG, PNG, etc.; documents: PDF)
     * @param llmId The ID of the LLM that will process the file - must support the file type
     * @return A validation result map containing:
     *         <ul>
     *           <li>"valid" (Boolean) - true if file can be processed</li>
     *           <li>"error" (String) - error message if validation fails</li>
     *         </ul>
     */
    public Map<String, Object> validateFile(MultipartFile file, String llmId) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        
        if (file == null || file.isEmpty()) {
            result.put("error", "File is empty or not provided");
            return result;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            result.put("error", "Unable to determine file type");
            return result;
        }
        
        LlmConfiguration llmConfig = llmCapabilityService.getLlmConfigurationMerged(llmId);
        if (llmConfig == null) {
            // Instead of returning an error, we assume the model is in the process of loading
            // and set a special flag to indicate this situation
            logger.info("Model {} not found in merged configuration (local + provider), assuming it's still loading", llmId);
            result.put("valid", true);  // Allow the request to proceed
            result.put("modelLoading", true);  // Indicate that the model is likely still loading
            result.put("message", "Note: Model " + llmId + " appears to be initializing. The request will be processed, but may take longer than usual.");
            return result;
        }
        
        // Check for image file types
        if (contentType.startsWith("image/")) {
            logger.debug("Validating image upload for model: {}", llmId);
            logger.debug("Model config supports image: {}", llmConfig.isSupportsImage());
            
            if (!llmConfig.isSupportsImage()) {
                // Check if this is a provider model that might support images but we don't have complete info
                boolean isLikelyVisionCapable = isLikelyVisionCapableModel(llmId);
                logger.debug("Model {} likely vision capable (fallback): {}", llmId, isLikelyVisionCapable);
                
                if (isLikelyVisionCapable) {
                    logger.info("Model {} doesn't explicitly declare image support but appears to be vision-capable, allowing request", llmId);
                    result.put("valid", true);
                    result.put("warning", "Model capabilities not fully determined - proceeding based on model name pattern");
                    return result;
                } else {
                    logger.warn("Model {} rejected for image input: not in vision-capable patterns", llmId);
                    result.put("error", "The selected LLM '" + llmConfig.getName() + "' does not support image inputs");
                    return result;
                }
            }
            
            if (file.getSize() > maxImageSize.toBytes()) {
                result.put("error", "Image size exceeds maximum allowed (" + maxImageSize.toMegabytes() + "MB)");
                return result;
            }
        } 
        // Check for PDF file types
        else if (contentType.equals("application/pdf")) {
            if (!llmConfig.isSupportsPdf()) {
                // For PDFs, be more lenient as many models can process them even if not explicitly declared
                logger.info("Model {} doesn't explicitly declare PDF support, but allowing request as many models can handle PDFs", llmId);
                result.put("valid", true);
                result.put("warning", "PDF support not explicitly declared for this model - proceeding with caution");
                return result;
            }
            
            if (file.getSize() > maxPdfSize.toBytes()) {
                result.put("error", "PDF size exceeds maximum allowed (" + maxPdfSize.toMegabytes() + "MB)");
                return result;
            }
        } else {
            result.put("error", "Unsupported file type: " + contentType);
            return result;
        }
        
        // If we reach here, the file is valid
        result.put("valid", true);
        return result;
    }
    
    /**
     * Converts a file to a base64 data URI format for inclusion in LLM requests.
     * <p>
     * This method transforms a multipart file into a data URI format that can be embedded
     * directly in API requests to vision-enabled LLMs. The resulting data URI follows the
     * RFC 2397 standard: "data:[mediatype][;base64],data"
     * <p>
     * The data URI format allows files to be included directly in JSON requests without
     * requiring separate file uploads or external URLs.
     * <p>
     * <b>Example output:</b> {@code data:image/jpeg;base64,/9j/4AAQSkZJRgABA...}
     * 
     * @param file The multipart file to convert (any file type supported by the target LLM)
     * @return A RFC 2397 compliant data URI containing the base64 encoded file content
     * @throws IOException If file reading or encoding fails
     * @throws IllegalArgumentException If file is null or empty
     */
    public String fileToDataUri(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        byte[] fileBytes = file.getBytes();
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);
        
        return "data:" + contentType + ";base64," + base64Content;
    }
    
    /**
     * Converts a PDF file to text content using the MCP file converter tool.
     * <p>
     * This method routes PDF files through the available MCP file converter tool
     * to extract text content. This is the correct flow for tool-calling models
     * where PDFs should be converted to text before being sent to the LLM.
     * 
     * @param file The PDF file to convert
     * @return A map containing:
     *         <ul>
     *           <li>"success" (Boolean) - true if conversion succeeded</li>
     *           <li>"text" (String) - extracted text content if successful</li>
     *           <li>"error" (String) - error message if conversion failed</li>
     *         </ul>
     * @throws IOException If file reading fails
     */
    public Map<String, Object> convertPdfToText(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        
        if (file == null || file.isEmpty()) {
            result.put("error", "PDF file is empty or not provided");
            return result;
        }
        
        // Check if file converter tool is available
        List<Map<String, Object>> availableTools = dynamicIntegrationService.getDiscoveredMcpTools();
        boolean hasFileConverter = availableTools.stream()
            .anyMatch(tool -> "file_to_markdown".equals(tool.get("name")) || 
                             "file_converter".equals(tool.get("name")));
        
        if (!hasFileConverter) {
            result.put("error", "PDF file converter tool not available. Please upload images only or select a vision-capable model for direct PDF processing.");
            return result;
        }
        
        try {
            // Convert file to base64
            byte[] fileBytes = file.getBytes();
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            
            // Prepare parameters for the file converter tool
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("filename", file.getOriginalFilename());
            parameters.put("base64_content", base64Content);
            
            // Try file_to_markdown first, fallback to file_converter
            String toolName = availableTools.stream()
                .anyMatch(tool -> "file_to_markdown".equals(tool.get("name"))) 
                ? "file_to_markdown" 
                : "file_converter";
            
            logger.info("Converting PDF '{}' to text using MCP tool: {}", file.getOriginalFilename(), toolName);
            
            // Execute the tool via MCP
            Map<String, Object> toolResponse = dynamicIntegrationService.executeToolCall(toolName, parameters);
            
            if (toolResponse != null && toolResponse.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> toolResult = (Map<String, Object>) toolResponse.get("result");
                
                // Check if the tool execution was successful
                if (toolResult.containsKey("content")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) toolResult.get("content");
                    
                    if (!content.isEmpty()) {
                        Map<String, Object> firstContent = content.get(0);
                        if ("text".equals(firstContent.get("type"))) {
                            String extractedText = (String) firstContent.get("text");
                            
                            logger.info("Successfully converted PDF '{}' to text ({} characters)", 
                                       file.getOriginalFilename(), extractedText.length());
                            
                            result.put("success", true);
                            result.put("text", extractedText);
                            return result;
                        }
                    }
                }
                
                // If we get here, the tool didn't return expected format
                logger.warn("File converter tool returned unexpected format: {}", toolResult);
                result.put("error", "File converter returned unexpected response format");
                return result;
            } else if (toolResponse != null && toolResponse.containsKey("error")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = (Map<String, Object>) toolResponse.get("error");
                String errorMessage = (String) error.get("message");
                
                logger.error("File converter tool failed for PDF '{}': {}", file.getOriginalFilename(), errorMessage);
                result.put("error", "PDF conversion failed: " + errorMessage);
                return result;
            } else {
                logger.error("File converter tool returned null or invalid response for PDF '{}'", file.getOriginalFilename());
                result.put("error", "PDF conversion service unavailable");
                return result;
            }
            
        } catch (Exception e) {
            logger.error("Error converting PDF '{}' to text via MCP tool: {}", file.getOriginalFilename(), e.getMessage(), e);
            result.put("error", "PDF conversion failed: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Creates a message content array for multimodal LLM requests, including both text and file data.
     * <p>
     * This method constructs a properly formatted content structure for sending multimodal requests
     * to OpenAI-compatible vision LLMs. The resulting structure follows the OpenAI API format with
     * an array of content blocks, each specifying its type and data.
     * <p>
     * The method automatically:
     * <ul>
     *   <li>Validates file compatibility with the target LLM</li>
     *   <li>Routes PDFs through MCP file converter when available (NEW)</li>
     *   <li>Converts the file to a data URI format or text as appropriate</li>
     *   <li>Creates a structured content array with both text and file components</li>
     *   <li>Uses appropriate content block types for the OpenAI API</li>
     * </ul>
     * <p>
     * <b>PDF Processing Enhancement:</b>
     * For PDF files, this method now attempts to convert them to text using the MCP file converter
     * tool if available. This provides better results for tool-calling models compared to sending
     * PDFs as images. If conversion fails, it falls back to the original image_url approach.
     * <p>
     * <b>Output structure examples:</b>
     * <pre>
     * // For images or non-convertible PDFs:
     * [
     *   { "type": "text", "text": "Analyze this image..." },
     *   { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,..." } }
     * ]
     * 
     * // For successfully converted PDFs:
     * [
     *   { "type": "text", "text": "Analyze this content.\n\nContent extracted from PDF...: [converted text]" }
     * ]
     * </pre>
     * 
     * @param prompt The text prompt to include in the request (optional, defaults to "Analyze this content.")
     * @param file The file to include (must be compatible with the specified LLM)
     * @param llmId The ID of the LLM that will process the content (used for validation)
     * @return A structured content object array suitable for OpenAI API multimodal requests
     * @throws IOException If file processing or encoding fails
     * @throws IllegalArgumentException If the file is not compatible with the specified LLM
     * @see #validateFile(MultipartFile, String)
     * @see #convertPdfToText(MultipartFile)
     * @see #fileToDataUri(MultipartFile)
     */    
    public Object createMultimodalContent(String prompt, MultipartFile file, String llmId) throws IOException {
        // First validate the file is compatible with the LLM
        Map<String, Object> validation = validateFile(file, llmId);
        if (!(boolean)validation.get("valid")) {
            throw new IllegalArgumentException((String)validation.get("error"));
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        // Create base text part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt != null ? prompt : "Analyze this content.");
        
        // Handle PDFs by converting to text first (preferred for tool-calling models)
        if (contentType.equals("application/pdf")) {
            logger.info("Processing PDF '{}' for LLM '{}'", file.getOriginalFilename(), llmId);
            
            // Check if this model would benefit from PDF-to-text conversion
            boolean isToolCallingModel = isLikelyToolCallingModel(llmId);
            
            if (isToolCallingModel) {
                logger.info("Model '{}' appears to be tool-calling capable, attempting PDF-to-text conversion", llmId);
            }
            
            Map<String, Object> pdfConversion = convertPdfToText(file);
            
            if ((boolean) pdfConversion.get("success")) {
                // PDF was successfully converted to text
                String extractedText = (String) pdfConversion.get("text");
                
                // Update the text part to include both prompt and extracted text
                String combinedText = (prompt != null ? prompt + "\n\n" : "") + 
                                     "Content extracted from PDF '" + file.getOriginalFilename() + "':\n\n" + 
                                     extractedText;
                textPart.put("text", combinedText);
                
                logger.info("Successfully converted PDF '{}' to text for LLM '{}'. Text length: {} characters", 
                           file.getOriginalFilename(), llmId, extractedText.length());
                
                // Return only text content (no image_url needed)
                return new Object[]{textPart};
            } else {
                // PDF conversion failed, fall back to sending as image_url (for vision models)
                String error = (String) pdfConversion.get("error");
                logger.warn("PDF conversion failed for '{}' with LLM '{}': {}. Falling back to direct PDF transmission to vision model.", 
                           file.getOriginalFilename(), llmId, error);
                
                // Add warning to the prompt
                String warningText = (prompt != null ? prompt + "\n\n" : "") + 
                                   "[WARNING: PDF could not be converted to text via file converter. " +
                                   "Processing as direct file upload to vision model.]";
                textPart.put("text", warningText);
            }
        }
        
        // For images or PDFs that couldn't be converted, use the original image_url approach
        String dataUri = fileToDataUri(file);
        
        Map<String, Object> filePart = new HashMap<>();
        Map<String, Object> urlData = new HashMap<>();
        urlData.put("url", dataUri);
        
        if (contentType.startsWith("image/")) {
            // Standard image format
            filePart.put("type", "image_url");
            filePart.put("image_url", urlData);
        } else if (contentType.equals("application/pdf")) {
            // For PDFs that couldn't be converted, send as image_url with high detail
            filePart.put("type", "image_url"); 
            filePart.put("image_url", urlData);
            urlData.put("detail", "high"); // Request high detail processing for PDFs
        } else {
            // For other file types, use image_url format (most vision models expect this)
            filePart.put("type", "image_url");
            filePart.put("image_url", urlData);
        }
        
        return new Object[]{textPart, filePart};
    }
    
    /**
     * Checks if a model ID suggests tool-calling capabilities.
     * Tool-calling models benefit more from PDF-to-text conversion via MCP tools.
     * 
     * @param modelId The model ID to check
     * @return true if the model ID suggests tool-calling capabilities
     */
    private boolean isLikelyToolCallingModel(String modelId) {
        if (modelId == null) {
            return false;
        }
        
        String normalizedId = modelId.toLowerCase()
            .replaceAll("[\\s\\-\\/]", "");
        
        // Models that are known to have good tool-calling capabilities
        return normalizedId.contains("gpt4") || 
               normalizedId.contains("gpt35") ||
               normalizedId.contains("claude3") ||
               normalizedId.contains("gemini") ||
               normalizedId.contains("mixtral") ||
               normalizedId.contains("llama3") ||
               normalizedId.contains("qwen3") ||
               normalizedId.contains("qwen2") ||
               normalizedId.contains("tool") ||
               normalizedId.contains("function");
    }

    /**
     * Checks if a model ID suggests vision capabilities based on naming patterns.
     * This is used as a fallback when explicit capability information is not available.
     * 
     * @param modelId The model ID to check
     * @return true if the model ID suggests vision capabilities
     */
    private boolean isLikelyVisionCapableModel(String modelId) {
        if (modelId == null) {
            return false;
        }
        
        // Normalize the model ID: remove spaces, hyphens, slashes, and convert to lowercase
        String normalizedId = modelId.toLowerCase()
            .replaceAll("[\\s\\-\\/]", "")  // Remove spaces, hyphens, and slashes
            .replaceAll("\\d+b", "");       // Remove version numbers like "27b", "3b", etc.
        
        // Known vision-capable model patterns (normalized)
        return normalizedId.contains("vision") || 
               normalizedId.contains("gpt4turbo") || 
               normalizedId.contains("gpt4o") || 
               normalizedId.contains("claude3") || 
               normalizedId.contains("gemini") || 
               normalizedId.contains("gemma") ||     // Gemma models support vision
               normalizedId.contains("llava") ||     // LLaVA models are vision-capable
               normalizedId.contains("bakllava") ||  // BakLLaVA variants
               normalizedId.contains("moondream") || // Moondream vision models
               normalizedId.contains("cogvlm") ||    // CogVLM models
               normalizedId.contains("minicpm") ||   // MiniCPM-V models
               normalizedId.contains("qwenvl") ||    // Qwen-VL models
               normalizedId.endsWith("v") ||         // Models ending with 'v' for vision
               normalizedId.contains("multimodal");  // Explicitly multimodal models
    }

    /**
     * Validates a file based on the new frontend format.
     * <p>
     * This method handles the new frontend format where files are sent as:
     * - file: Base64 encoded string of the file content
     * - fileName: String with the file name
     * - fileType: String with the file type (e.g., "application/pdf")
     * - fileSize: Number representing the file size
     * - prompt: String with the text prompt for the LLM
     * <p>
     * It performs the same validation as the existing method but adapts to the new data structure.
     *
     * @param fileData Map containing the file data in the new format
     * @param llmId The ID of the LLM that will process the file
     * @return A validation result map containing:
     *         <ul>
     *           <li>"valid" (Boolean) - true if file can be processed</li>
     *           <li>"error" (String) - error message if validation fails</li>
     *         </ul>
     */
    public Map<String, Object> validateFileFromBase64(Map<String, Object> fileData, String llmId) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        
        if (fileData == null || !fileData.containsKey("file") || !fileData.containsKey("fileType")) {
            result.put("error", "File data is incomplete or not provided");
            return result;
        }
          String base64Content = (String) fileData.get("file");
        String contentType = (String) fileData.get("fileType");
        String fileName = (String) fileData.get("fileName");
        Number fileSize = (Number) fileData.get("fileSize");
        
        if (base64Content == null || base64Content.isEmpty()) {
            result.put("error", "File content is empty or not provided");
            return result;
        }
          // Validate Base64 content early
        try {
            logger.debug("Validating Base64 content for file: {}, original length: {}", fileName, base64Content.length());
            base64Content = validateAndFixBase64Padding(base64Content);
            logger.debug("Base64 validation successful, final length: {}", base64Content.length());
        } catch (IllegalArgumentException e) {
            logger.error("Base64 validation failed for file '{}': {}", fileName, e.getMessage());
            result.put("error", "Invalid Base64 file content: " + e.getMessage());
            return result;
        }
        
        if (contentType == null) {
            result.put("error", "Unable to determine file type");
            return result;
        }
        
        LlmConfiguration llmConfig = llmCapabilityService.getLlmConfigurationMerged(llmId);
        if (llmConfig == null) {
            // Instead of returning an error, we assume the model is in the process of loading
            // and set a special flag to indicate this situation
            logger.info("Model {} not found in merged configuration (local + provider), assuming it's still loading", llmId);
            result.put("valid", true);  // Allow the request to proceed
            result.put("modelLoading", true);  // Indicate that the model is likely still loading
            result.put("message", "Note: Model " + llmId + " appears to be initializing. The request will be processed, but may take longer than usual.");
            return result;
        }
        
        // Check for image file types
        if (contentType.startsWith("image/")) {
            logger.debug("Validating image upload for model: {}", llmId);
            logger.debug("Model config supports image: {}", llmConfig.isSupportsImage());
            
            if (!llmConfig.isSupportsImage()) {
                // Check if this is a provider model that might support images but we don't have complete info
                boolean isLikelyVisionCapable = isLikelyVisionCapableModel(llmId);
                logger.debug("Model {} likely vision capable (fallback): {}", llmId, isLikelyVisionCapable);
                
                if (isLikelyVisionCapable) {
                    logger.info("Model {} doesn't explicitly declare image support but appears to be vision-capable, allowing request", llmId);
                    result.put("valid", true);
                    result.put("warning", "Model capabilities not fully determined - proceeding based on model name pattern");
                    return result;
                } else {
                    logger.warn("Model {} rejected for image input: not in vision-capable patterns", llmId);
                    result.put("error", "The selected LLM '" + llmConfig.getName() + "' does not support image inputs");
                    return result;
                }
            }
            
            if (fileSize != null && fileSize.longValue() > maxImageSize.toBytes()) {
                result.put("error", "Image size exceeds maximum allowed (" + maxImageSize.toMegabytes() + "MB)");
                return result;
            }
        } 
        // Check for PDF file types
        else if (contentType.equals("application/pdf")) {
            if (!llmConfig.isSupportsPdf()) {
                // For PDFs, be more lenient as many models can process them even if not explicitly declared
                logger.info("Model {} doesn't explicitly declare PDF support, but allowing request as many models can handle PDFs", llmId);
                result.put("valid", true);
                result.put("warning", "PDF support not explicitly declared for this model - proceeding with caution");
                return result;
            }
            
            if (fileSize != null && fileSize.longValue() > maxPdfSize.toBytes()) {
                result.put("error", "PDF size exceeds maximum allowed (" + maxPdfSize.toMegabytes() + "MB)");
                return result;
            }
        } else {
            result.put("error", "Unsupported file type: " + contentType);
            return result;
        }
        
        // If we reach here, the file is valid
        result.put("valid", true);
        return result;
    }
    
    /**
     * Creates a data URI from a base64 encoded file content and content type.
     * <p>
     * This method creates a data URI (RFC 2397) from the file's base64 content and content type.
     * 
     * @param base64Content The base64 encoded file content
     * @param contentType The content type of the file
     * @return A RFC 2397 compliant data URI containing the base64 encoded file content
     */
    public String base64ToDataUri(String base64Content, String contentType) {
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return "data:" + contentType + ";base64," + base64Content;
    }
    
    /**
     * Creates a message content array for multimodal LLM requests using the new frontend format.
     * <p>
     * This method adapts to the new frontend format where files are sent as:
     * - file: Base64 encoded string of the file content
     * - fileName: String with the file name
     * - fileType: String with the file type (e.g., "application/pdf")
     * - fileSize: Number representing the file size
     * - prompt: String with the text prompt for the LLM
     * <p>
     * The method constructs a properly formatted content structure for sending multimodal requests
     * to OpenAI-compatible vision LLMs.
     * 
     * @param fileData Map containing the file data in the new format
     * @param llmId The ID of the LLM that will process the content
     * @return A structured content object array suitable for OpenAI API multimodal requests
     * @throws IOException If file processing or encoding fails
     * @throws IllegalArgumentException If the file is not compatible with the specified LLM
     */
    public Object createMultimodalContentFromBase64(Map<String, Object> fileData, String llmId) throws IOException {
        // First validate the file is compatible with the LLM
        Map<String, Object> validation = validateFileFromBase64(fileData, llmId);
        if (!(boolean)validation.get("valid")) {
            throw new IllegalArgumentException((String)validation.get("error"));
        }
          String base64Content = (String) fileData.get("file");
        String contentType = (String) fileData.get("fileType");
        String fileName = (String) fileData.get("fileName");
        String prompt = (String) fileData.get("prompt");
          // Validate and fix Base64 padding
        try {
            logger.debug("Validating Base64 content for multimodal creation: {}, original length: {}", fileName, base64Content.length());
            base64Content = validateAndFixBase64Padding(base64Content);
            logger.debug("Base64 validation successful for multimodal creation, final length: {}", base64Content.length());
        } catch (IllegalArgumentException e) {
            logger.error("Base64 validation failed for multimodal creation '{}': {}", fileName, e.getMessage());
            throw new IllegalArgumentException("Invalid Base64 file content: " + e.getMessage());
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        // Create base text part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt != null ? prompt : "Analyze this content.");
        
        // Handle PDFs by converting to text first (preferred for tool-calling models)
        if (contentType.equals("application/pdf")) {
            logger.info("Processing PDF '{}' for LLM '{}'", fileName, llmId);
            
            // Check if this model would benefit from PDF-to-text conversion
            boolean isToolCallingModel = isLikelyToolCallingModel(llmId);
            
            if (isToolCallingModel) {
                logger.info("Model '{}' appears to be tool-calling capable, attempting PDF-to-text conversion", llmId);
            }
            
            // Prepare parameters for the file converter tool
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("filename", fileName);
            parameters.put("base64_content", base64Content);
            
            // Try file_to_markdown first, fallback to file_converter
            List<Map<String, Object>> availableTools = dynamicIntegrationService.getDiscoveredMcpTools();
            boolean hasFileConverter = availableTools.stream()
                .anyMatch(tool -> "file_to_markdown".equals(tool.get("name")) || 
                                 "file_converter".equals(tool.get("name")));
            
            if (hasFileConverter) {
                String toolName = availableTools.stream()
                    .anyMatch(tool -> "file_to_markdown".equals(tool.get("name"))) 
                    ? "file_to_markdown" 
                    : "file_converter";
                
                logger.info("Converting PDF '{}' to text using MCP tool: {}", fileName, toolName);
                
                try {
                    // Execute the tool via MCP
                    Map<String, Object> toolResponse = dynamicIntegrationService.executeToolCall(toolName, parameters);
                    
                    if (toolResponse != null && toolResponse.containsKey("result")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> toolResult = (Map<String, Object>) toolResponse.get("result");
                        
                        // Check if the tool execution was successful
                        if (toolResult.containsKey("content")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> content = (List<Map<String, Object>>) toolResult.get("content");
                            
                            if (!content.isEmpty()) {
                                Map<String, Object> firstContent = content.get(0);
                                if ("text".equals(firstContent.get("type"))) {
                                    String extractedText = (String) firstContent.get("text");
                                    
                                    logger.info("Successfully converted PDF '{}' to text ({} characters)", 
                                              fileName, extractedText.length());
                                    
                                    // Update the text part to include both prompt and extracted text
                                    String combinedText = (prompt != null ? prompt + "\n\n" : "") + 
                                                        "Content extracted from PDF '" + fileName + "':\n\n" + 
                                                        extractedText;
                                    textPart.put("text", combinedText);
                                    
                                    // Return only text content (no image_url needed)
                                    return new Object[]{textPart};
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error converting PDF '{}' to text via MCP tool: {}", fileName, e.getMessage(), e);
                    
                    // Add warning to the prompt
                    String warningText = (prompt != null ? prompt + "\n\n" : "") + 
                                       "[WARNING: PDF could not be converted to text via file converter. " +
                                       "Processing as direct file upload to vision model.]";
                    textPart.put("text", warningText);
                }
            }
        }
        
        // For images or PDFs that couldn't be converted, use the original image_url approach
        String dataUri = base64ToDataUri(base64Content, contentType);
        
        Map<String, Object> filePart = new HashMap<>();
        Map<String, Object> urlData = new HashMap<>();
        urlData.put("url", dataUri);
        
        if (contentType.startsWith("image/")) {
            // Standard image format
            filePart.put("type", "image_url");
            filePart.put("image_url", urlData);
        } else if (contentType.equals("application/pdf")) {
            // For PDFs that couldn't be converted, send as image_url with high detail
            filePart.put("type", "image_url"); 
            filePart.put("image_url", urlData);
            urlData.put("detail", "high"); // Request high detail processing for PDFs
        } else {
            // For other file types, use image_url format (most vision models expect this)
            filePart.put("type", "image_url");
            filePart.put("image_url", urlData);
        }
        
        return new Object[]{textPart, filePart};
    }

    /**
     * Converts form data containing Base64 file content to a Map format for processing.
     * This method handles the conversion from multipart form data fields to the internal
     * Map structure used by the service methods.
     * 
     * @param file Base64 encoded string of the file content
     * @param fileName String with the file name
     * @param fileType String with the file type (e.g., "application/pdf")
     * @param fileSize Number representing the file size
     * @param prompt String with the text prompt for the LLM
     * @return Map containing the file data in the expected format
     */
    public Map<String, Object> createFileDataFromFormParams(String file, String fileName, String fileType, Number fileSize, String prompt) {
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file", file);
        fileData.put("fileName", fileName);
        fileData.put("fileType", fileType);
        fileData.put("fileSize", fileSize);
        fileData.put("prompt", prompt);
        return fileData;
    }

    /**
     * Validates and fixes Base64 content padding if necessary.
     * <p>
     * Base64 strings must be properly padded with '=' characters to be valid.
     * This method checks if the Base64 string is valid and adds padding if needed.
     * 
     * @param base64Content The Base64 content to validate and fix
     * @return A properly padded and validated Base64 string
     * @throws IllegalArgumentException if the content is not valid Base64
     */
    private String validateAndFixBase64Padding(String base64Content) {
        if (base64Content == null || base64Content.isEmpty()) {
            throw new IllegalArgumentException("Base64 content cannot be null or empty");
        }
        
        // Remove any whitespace or newlines
        String cleanBase64 = base64Content.replaceAll("\\s", "");
        
        // Check if it contains only valid Base64 characters
        if (!cleanBase64.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            throw new IllegalArgumentException("Invalid Base64 characters found");
        }
        
        // Add padding if necessary
        int padding = cleanBase64.length() % 4;
        if (padding != 0) {
            int paddingToAdd = 4 - padding;
            StringBuilder sb = new StringBuilder(cleanBase64);
            for (int i = 0; i < paddingToAdd; i++) {
                sb.append('=');
            }
            cleanBase64 = sb.toString();
        }
        
        // Validate that the padded string can be decoded
        try {
            Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 content: " + e.getMessage());
        }
        
        return cleanBase64;
    }
}
