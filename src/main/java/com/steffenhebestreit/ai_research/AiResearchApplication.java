package com.steffenhebestreit.ai_research;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the AI Research project.
 * 
 * This Spring Boot application provides an A2A-compatible AI research agent
 * with capabilities for information retrieval, content summarization, and
 * integration with other AI agents and MCP servers.
 */
@SpringBootApplication
public class AiResearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiResearchApplication.class, args);
	}

}
