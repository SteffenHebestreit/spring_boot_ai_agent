/**
 * Domain model classes for the AI Research application's core entities and data structures.
 * 
 * <p>This package contains the complete domain model for the AI Research Agent system, providing
 * comprehensive data structures for Agent-to-Agent (A2A) protocol compliance, multimodal content
 * management, conversation persistence, and task lifecycle management. The models support both
 * JPA persistence and JSON serialization for API interactions.</p>
 * 
 * <h3>Core Domain Categories:</h3>
 * <ul>
 * <li><strong>Agent Protocol Models:</strong> A2A protocol compliance with agent cards, capabilities, and skills</li>
 * <li><strong>Communication Models:</strong> Message structures supporting multimodal content and conversation management</li>
 * <li><strong>Persistence Models:</strong> JPA entities for data persistence with optimized relationships</li>
 * <li><strong>Task Management:</strong> Comprehensive task lifecycle models with artifact tracking</li>
 * </ul>
 * 
 * <h3>Key Model Classes:</h3>
 * <ul>
 * <li>{@link com.steffenhebestreit.ai_research.Model.AgentCard} - Complete A2A protocol agent metadata and discovery information</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.AgentCapabilities} - Agent capability declaration and feature flags</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.AgentSkill} - Detailed skill specifications for agent functionality</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.AgentProvider} - Provider attribution and organizational information</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.Message} - Multimodal message structure with polymorphic content support</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.Chat} - Persistent conversation management with JPA relationships</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.ChatMessage} - Individual messages within conversation contexts</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.Task} - Research task lifecycle management and state tracking</li>
 * <li>{@link com.steffenhebestreit.ai_research.Model.TaskArtifact} - AI-generated deliverables and content management</li>
 * </ul>
 * 
 * <h3>A2A Protocol Compliance:</h3>
 * <ul>
 * <li><strong>Agent Discovery:</strong> Standard agent card structure for inter-agent discovery</li>
 * <li><strong>Capability Exchange:</strong> Structured capability and skill declaration</li>
 * <li><strong>Communication Standards:</strong> Standardized message formats for agent interaction</li>
 * <li><strong>Provider Attribution:</strong> Clear identification of agent creators and maintainers</li>
 * </ul>
 * 
 * <h3>Multimodal Content Support:</h3>
 * <ul>
 * <li><strong>Content Flexibility:</strong> Support for text, images, documents, and structured data</li>
 * <li><strong>Polymorphic Messaging:</strong> Unified message structure for diverse content types</li>
 * <li><strong>Metadata Management:</strong> Comprehensive content metadata and type information</li>
 * <li><strong>Serialization Support:</strong> Optimized JSON serialization for API interactions</li>
 * </ul>
 * 
 * <h3>Persistence Architecture:</h3>
 * <ul>
 * <li><strong>JPA Integration:</strong> Full JPA support with optimized entity relationships</li>
 * <li><strong>Bidirectional Mapping:</strong> Proper relationship management between entities</li>
 * <li><strong>Lifecycle Management:</strong> Automatic timestamp tracking and state management</li>
 * <li><strong>Performance Optimization:</strong> Lazy loading and fetch strategies for efficiency</li>
 * </ul>
 * 
 * <h3>Task Management Models:</h3>
 * <ul>
 * <li><strong>Task Lifecycle:</strong> Complete task state management from creation to completion</li>
 * <li><strong>Artifact Tracking:</strong> Management of AI-generated deliverables and outputs</li>
 * <li><strong>Progress Monitoring:</strong> Real-time task progress and status tracking</li>
 * <li><strong>Result Management:</strong> Structured storage and retrieval of task results</li>
 * </ul>
 * 
 * <h3>JSON Serialization Features:</h3>
 * <ul>
 * <li><strong>Clean Serialization:</strong> Optimized JSON output with null value exclusion</li>
 * <li><strong>Circular Reference Handling:</strong> Proper management of bidirectional relationships</li>
 * <li><strong>Content Type Support:</strong> MIME type-aware content serialization</li>
 * <li><strong>API Compatibility:</strong> JSON structures compatible with frontend and external systems</li>
 * </ul>
 * 
 * <h3>Data Validation and Integrity:</h3>
 * <ul>
 * <li><strong>Type Safety:</strong> Strong typing for all model properties</li>
 * <li><strong>Constraint Validation:</strong> JPA and Bean Validation constraints</li>
 * <li><strong>Relationship Integrity:</strong> Proper cascade operations and referential integrity</li>
 * <li><strong>Content Validation:</strong> Validation for multimodal content structures</li>
 * </ul>
 * 
 * <h3>Thread Safety and Concurrency:</h3>
 * <ul>
 * <li><strong>Immutable Design:</strong> Immutable structures where appropriate</li>
 * <li><strong>Safe Updates:</strong> Thread-safe update patterns for mutable entities</li>
 * <li><strong>Concurrent Access:</strong> Proper handling of concurrent data access</li>
 * <li><strong>State Consistency:</strong> Consistent state management across concurrent operations</li>
 * </ul>
 * 
 * @author Steffen Hebestreit
 * @version 1.0
 * @since 1.0
 * @see jakarta.persistence.Entity
 * @see com.fasterxml.jackson.annotation.JsonInclude
 * @see com.fasterxml.jackson.annotation.JsonManagedReference
 */
package com.steffenhebestreit.ai_research.Model;
