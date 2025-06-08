package com.airesearch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAIService {
    
    @Value("${openai.api.baseurl}")
    private String baseUrl;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.model}")
    private String model;
    
    private final String systemRole;
    
    @Autowired
    public OpenAIService(String systemRole) {
        this.systemRole = systemRole;
    }
    
    // Rest of your service implementation
    // Use this.systemRole when creating chat completions
}
