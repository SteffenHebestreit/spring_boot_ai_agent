package com.steffenhebestreit.ai_research.Model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing task-generated artifacts in the AI Research system.
 * 
 * <p>This class encapsulates outputs and deliverables produced by task processing,
 * including research reports, analyses, summaries, generated content, and other
 * AI-produced artifacts. It implements the Agent-to-Agent (A2A) protocol
 * specifications for artifact management and content delivery.</p>
 * 
 * <h3>Core Components:</h3>
 * <ul>
 * <li><strong>Artifact Identity:</strong> Unique UUID-based identification</li>
 * <li><strong>Type Classification:</strong> Semantic categorization of artifact purpose</li>
 * <li><strong>Content Management:</strong> MIME type specification and content storage</li>
 * <li><strong>Temporal Tracking:</strong> Creation timestamp for lifecycle management</li>
 * </ul>
 * 
 * <h3>Artifact Types:</h3>
 * <ul>
 * <li><code>literature-review</code> - Academic literature analysis and synthesis</li>
 * <li><code>data-analysis</code> - Statistical and data processing results</li>
 * <li><code>research-summary</code> - Condensed research findings and conclusions</li>
 * <li><code>generated-content</code> - AI-created text, images, or multimedia</li>
 * <li><code>report</code> - Structured research reports and documentation</li>
 * <li><code>visualization</code> - Charts, graphs, and visual representations</li>
 * </ul>
 * 
 * <h3>Content Type Support:</h3>
 * <ul>
 * <li><strong>Text Formats:</strong> text/plain, text/html, text/markdown</li>
 * <li><strong>Structured Data:</strong> application/json, application/xml</li>
 * <li><strong>Documents:</strong> application/pdf, application/docx</li>
 * <li><strong>Media:</strong> image/png, image/jpeg, audio/*, video/*</li>
 * </ul>
 * 
 * <h3>A2A Protocol Integration:</h3>
 * <ul>
 * <li><strong>Standard Structure:</strong> Compliant with A2A artifact specifications</li>
 * <li><strong>Interoperability:</strong> Compatible with other agent systems</li>
 * <li><strong>Content Delivery:</strong> Structured format for artifact sharing</li>
 * <li><strong>Metadata Preservation:</strong> Complete artifact context and typing</li>
 * </ul>
 * 
 * <h3>Usage Patterns:</h3>
 * <ul>
 * <li>Task output collection and organization</li>
 * <li>Research deliverable management</li>
 * <li>Content generation and storage</li>
 * <li>Multi-agent artifact sharing</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see Task
 */
public class TaskArtifact {
    private String id;
    private String type;
    private String contentType;
    private String content;
    private Instant createdAt;    /**
     * Default constructor creating a new artifact with auto-generated ID and timestamp.
     * 
     * <p>Initializes a new task artifact with a randomly generated UUID identifier
     * and current creation timestamp. Prepares the artifact for type, content type,
     * and content assignment through setter methods or specialized constructors.</p>
     * 
     * <h3>Initial State:</h3>
     * <ul>
     * <li>Random UUID for unique identification</li>
     * <li>Current timestamp for creation tracking</li>
     * <li>Null values for type, content type, and content</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <p>Typically used by frameworks, serialization libraries, or when
     * artifact properties will be set incrementally during processing.</p>
     */
    public TaskArtifact() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }    /**
     * Constructor creating a new artifact with specified type, content type, and content.
     * 
     * <p>Initializes a complete task artifact with all essential properties set.
     * Automatically generates a unique identifier and creation timestamp while
     * accepting the artifact's semantic type, MIME content type, and actual content.</p>
     * 
     * <h3>Parameter Guidelines:</h3>
     * <ul>
     * <li><strong>type:</strong> Use semantic artifact types (literature-review, data-analysis, etc.)</li>
     * <li><strong>contentType:</strong> Specify proper MIME type (text/html, application/json, etc.)</li>
     * <li><strong>content:</strong> Provide the actual artifact content or data</li>
     * </ul>
     * 
     * <h3>Usage Examples:</h3>
     * <pre>
     * new TaskArtifact("research-summary", "text/markdown", "## Research Findings...")
     * new TaskArtifact("data-analysis", "application/json", "{\"results\": {...}}")
     * new TaskArtifact("visualization", "image/png", "base64EncodedImageData")
     * </pre>
     * 
     * @param type The semantic type of artifact (literature-review, data-analysis, etc.)
     * @param contentType The MIME type of the content (text/plain, application/json, etc.)
     * @param content The actual artifact content or data
     */
    public TaskArtifact(String type, String contentType, String content) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.contentType = contentType;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
