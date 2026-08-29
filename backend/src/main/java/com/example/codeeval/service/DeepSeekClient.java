package com.example.codeeval.service;

import com.example.codeeval.config.LlmConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekClient {

    private final WebClient webClient;
    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(LlmConfig llmConfig, ObjectMapper objectMapper) {
        this.llmConfig = llmConfig;
        this.objectMapper = objectMapper;

        // Phase 3：为 WebClient 显式配置超时（此前 LlmConfig.timeout 配置项存在但从未生效，
        // 默认情况下 WebClient/Reactor Netty 的响应超时为无限等待，LLM 慢响应会拖死调用线程）
        HttpClient httpClient = HttpClient.create()
                // 响应超时：从连接建立后等待完整响应的最大时长，取自 llm.timeout 配置（120s）
                .responseTimeout(Duration.ofMillis(llmConfig.getTimeout()))
                // TCP 连接建立超时：DeepSeek API 地址不可达时快速失败（10s）
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

        this.webClient = WebClient.builder()
                .baseUrl(llmConfig.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + llmConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public String chat(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", llmConfig.getTemperature());
        requestBody.put("max_tokens", llmConfig.getMaxTokens());

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("DeepSeek调用失败: " + e.getMessage(), e);
        }
    }

    public String chat(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        );
        return chat(messages);
    }
}