package com.steffenhebestreit.ai_research.Model;
import java.util.List;

/**
 * Represents a skill that an AI agent can perform according to the A2A protocol.
 * 
 * Each skill defines a capability of the agent, including its identifier, name,
 * description, tags, examples of use, and supported input/output formats.
 */
public class AgentSkill {
    private String id;
    private String name;
    private String description;    
    private List<String> tags;
    private List<String> examples;
    private List<String> inputModes;
    private List<String> outputModes;
    private String inputFormat;
    private String outputFormat;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public List<String> getInputModes() {
        return inputModes;
    }

    public void setInputModes(List<String> inputModes) {
        this.inputModes = inputModes;
    }

    public List<String> getOutputModes() {
        return outputModes;
    }    
    
    public void setOutputModes(List<String> outputModes) {
        this.outputModes = outputModes;
    }
    
    public String getInputFormat() {
        return inputFormat;
    }
    
    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }
    
    public String getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}
