package com.steffenhebestreit.ai_research.Util;

import java.util.regex.Pattern;

/**
 * Utility class for filtering AI response content before database persistence.
 * 
 * <p>This utility provides methods to clean AI responses by removing internal reasoning
 * content and tool execution status messages that should not be saved to the database, 
 * such as &lt;think&gt; tags, tool execution notifications, and other
 * LLM-specific artifacts that exceed content limits.</p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 * <li>Removes &lt;think&gt; and &lt;thinking&gt; tags and their internal reasoning content</li>
 * <li>Filters out tool execution status messages (e.g., "[Tool completed successfully]")</li>
 * <li>Removes &lt;tool_code&gt; tags and their content</li>
 * <li>Handles nested tags and multi-line reasoning blocks</li>
 * <li>Preserves the actual response content for user display</li>
 * <li>Ensures filtered content fits within database constraints</li>
 * </ul>
 * 
 * @author AI Research System
 * @version 1.1
 * @since 1.0
 */
public class ContentFilterUtil {
    /**
     * Regex pattern to match <think> and <thinking> tags and their content.
     * Uses DOTALL flag to match across newlines and non-greedy matching
     * to handle multiple think blocks properly.
     */
    private static final Pattern THINK_TAG_PATTERN = Pattern.compile(
        "<think(?:ing)?>.*?</think(?:ing)?>", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    /**
     * Regex pattern to match <tool_code> tags and their content.
     * Uses DOTALL flag to match across newlines and non-greedy matching.
     */
    private static final Pattern TOOL_CODE_TAG_PATTERN = Pattern.compile(
        "<tool_code>.*?</tool_code>", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );    /**
     * Regex pattern to match text about tool execution status like:
     * [Calling tool: X] [Executing tools...] [Tool execution failed: Y] [Tool completed successfully] etc.
     */    private static final Pattern TOOL_EXECUTION_STATUS_PATTERN = Pattern.compile(
        "\\[(?:Calling tool|Executing tools?|Tool execution|Tool result|Tool error|Tool failed|Tool execution failed|Tool completed|Tool completed successfully|Continuing conversation|Step [0-9]+|Using tool|Task complete|Task started|Processing|Tool thinking|Tool output|Result|Executing|Tool execution continues)[^\\]]*\\]", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Maximum content length allowed in database (VARCHAR(30000) limit).
     */
    private static final int MAX_CONTENT_LENGTH = 30000;
      /**
     * Filters AI response content by removing internal reasoning tags, tool code blocks, and tool execution status messages.
     *     * <p>This method removes &lt;think&gt;, &lt;thinking&gt;, &lt;tool_code&gt; tags and their contents, 
     * as well as tool execution status messages like "[Tool completed successfully]". These contain
     * internal LLM reasoning and processing information that should not be persisted to the database. The
     * filtering ensures that only the actual response content is saved.</p>
     * 
     * <h3>Filtering Rules:</h3>
     * <ul>
     * <li>Removes all &lt;think&gt;...&lt;/think&gt; and &lt;thinking&gt;...&lt;/thinking&gt; blocks (case-insensitive)</li>
     * <li>Removes all &lt;tool_code&gt;...&lt;/tool_code&gt; blocks</li>
     * <li>Filters out tool execution status messages (e.g., "[Tool completed successfully]")</li>
     * <li>Handles nested and multi-line blocks</li>
     * <li>Trims whitespace from the result</li>
     * <li>Truncates to maximum allowed length if necessary</li>
     * </ul>
     * 
     * @param content The raw AI response content that may contain think tags, tool code, or tool status messages
     * @return Filtered content safe for database persistence, or empty string if input is null
     *     * @example
     * <pre>
     * String rawResponse = "Here's my answer. <thinking>Let me reason about this...</thinking> [Tool completed successfully] The result is 42.";
     * String filtered = ContentFilterUtil.filterForDatabase(rawResponse);
     * // Result: "Here's my answer. The result is 42."
     * </pre>
     */public static String filterForDatabase(String content) {
        if (content == null) {
            return "";
        }
        
        String filtered = content;

        // Remove all <think>...</think> and <thinking>...</thinking> blocks
        filtered = THINK_TAG_PATTERN.matcher(filtered).replaceAll("");
        
        // Remove all <tool_code>...</tool_code> blocks
        filtered = TOOL_CODE_TAG_PATTERN.matcher(filtered).replaceAll("");
        
        // Remove tool execution status messages
        filtered = TOOL_EXECUTION_STATUS_PATTERN.matcher(filtered).replaceAll("");
        
        // Fix multiple consecutive spaces
        filtered = filtered.replaceAll("\\s{2,}", " ");
        
        // Trim whitespace
        filtered = filtered.trim();
        
        // Ensure content doesn't exceed database limit
        if (filtered.length() > MAX_CONTENT_LENGTH) {
            filtered = filtered.substring(0, MAX_CONTENT_LENGTH);
        }
        
        return filtered;
    }/**
     * Checks if the given content contains think or thinking tags, tool code tags, or tool execution status messages that need filtering.
     * 
     * <p>This method can be used to determine if content needs processing
     * before database persistence, allowing for performance optimization
     * by skipping filtering when not needed.</p>
     *     
     * @param content The content to check for tags or status messages that should be filtered
     * @return true if content contains elements that need filtering, false otherwise
     */
    public static boolean containsThinkTags(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        return THINK_TAG_PATTERN.matcher(content).find() || 
               TOOL_CODE_TAG_PATTERN.matcher(content).find() ||
               TOOL_EXECUTION_STATUS_PATTERN.matcher(content).find();
    }
    
    /**
     * Gets the maximum allowed content length for database persistence.
     * 
     * @return The maximum content length (VARCHAR constraint)
     */
    public static int getMaxContentLength() {
        return MAX_CONTENT_LENGTH;
    }
}
