package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Service.DynamicIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing and retrieving available tools.
 * 
 * <p>This controller provides endpoints for the frontend to discover available
 * MCP tools and manage tool capabilities. It serves as the bridge between
 * the frontend tool selection UI and the backend tool discovery system.</p>
 * 
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li><strong>GET /research-agent/api/tools</strong> - List all available tools</li>
 *   <li><strong>POST /research-agent/api/tools/refresh</strong> - Refresh tool discovery</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.1.0
 */
@RestController
@RequestMapping("/research-agent/api/tools")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ToolController {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolController.class);
    
    @Autowired
    private DynamicIntegrationService dynamicIntegrationService;
    
    /**
     * Retrieves the list of all available MCP tools.
     * 
     * <p>This endpoint returns all tools discovered from configured MCP servers,
     * including their metadata, parameter schemas, and source server information.
     * The frontend can use this information to build tool selection interfaces
     * and display available capabilities to users.</p>
     * 
     * <h3>Response Format:</h3>
     * <p>Returns an array of tool objects, each containing:</p>
     * <ul>
     *   <li><strong>name</strong> - Tool identifier used for selection</li>
     *   <li><strong>description</strong> - Human-readable tool description</li>
     *   <li><strong>sourceMcpServerName</strong> - Name of the providing MCP server</li>
     *   <li><strong>sourceMcpServerUrl</strong> - URL of the providing MCP server</li>
     *   <li><strong>function</strong> - OpenAI-formatted function definition with parameters</li>
     * </ul>
     * 
     * @return ResponseEntity containing the list of available tools
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAvailableTools() {
        try {
            logger.info("Fetching available MCP tools for frontend");
            List<Map<String, Object>> tools = dynamicIntegrationService.getDiscoveredMcpTools();
            
            logger.info("Successfully retrieved {} available tools", tools.size());
            if (logger.isDebugEnabled()) {
                tools.forEach(tool -> {
                    String toolName = (String) tool.get("name");
                    String serverName = (String) tool.get("sourceMcpServerName");
                    logger.debug("Available tool: '{}' from server '{}'", toolName, serverName);
                });
            }
            
            return ResponseEntity.ok(tools);
        } catch (Exception e) {
            logger.error("Error fetching available tools for frontend", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Triggers a refresh of tool discovery from all configured MCP servers.
     * 
     * <p>This endpoint forces the system to re-discover tools from all configured
     * MCP servers. This is useful when:</p>
     * <ul>
     *   <li>New MCP servers have been added to the configuration</li>
     *   <li>Existing MCP servers have been updated with new tools</li>
     *   <li>Tool discovery failed previously and needs to be retried</li>
     *   <li>The frontend needs to ensure it has the latest tool information</li>
     * </ul>
     * 
     * @return ResponseEntity indicating success or failure of the refresh operation
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshTools() {
        try {
            logger.info("Refreshing MCP tool discovery as requested by frontend");
            dynamicIntegrationService.refreshDynamicCapabilities();
            
            // Get the updated tool count
            List<Map<String, Object>> tools = dynamicIntegrationService.getDiscoveredMcpTools();
            int toolCount = tools.size();
            
            logger.info("Tool refresh completed successfully. {} tools now available", toolCount);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Tools refreshed successfully",
                "toolCount", toolCount,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error refreshing MCP tools", e);
            
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "message", "Failed to refresh tools: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for the tool management system.
     * 
     * <p>Provides basic status information about the tool discovery system,
     * including the number of available tools and the status of MCP server connections.</p>
     * 
     * @return ResponseEntity containing system status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getToolSystemStatus() {
        try {
            List<Map<String, Object>> tools = dynamicIntegrationService.getDiscoveredMcpTools();
            
            Map<String, Object> status = Map.of(
                "status", "healthy",
                "availableTools", tools.size(),
                "timestamp", System.currentTimeMillis(),
                "version", "1.1.0"
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error checking tool system status", e);
            
            Map<String, Object> errorStatus = Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorStatus);
        }
    }
}
