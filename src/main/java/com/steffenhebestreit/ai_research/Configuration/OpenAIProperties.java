package com.steffenhebestreit.ai_research.Configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "openai.api")
@Validated
public class OpenAIProperties {

    @NotBlank
    private String baseurl;

    @NotBlank
    private String key;

    @NotBlank
    private String model;

    public String getBaseurl() {
        return baseurl;
    }

    public void setBaseurl(String baseurl) {
        this.baseurl = baseurl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
