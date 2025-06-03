package com.steffenhebestreit.ai_research.Controller;

import com.steffenhebestreit.ai_research.Configuration.AgentCardProperties;
import com.steffenhebestreit.ai_research.Service.DynamicIntegrationService;
import com.steffenhebestreit.ai_research.Configuration.IntegrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import com.steffenhebestreit.ai_research.Configuration.SecurityConfig;
import com.steffenhebestreit.ai_research.Configuration.TestSecurityConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import java.util.function.Supplier;

import java.nio.charset.StandardCharsets;
import org.springframework.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

/**
 * Tests for the AgentCardController to ensure it serves the agent card correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestSecurityConfig.class, AgentCardControllerTest.AgentCardControllerTestConfiguration.class})
class AgentCardControllerTest {

    @TestConfiguration
    static class AgentCardControllerTestConfiguration {
        @Bean
        @Primary
        public AgentCardProperties agentCardProperties() {
            AgentCardProperties props = new AgentCardProperties();
            // Set default test values for AgentCardProperties that AgentCardController uses for overriding
            props.setId("test-props-id");
            props.setName("Test Props Name");
            props.setDescription("Test Props Description");
            props.setUrl("http://test-props-url.com");
            props.setContactEmail("test-props-contact@example.com");

            AgentCardProperties.Provider provider = new AgentCardProperties.Provider();
            provider.setOrganization("Test Props Org");
            provider.setUrl("http://test-props-provider-url.com");
            props.setProvider(provider);
            
            return props;
        }

        @Bean
        @Primary
        public DynamicIntegrationService dynamicIntegrationService() {
            IntegrationProperties mockIntegrationProps = Mockito.mock(IntegrationProperties.class);
            RestTemplateBuilder mockRestTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
            RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);

            // Stubbing for RestTemplateBuilder
            when(mockRestTemplateBuilder.requestFactory(Mockito.<Supplier<ClientHttpRequestFactory>>any())).thenReturn(mockRestTemplateBuilder);
            when(mockRestTemplateBuilder.build()).thenReturn(mockRestTemplate);

            DynamicIntegrationService serviceInstance = new DynamicIntegrationService(mockIntegrationProps, mockRestTemplateBuilder);
            return Mockito.spy(serviceInstance);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that the controller returns the agent card JSON from the correct endpoint
     */    
    @Test
    @WithMockUser(username = "user")
    void getAgentCard_ShouldReturnAgentCardJson() throws Exception {
        // When - using @WithMockUser annotation for authentication
        MvcResult result = mockMvc.perform(get("/.well-known/agent.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                // Verify that values from AgentCardProperties (set in TestConfiguration) are used for overriding
                .andExpect(jsonPath("$.id").value("test-props-id"))
                .andExpect(jsonPath("$.name").value("Test Props Name"))
                .andExpect(jsonPath("$.description").value("Test Props Description"))
                .andExpect(jsonPath("$.url").value("http://test-props-url.com"))
                .andExpect(jsonPath("$.provider.organization").value("Test Props Org"))
                .andExpect(jsonPath("$.provider.url").value("http://test-props-provider-url.com"))
                .andExpect(jsonPath("$.contact_email").value("test-props-contact@example.com"))
                .andReturn();

        // Then
        String responseContent = result.getResponse().getContentAsString();
        // Basic check that the overridden description is present
        assertTrue(responseContent.contains("\"description\":\"Test Props Description\""), "Response should contain description from props");
    }

    /**
     * Test that the returned agent card contains all required fields
     */
    @Test
    void getAgentCard_ShouldContainRequiredFields() throws Exception {
        // Get the actual agent card content from the classpath resource
        Resource actualAgentCardResource = new ClassPathResource("agentCard.json");
        String agentCardContent = StreamUtils.copyToString(
                actualAgentCardResource.getInputStream(), StandardCharsets.UTF_8);

        // The agent card should have required A2A fields
        assertTrue(agentCardContent.contains("\"id\":"), "Agent card should contain an id field");
        assertTrue(agentCardContent.contains("\"name\":"), "Agent card should contain a name field");
        assertTrue(agentCardContent.contains("\"description\":"), "Agent card should contain a description field");
        assertTrue(agentCardContent.contains("\"version\":"), "Agent card should contain a version field");
        assertTrue(agentCardContent.contains("\"contact_email\":"), "Agent card should contain a contact_email field");
        assertTrue(agentCardContent.contains("\"authentication\":"), "Agent card should contain an authentication field");
        assertTrue(agentCardContent.contains("\"skills\":"), "Agent card should contain skills field");
    }
}
