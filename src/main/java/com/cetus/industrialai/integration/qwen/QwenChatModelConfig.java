package com.cetus.industrialai.integration.qwen;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import dev.langchain4j.model.openai.OpenAiChatRequestParameters;

import java.util.Map;

/**
 * description: 手动构造自定义的 QwenChatModel
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.chat-model")
public class QwenChatModelConfig {

    private String apiKey;

    private String modelName;

    @Resource
    private ChatModelListener chatModelListener;

    @Bean("myQwenChatModel")
    public ChatModel myQwenChatModel(
            @Value("${app.ai.qwen.api-key}") String apiKey,
            @Value("${app.ai.qwen.base-url}") String baseUrl,
            @Value("${app.ai.qwen.model-name}") String modelName
    ) {

        OpenAiChatRequestParameters requestParameters =
                OpenAiChatRequestParameters.builder()
                        .customParameters(Map.of(
                                "enable_thinking", false
                        ))
                        .build();

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .defaultRequestParameters(requestParameters)
                .timeout(Duration.ofSeconds(180))
                .maxRetries(1)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
