package com.steffenhebestreit.ai_research.Model;

/**
 * Represents the status of a task according to the A2A protocol.
 * 
 * This class encapsulates the current state of a task (e.g., PENDING, PROCESSING, COMPLETED)
 * along with an optional status message providing additional details about the task's status.
 */
public class TaskStatus {
    private String state;
    private String message;

    /**
     * Default constructor.
     */
    public TaskStatus() {
    }

    /**
     * Creates a new TaskStatus with the specified state and message.
     * 
     * @param state The state of the task (e.g., PENDING, PROCESSING, COMPLETED)
     * @param message A descriptive message about the task's status
     */
    public TaskStatus(String state, String message) {
        this.state = state;
        this.message = message;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
