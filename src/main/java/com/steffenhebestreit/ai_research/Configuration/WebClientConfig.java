package com.steffenhebestreit.ai_research.Configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for WebClient instances used throughout the application.
 * 
 * <p>This class provides a customized WebClient.Builder bean with appropriate timeout
 * settings to handle long-running operations and tool executions.</p>
 */
@Configuration
public class WebClientConfig {

    /**
     * Creates a customized WebClient.Builder with extended timeout settings.
     * 
     * <p>The WebClient is configured with:
     * <ul>
     *   <li>Connection timeout: 30 seconds</li>
     *   <li>Read timeout: 6 minutes (360 seconds)</li>
     *   <li>Write timeout: 6 minutes (360 seconds)</li>
     *   <li>Response timeout: 6 minutes (360 seconds)</li>
     * </ul>
     * 
     * <p>These extended timeouts ensure that long-running tool executions won't cause
     * premature connection closures.</p>
     * 
     * @return A WebClient.Builder with custom timeout settings
     */    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 30 seconds connection timeout
                .responseTimeout(Duration.ofSeconds(360)) // 6 minutes response timeout
                .keepAlive(true) // Enable keep-alive
                .option(ChannelOption.SO_KEEPALIVE, true) // Enable TCP keep-alive
                .option(ChannelOption.TCP_NODELAY, true) // Disable Nagle's algorithm for better latency
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(360, TimeUnit.SECONDS)) // 6 minutes read timeout
                        .addHandlerLast(new WriteTimeoutHandler(360, TimeUnit.SECONDS))) // 6 minutes write timeout
                .doOnDisconnected(conn -> {
                    // Log when connections are closed
                    System.out.println("Connection disconnected: " + conn);
                })
                .doOnError((request, throwable) -> {
                    // Log connection errors
                    System.err.println("Connection error for request: " + request + ", error: " + throwable.getMessage());
                }, (response, throwable) -> {
                    // Log response errors
                    System.err.println("Response error: " + response + ", error: " + throwable.getMessage());
                });
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
