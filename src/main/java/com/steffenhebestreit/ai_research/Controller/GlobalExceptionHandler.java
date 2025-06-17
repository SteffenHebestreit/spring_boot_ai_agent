package com.steffenhebestreit.ai_research.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for controller exceptions.
 * <p>
 * This class provides centralized exception handling across all controllers 
 * in the application. It defines methods to handle specific exceptions and 
 * return appropriate HTTP responses.
 * <p>
 * Currently handles:
 * <ul>
 *   <li>MaxUploadSizeExceededException - When file uploads exceed the configured size limits</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles MaxUploadSizeExceededException which is thrown when a file upload exceeds
     * the maximum allowed size defined in application properties.
     * <p>
     * Returns a 400 Bad Request response with a descriptive error message explaining the
     * file size limits for different content types.
     * 
     * @param ex The MaxUploadSizeExceededException that was thrown
     * @return ResponseEntity with error details and HTTP 400 status
     */    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "File size exceeded the maximum allowed upload size");
        errorResponse.put("details", "Images must be under 10MB and PDF documents under 20MB. The overall request must be under 25MB.");
        errorResponse.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
