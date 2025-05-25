package com.steffenhebestreit.ai_research.Model;

/**
 * Represents the capabilities of an AI agent according to the A2A protocol.
 * 
 * This class defines what features the agent supports, such as streaming responses,
 * push notifications, state history tracking, multi-step interactions, and multi-turn conversations.
 */
public class AgentCapabilities {
    private boolean streaming;
    private boolean pushNotifications;
    private boolean stateTransitionHistory;
    private boolean multiStep;
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
