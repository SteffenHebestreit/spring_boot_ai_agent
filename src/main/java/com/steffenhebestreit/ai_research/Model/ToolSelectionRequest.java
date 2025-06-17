package com.steffenhebestreit.ai_research.Model;

import java.util.List;

/**
 * Request model to specify which tools should be enabled when making requests to the LLM.
 * <p>
 * This model provides flexibility for the frontend to control which tools are included
 * in requests to the LLM. Tools can be selected individually by name or controlled
 * through a global enable/disable flag.
 * </p>
 *
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 */
public class ToolSelectionRequest {
    
    /**
     * Whether to enable tools at all. If false, no tools will be included regardless of enabledTools.
     */
    private boolean enableTools = true;
    
    /**
     * List of specific tool names to enable. If null or empty, all available tools will be included
     * when enableTools is true.
     */
    private List<String> enabledTools;
    
    /**
     * Default constructor.
     */
    public ToolSelectionRequest() {
    }
    
    /**
     * Constructor with all fields.
     *
     * @param enableTools Whether tools should be enabled at all
     * @param enabledTools List of specific tool names to enable
     */
    public ToolSelectionRequest(boolean enableTools, List<String> enabledTools) {
        this.enableTools = enableTools;
        this.enabledTools = enabledTools;
    }
    
    /**
     * Gets whether tools should be enabled.
     *
     * @return true if tools should be enabled, false otherwise
     */
    public boolean isEnableTools() {
        return enableTools;
    }
    
    /**
     * Sets whether tools should be enabled.
     *
     * @param enableTools true to enable tools, false to disable them
     */
    public void setEnableTools(boolean enableTools) {
        this.enableTools = enableTools;
    }
    
    /**
     * Gets the list of specific tool names to enable.
     *
     * @return List of tool names to enable, or null if all tools should be enabled
     */
    public List<String> getEnabledTools() {
        return enabledTools;
    }
    
    /**
     * Sets the list of specific tool names to enable.
     *
     * @param enabledTools List of tool names to enable, or null to enable all tools
     */
    public void setEnabledTools(List<String> enabledTools) {
        this.enabledTools = enabledTools;
    }
}
