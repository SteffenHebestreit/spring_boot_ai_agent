package com.steffenhebestreit.ai_research.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.ArrayList;

@Configuration
@ConfigurationProperties(prefix = "agent.integrations")
public class IntegrationProperties {

    private List<McpServerConfig> mcpServers = new ArrayList<>();
    private List<A2aPeerConfig> a2aPeers = new ArrayList<>();

    public List<McpServerConfig> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(List<McpServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }

    public List<A2aPeerConfig> getA2aPeers() {
        return a2aPeers;
    }

    public void setA2aPeers(List<A2aPeerConfig> a2aPeers) {
        this.a2aPeers = a2aPeers;
    }
}
