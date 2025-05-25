package com.steffenhebestreit.ai_research;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Main application test class for the AI Research project.
 * 
 * This class tests that the Spring application context loads correctly,
 * which verifies that all components, beans, and configurations are properly set up.
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class AiResearchApplicationTests {

	@Test
	void contextLoads() {
		// This test will fail if the application context cannot be loaded
	}

}
