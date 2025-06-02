package com.steffenhebestreit.ai_research.Model;
import java.util.List;

/**
 * Model representing individual skills and capabilities of AI agents in the A2A protocol ecosystem.
 * 
 * <p>This class defines a comprehensive skill specification that enables precise capability
 * advertising, discovery, and matching in multi-agent systems. Each skill represents a
 * specific functionality or service that an agent can provide, complete with metadata
 * for intelligent task routing and capability negotiation.</p>
 * 
 * <h3>Core Skill Components:</h3>
 * <ul>
 * <li><strong>Identity:</strong> Unique identification and human-readable naming</li>
 * <li><strong>Description:</strong> Detailed capability and usage information</li>
 * <li><strong>Classification:</strong> Tags and categories for skill organization</li>
 * <li><strong>Examples:</strong> Usage patterns and input/output samples</li>
 * <li><strong>Modality Support:</strong> Input and output format specifications</li>
 * </ul>
 * 
 * <h3>A2A Protocol Integration:</h3>
 * <ul>
 * <li><strong>Capability Discovery:</strong> Enables automatic skill enumeration</li>
 * <li><strong>Task Matching:</strong> Facilitates intelligent task routing to appropriate agents</li>
 * <li><strong>Interoperability:</strong> Standard format for cross-agent skill exchange</li>
 * <li><strong>Negotiation:</strong> Supports capability-based agent selection</li>
 * </ul>
 * 
 * <h3>Skill Categories:</h3>
 * <ul>
 * <li><strong>Content Processing:</strong> Text analysis, summarization, translation</li>
 * <li><strong>Data Analysis:</strong> Statistical analysis, pattern recognition</li>
 * <li><strong>Research:</strong> Literature review, information synthesis</li>
 * <li><strong>Generation:</strong> Content creation, visualization, reporting</li>
 * <li><strong>Communication:</strong> Multi-agent coordination, messaging</li>
 * </ul>
 * 
 * <h3>Modality Support:</h3>
 * <ul>
 * <li><strong>Text:</strong> Plain text, formatted text, structured documents</li>
 * <li><strong>Visual:</strong> Images, charts, diagrams, videos</li>
 * <li><strong>Audio:</strong> Speech, music, sound effects</li>
 * <li><strong>Data:</strong> Structured data, APIs, databases</li>
 * <li><strong>Multimodal:</strong> Combined text, image, and audio processing</li>
 * </ul>
 * 
 * <h3>Usage Patterns:</h3>
 * <ul>
 * <li>Agent capability advertising in agent cards</li>
 * <li>Task requirement matching and agent selection</li>
 * <li>Skill-based load balancing and routing</li>
 * <li>Capability gap analysis and agent composition</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see AgentCard
 * @see AgentCapabilities
 */
public class AgentSkill {
    /**
     * Unique identifier for the skill within the agent's capability set.
     * 
     * <p>Provides a stable, unique identifier for this specific skill that can
     * be referenced in task requests, capability negotiations, and agent
     * coordination protocols. Should be consistent across agent deployments.</p>
     * 
     * <h3>ID Guidelines:</h3>
     * <ul>
     * <li>Use descriptive, kebab-case identifiers</li>
     * <li>Include domain context (e.g., "text-summarization", "image-analysis")</li>
     * <li>Maintain consistency across agent versions</li>
     * <li>Avoid spaces and special characters</li>
     * </ul>
     */
    private String id;
    
    /**
     * Human-readable name for the skill.
     * 
     * <p>Provides a clear, descriptive name that humans can understand and
     * use to identify the skill's purpose. Used in user interfaces, logs,
     * and documentation for skill identification and selection.</p>
     * 
     * <h3>Naming Best Practices:</h3>
     * <ul>
     * <li>Use clear, actionable names (e.g., "Text Summarization", "Image Analysis")</li>
     * <li>Include domain context when helpful</li>
     * <li>Keep names concise but descriptive</li>
     * <li>Use title case for consistency</li>
     * </ul>
     */
    private String name;
    
