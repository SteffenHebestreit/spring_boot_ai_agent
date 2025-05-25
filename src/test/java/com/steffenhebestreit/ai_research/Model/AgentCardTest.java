package com.steffenhebestreit.ai_research.Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentCard model class
 */
public class AgentCardTest {

    private AgentCard agentCard;
    private AgentProvider provider;
    private AgentCapabilities capabilities;
    private AgentSkill skill;

    @BeforeEach
    void setUp() {
        agentCard = new AgentCard();

        // Setup provider
        provider = new AgentProvider();
        provider.setName("AI Research Team");
        provider.setUrl("https://example.com");

        // Setup capabilities
        capabilities = new AgentCapabilities();
        capabilities.setMultiStep(true);
        capabilities.setMultiTurn(true);
        capabilities.setStreaming(true);
        
        // Setup skill
        skill = new AgentSkill();
        skill.setName("literature_review");
        skill.setDescription("Generate comprehensive literature reviews");
        skill.setInputFormat("text/plain");
        skill.setOutputFormat("text/markdown");
    }

    @Test
    void setName_ShouldUpdateName() {
        // Given
        String name = "AI Research Agent";
        
        // When
        agentCard.setName(name);
        
        // Then
        assertEquals(name, agentCard.getName());
    }

    @Test
    void setDescription_ShouldUpdateDescription() {
        // Given
        String description = "A comprehensive AI research assistant";
        
        // When
        agentCard.setDescription(description);
        
        // Then
        assertEquals(description, agentCard.getDescription());
    }

    @Test
    void setUrl_ShouldUpdateUrl() {
        // Given
        String url = "https://ai-research-agent.example.com";
        
        // When
        agentCard.setUrl(url);
        
        // Then
        assertEquals(url, agentCard.getUrl());
    }

    @Test
    void setProvider_ShouldUpdateProvider() {
        // When
        agentCard.setProvider(provider);
        
        // Then
        assertEquals(provider, agentCard.getProvider());
        assertEquals("AI Research Team", agentCard.getProvider().getName());
    }

    @Test
    void setVersion_ShouldUpdateVersion() {
        // Given
        String version = "1.0.0";
        
        // When
        agentCard.setVersion(version);
        
        // Then
        assertEquals(version, agentCard.getVersion());
    }

    @Test
    void setCapabilities_ShouldUpdateCapabilities() {
        // When
        agentCard.setCapabilities(capabilities);
        
        // Then
        assertEquals(capabilities, agentCard.getCapabilities());
        assertTrue(agentCard.getCapabilities().isMultiStep());
        assertTrue(agentCard.getCapabilities().isMultiTurn());
        assertTrue(agentCard.getCapabilities().isStreaming());
    }

    @Test
    void setDefaultInputModes_ShouldUpdateInputModes() {
        // Given
        List<String> inputModes = Arrays.asList("text/plain", "application/json");
        
        // When
        agentCard.setDefaultInputModes(inputModes);
        
        // Then
        assertEquals(inputModes, agentCard.getDefaultInputModes());
        assertEquals(2, agentCard.getDefaultInputModes().size());
    }

    @Test
    void setDefaultOutputModes_ShouldUpdateOutputModes() {
        // Given
        List<String> outputModes = Arrays.asList("text/markdown", "application/pdf");
        
        // When
        agentCard.setDefaultOutputModes(outputModes);
        
        // Then
        assertEquals(outputModes, agentCard.getDefaultOutputModes());
        assertEquals(2, agentCard.getDefaultOutputModes().size());
    }

    @Test
    void setSkills_ShouldUpdateSkills() {
        // Given
        List<AgentSkill> skills = Arrays.asList(skill);
        
        // When
        agentCard.setSkills(skills);
        
        // Then
        assertEquals(skills, agentCard.getSkills());
        assertEquals(1, agentCard.getSkills().size());
        assertEquals("literature_review", agentCard.getSkills().get(0).getName());
    }

    @Test
    void setSupportsAuthenticatedExtendedCard_ShouldUpdateFlag() {
        // Given
        boolean supportsAuth = true;
        
        // When
        agentCard.setSupportsAuthenticatedExtendedCard(supportsAuth);
        
        // Then
        assertEquals(supportsAuth, agentCard.isSupportsAuthenticatedExtendedCard());
    }
}
