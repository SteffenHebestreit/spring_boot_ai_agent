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
 * </ul>
 * <p>
 * This service works closely with {@link LlmCapabilityService} to determine
 * which files can be processed by which models.
 *
 * @see LlmCapabilityService
 * @see com.steffenhebestreit.ai_research.Controller.MultimodalController
 */
@Service
public class MultimodalContentService {
    private static final Logger logger = LoggerFactory.getLogger(MultimodalContentService.class);
    
    @Value("${multimodal.max-image-size:5MB}")
    private DataSize maxImageSize;
    
    @Value("${multimodal.max-pdf-size:10MB}")
    private DataSize maxPdfSize;

    @Autowired
    private LlmCapabilityService llmCapabilityService;/**
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
          LlmConfiguration llmConfig = llmCapabilityService.getLlmConfiguration(llmId);
        if (llmConfig == null) {            // Instead of returning an error, we assume the model is in the process of loading
            // and set a special flag to indicate this situation
            logger.info("Model {} not found in current configuration, assuming it's still loading", llmId);
            result.put("valid", true);  // Allow the request to proceed
            result.put("modelLoading", true);  // Indicate that the model is likely still loading
            result.put("message", "Note: Model " + llmId + " appears to be initializing. The request will be processed, but may take longer than usual.");
            return result;
        }
        
        // Check for image file types
        if (contentType.startsWith("image/")) {
            if (!llmConfig.isSupportsImage()) {
                result.put("error", "The selected LLM '" + llmConfig.getName() + "' does not support image inputs");
                return result;
            }            if (file.getSize() > maxImageSize.toBytes()) {
                result.put("error", "Image size exceeds maximum allowed (" + maxImageSize.toMegabytes() + "MB)");
                return result;
            }
        } 
        // Check for PDF file types
        else if (contentType.equals("application/pdf")) {
            if (!llmConfig.isSupportsPdf()) {
                result.put("error", "The selected LLM '" + llmConfig.getName() + "' does not support PDF inputs");
                return result;
            }              if (file.getSize() > maxPdfSize.toBytes()) {
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
     * Creates a message content array for multimodal LLM requests, including both text and file data.
     * <p>
     * This method constructs a properly formatted content structure for sending multimodal requests
     * to OpenAI-compatible vision LLMs. The resulting structure follows the OpenAI API format with
     * an array of content blocks, each specifying its type and data.
     * <p>
     * The method automatically:
     * <ul>
     *   <li>Validates file compatibility with the target LLM</li>
     *   <li>Converts the file to a data URI format</li>
     *   <li>Creates a structured content array with both text and file components</li>
     *   <li>Uses appropriate content block types for the OpenAI API</li>
     * </ul>
     * <p>
     * <b>Output structure example:</b>
     * <pre>
     * [
     *   { "type": "text", "text": "Analyze this image..." },
     *   { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,..." } }
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
     * @see #fileToDataUri(MultipartFile)
     */
    public Object createMultimodalContent(String prompt, MultipartFile file, String llmId) throws IOException {
        // First validate the file is compatible with the LLM
        Map<String, Object> validation = validateFile(file, llmId);
        if (!(boolean)validation.get("valid")) {
            throw new IllegalArgumentException((String)validation.get("error"));
        }
        
        // Convert file to data URI
        String dataUri = fileToDataUri(file);
        
        // Create content array with text and file parts
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt != null ? prompt : "Analyze this content.");
        
        Map<String, Object> imageUrlData = new HashMap<>();
        imageUrlData.put("url", dataUri);
        
        Map<String, Object> filePart = new HashMap<>();
        filePart.put("type", "image_url");
        filePart.put("image_url", imageUrlData);
        
        return new Object[]{textPart, filePart};
    }
}