    /**
     * Detailed description of the skill's capabilities and usage.
     * 
     * <p>Provides comprehensive information about what the skill does, how it
     * works, and when it should be used. This description helps other agents
     * and users understand the skill's purpose and capabilities for intelligent
     * task routing and agent selection.</p>
     * 
     * <h3>Description Content:</h3>
     * <ul>
     * <li>Primary functionality and purpose</li>
     * <li>Input requirements and constraints</li>
     * <li>Output format and characteristics</li>
     * <li>Performance characteristics and limitations</li>
     * <li>Best use cases and scenarios</li>
     * </ul>
     */
    private String description;
    
    /**
     * Tags for skill categorization and discovery.
     * 
     * <p>Provides a flexible tagging system for organizing and discovering
     * skills across agents. Tags enable efficient skill search, filtering,
     * and categorization in multi-agent environments.</p>
     * 
     * <h3>Tag Categories:</h3>
     * <ul>
     * <li><strong>Domain:</strong> "nlp", "vision", "data-science", "research"</li>
     * <li><strong>Type:</strong> "analysis", "generation", "transformation", "synthesis"</li>
     * <li><strong>Format:</strong> "text", "image", "audio", "multimodal"</li>
     * <li><strong>Complexity:</strong> "simple", "advanced", "expert-level"</li>
     * </ul>
     */
    private List<String> tags;
    
    /**
     * Example usage patterns and scenarios for the skill.
     * 
     * <p>Provides concrete examples of how the skill can be used, including
     * sample inputs, expected outputs, and common use cases. These examples
     * help other agents and users understand the skill's practical applications.</p>
     * 
     * <h3>Example Types:</h3>
     * <ul>
     * <li>Sample input/output pairs</li>
     * <li>Common use case descriptions</li>
     * <li>Integration patterns with other skills</li>
     * <li>Performance benchmarks and expectations</li>
     * </ul>
     */
    private List<String> examples;
    
    /**
     * Supported input modalities and formats.
     * 
     * <p>Specifies the types of input content that this skill can process,
     * enabling precise capability matching for task routing. Input modes
     * define the content types and formats that the skill accepts.</p>
     * 
     * <h3>Common Input Modes:</h3>
     * <ul>
     * <li><code>text</code> - Plain text, formatted text, documents</li>
     * <li><code>image</code> - JPEG, PNG, WebP, and other image formats</li>
     * <li><code>audio</code> - MP3, WAV, and other audio formats</li>
     * <li><code>video</code> - MP4, AVI, and other video formats</li>
     * <li><code>data</code> - JSON, CSV, XML, and structured data</li>
     * <li><code>multimodal</code> - Combined content types</li>
     * </ul>
     */
    private List<String> inputModes;
    
    /**
     * Supported output modalities and formats.
     * 
     * <p>Specifies the types of output content that this skill produces,
     * enabling consumers to understand what to expect from skill execution.
     * Output modes define the content types and formats that the skill generates.</p>
     * 
     * <h3>Common Output Modes:</h3>
     * <ul>
     * <li><code>text</code> - Generated text, summaries, reports</li>
     * <li><code>image</code> - Generated images, charts, visualizations</li>
     * <li><code>audio</code> - Generated speech, music, sound effects</li>
     * <li><code>data</code> - Structured analysis results, extracted data</li>
     * <li><code>visualization</code> - Charts, graphs, interactive displays</li>
     * </ul>
     */
    private List<String> outputModes;
    
    /**
     * Specific input format specification and schema.
     * 
     * <p>Provides detailed specification of the expected input format,
     * including schema definitions, required fields, and format constraints.
     * This enables precise input validation and proper skill integration.</p>
     * 
     * <h3>Format Specifications:</h3>
     * <ul>
     * <li>JSON schema for structured inputs</li>
     * <li>MIME type specifications for media content</li>
     * <li>Field requirements and constraints</li>
     * <li>Validation rules and error handling</li>
     * </ul>
     */
    private String inputFormat;
    
    /**
     * Specific output format specification and schema.
     * 
     * <p>Provides detailed specification of the output format that consumers
     * can expect from this skill, including schema definitions, field descriptions,
     * and format guarantees. This enables proper output processing and integration.</p>
     * 
     * <h3>Format Specifications:</h3>
     * <ul>
     * <li>JSON schema for structured outputs</li>
     * <li>MIME type specifications for generated content</li>
     * <li>Field descriptions and data types</li>
     * <li>Format consistency guarantees</li>
     * </ul>
     */
    private String outputFormat;

    // Getters and setters with comprehensive documentation

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
