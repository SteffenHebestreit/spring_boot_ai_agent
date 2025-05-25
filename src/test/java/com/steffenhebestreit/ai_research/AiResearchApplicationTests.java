package com.steffenhebestreit.ai_research;

import com.steffenhebestreit.ai_research.Configuration.AgentCardProperties;
import com.steffenhebestreit.ai_research.Configuration.OpenAIProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Main application test class for the AI Research project.
 * 
 * This class tests that the Spring application context loads correctly,
 * which verifies that all components, beans, and configurations are properly set up.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({AgentCardProperties.class, OpenAIProperties.class}) // Ensure these are loaded
class AiResearchApplicationTests {

	@Test
	void contextLoads() {
		// This test will fail if the application context cannot be loaded
	}

}
