package com.steffenhebestreit.ai_research.Configuration;

public class McpServerConfig {
    private String name;
    private String url;
    private AuthConfig auth;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }
}
