package com.steffenhebestreit.ai_research.Model;

/**
 * Domain model representing task status information in the AI Research system.
 * 
 * <p>This class encapsulates comprehensive task state information including
 * standardized status values and descriptive messages. It implements the
 * Agent-to-Agent (A2A) protocol specifications for task status reporting
 * and provides structured status communication capabilities.</p>
 * 
 * <h3>Core Components:</h3>
 * <ul>
 * <li><strong>State Management:</strong> Standardized task state enumeration</li>
 * <li><strong>Descriptive Messaging:</strong> Human-readable status descriptions</li>
 * <li><strong>A2A Compliance:</strong> Protocol-compliant status reporting</li>
 * <li><strong>Status Communication:</strong> Structured information exchange</li>
 * </ul>
 * 
 * <h3>Standard Task States:</h3>
 * <ul>
 * <li><code>PENDING</code> - Task created and awaiting processing</li>
 * <li><code>PROCESSING</code> - Task actively being processed</li>
 * <li><code>COMPLETED</code> - Task successfully completed</li>
 * <li><code>FAILED</code> - Task processing encountered an error</li>
 * <li><code>CANCELLED</code> - Task was cancelled before completion</li>
 * </ul>
 * 
 * <h3>Message Guidelines:</h3>
 * <ul>
 * <li><strong>Descriptive:</strong> Provide clear, actionable status information</li>
 * <li><strong>User-Friendly:</strong> Write messages suitable for end-user display</li>
 * <li><strong>Informative:</strong> Include relevant details about current progress</li>
 * <li><strong>Consistent:</strong> Maintain consistent messaging patterns</li>
 * </ul>
 * 
 * <h3>A2A Protocol Integration:</h3>
 * <p>Supports standard A2A task status reporting patterns enabling
 * interoperability with other agents and systems in the research ecosystem.</p>
 * 
 * <h3>Usage Patterns:</h3>
 * <ul>
 * <li>Real-time status updates via streaming endpoints</li>
 * <li>Task progress reporting in user interfaces</li>
 * <li>Service integration and monitoring</li>
 * <li>Error reporting and debugging information</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see Task
 */
public class TaskStatus {
    /**
     * The current state of the task using standardized state values.
     * 
     * <p>This field represents the primary status classification using
     * standardized values such as PENDING, PROCESSING, COMPLETED, FAILED,
     * or CANCELLED. The state provides a machine-readable status indicator
     * for programmatic processing and workflow management.</p>
     * 
     * @see #getState()
     * @see #setState(String)
     */
    private String state;
    
    /**
     * Human-readable message providing additional status details and context.
     * 
     * <p>This field contains descriptive text that provides meaningful
     * information about the current task status, progress details, error
     * descriptions, or completion summaries. Messages should be user-friendly
     * and suitable for display in user interfaces.</p>
     * 
     * @see #getMessage()
     * @see #setMessage(String)
     */
    private String message;

    /**
     * Default constructor for framework and serialization support.
     * 
     * <p>Creates an empty TaskStatus instance suitable for framework
     * instantiation, JSON deserialization, and JPA entity management.
     * Fields should be populated using setter methods after construction.</p>
     */
    public TaskStatus() {
    }

    /**
     * Creates a new TaskStatus with specified state and descriptive message.
     * 
     * <p>Initializes a complete task status with both state enumeration and
     * human-readable description. This constructor enables immediate creation
     * of fully-formed status objects for task reporting and communication.</p>
     * 
     * <h3>Parameter Guidelines:</h3>
     * <ul>
     * <li><strong>state:</strong> Use standard state values (PENDING, PROCESSING, etc.)</li>
     * <li><strong>message:</strong> Provide clear, actionable status description</li>
     * </ul>
     * 
     * <h3>Usage Examples:</h3>
     * <pre>
     * new TaskStatus("PROCESSING", "Analyzing document content...")
     * new TaskStatus("COMPLETED", "Successfully generated research summary")
     * new TaskStatus("FAILED", "Unable to process due to invalid input format")
     * </pre>
     * 
     * @param state The standardized state of the task (PENDING, PROCESSING, COMPLETED, etc.)
     * @param message A descriptive message providing additional status details
     */
    public TaskStatus(String state, String message) {
        this.state = state;
        this.message = message;
    }

    /**
     * Returns the current standardized state of the task.
     * 
     * <p>Retrieves the machine-readable status classification that indicates
     * the current phase of task processing. This state value is used for
     * programmatic decision making and workflow coordination.</p>
     * 
     * @return The current task state (PENDING, PROCESSING, COMPLETED, etc.)
     * @see #setState(String)
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the standardized state of the task.
     * 
     * <p>Updates the machine-readable status classification to reflect
     * the current phase of task processing. This method should be used
     * to transition tasks through their lifecycle states.</p>
     * 
     * @param state The new standardized state (PENDING, PROCESSING, COMPLETED, etc.)
     * @see #getState()
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Returns the human-readable status message.
     * 
     * <p>Retrieves the descriptive text that provides additional context
     * and details about the current task status. This message is intended
     * for display in user interfaces and logging systems.</p>
     * 
     * @return The descriptive status message, or null if no message is set
     * @see #setMessage(String)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the human-readable status message.
     * 
     * <p>Updates the descriptive text that provides additional context
     * about the current task status. Messages should be clear, informative,
     * and suitable for end-user display.</p>
     * 
     * @param message The descriptive status message to set
     * @see #getMessage()
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
