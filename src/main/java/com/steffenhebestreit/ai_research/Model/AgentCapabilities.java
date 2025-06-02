package com.steffenhebestreit.ai_research.Model;

/**
 * Agent Capabilities model representing the technical capabilities and features supported by an AI agent
 * according to the Agent-to-Agent (A2A) protocol. This class defines the operational characteristics
 * and interaction patterns that an agent can handle, enabling precise capability matching and
 * intelligent request routing in multi-agent systems.
 * 
 * <h3>Core Capability Categories:</h3>
 * <ul>
 *   <li><strong>Communication Patterns</strong> - Streaming responses and real-time interaction support</li>
 *   <li><strong>Notification Systems</strong> - Push notification capabilities for proactive communication</li>
 *   <li><strong>State Management</strong> - History tracking and state transition monitoring</li>
 *   <li><strong>Interaction Complexity</strong> - Multi-step workflows and conversational continuity</li>
 * </ul>
 * 
 * <h3>A2A Protocol Integration:</h3>
 * <ul>
 *   <li><strong>Capability Declaration</strong> - Standard format for advertising agent features</li>
 *   <li><strong>Compatibility Checking</strong> - Enables agents to verify interaction compatibility</li>
 *   <li><strong>Request Routing</strong> - Facilitates intelligent task distribution based on capabilities</li>
 *   <li><strong>Feature Negotiation</strong> - Supports dynamic capability-based interaction patterns</li>
 * </ul>
 * 
 * <h3>Capability Flags:</h3>
 * All capabilities are represented as boolean flags that can be individually enabled or disabled
 * based on the agent's implementation and configuration. These flags are used by the A2A protocol
 * for capability discovery and interaction planning.
 * 
 * <h3>JSON Serialization:</h3>
 * The class is optimized for JSON serialization as part of the agent card specification,
 * providing a standardized format for capability advertisement in the A2A ecosystem.
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see com.steffenhebestreit.ai_research.Model.AgentCard
 * @see com.steffenhebestreit.ai_research.Model.AgentSkill
 */
public class AgentCapabilities {
    /**
     * Indicates whether the agent supports streaming responses for real-time communication.
     * When enabled, the agent can provide incremental responses as data becomes available,
     * enabling responsive user experiences and efficient resource utilization.
     * 
     * <p>Streaming capabilities include:</p>
     * <ul>
     *   <li>Real-time response delivery</li>
     *   <li>Chunked data transmission</li>
     *   <li>Progressive result updates</li>
     *   <li>Server-Sent Events (SSE) support</li>
     * </ul>
     */
    private boolean streaming;
    
    /**
     * Indicates whether the agent supports push notification capabilities for proactive communication.
     * When enabled, the agent can initiate communication with clients or other agents without
     * waiting for explicit requests, enabling event-driven architectures and autonomous operations.
     * 
     * <p>Push notification features include:</p>
     * <ul>
     *   <li>Proactive status updates</li>
     *   <li>Event-driven messaging</li>
     *   <li>Autonomous task completion notifications</li>
     *   <li>WebSocket or similar real-time channels</li>
     * </ul>
     */
    private boolean pushNotifications;
    
    /**
     * Indicates whether the agent maintains state transition history for tracking and auditing.
     * When enabled, the agent records state changes, decision points, and workflow transitions,
     * providing transparency and enabling sophisticated debugging and analytics capabilities.
     * 
     * <p>State history features include:</p>
     * <ul>
     *   <li>Complete interaction timeline tracking</li>
     *   <li>Decision point documentation</li>
     *   <li>State change auditing</li>
     *   <li>Workflow step progression records</li>
     * </ul>
     */
    private boolean stateTransitionHistory;
    
    /**
     * Indicates whether the agent supports multi-step interaction workflows and complex operations.
     * When enabled, the agent can handle sophisticated tasks that require multiple sequential
     * operations, decision points, and intermediate state management across extended interactions.
     * 
     * <p>Multi-step capabilities include:</p>
     * <ul>
     *   <li>Complex workflow orchestration</li>
     *   <li>Sequential task execution</li>
     *   <li>Intermediate result processing</li>
     *   <li>Conditional branching and decision trees</li>
     * </ul>
     */
    private boolean multiStep;
    
    /**
     * Indicates whether the agent supports multi-turn conversational interactions and context retention.
     * When enabled, the agent can maintain conversation context across multiple exchanges,
     * enabling natural dialogue flows and contextual understanding over extended interactions.
     * 
     * <p>Multi-turn features include:</p>
     * <ul>
     *   <li>Conversation context preservation</li>
     *   <li>Reference resolution across turns</li>
     *   <li>Dialogue state management</li>
     *   <li>Contextual response generation</li>
     * </ul>
     */
    private boolean multiTurn;

    // Getters and setters
    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    public boolean isPushNotifications() {
        return pushNotifications;
    }

    public void setPushNotifications(boolean pushNotifications) {
        this.pushNotifications = pushNotifications;
    }

    public boolean isStateTransitionHistory() {
        return stateTransitionHistory;
    }    
    
    public void setStateTransitionHistory(boolean stateTransitionHistory) {
        this.stateTransitionHistory = stateTransitionHistory;
    }
    
    public boolean isMultiStep() {
        return multiStep;
    }

    public void setMultiStep(boolean multiStep) {
        this.multiStep = multiStep;
    }

    public boolean isMultiTurn() {
        return multiTurn;
    }

    public void setMultiTurn(boolean multiTurn) {
        this.multiTurn = multiTurn;
    }
}
