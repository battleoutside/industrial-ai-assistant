package com.cetus.industrialai.designguidance.generation;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工程设计指导模块的 AI Service 配置。
 *
 * <p>该服务只装配千问ChatModel，
 * 保持工程指导RAG链路独立且易于测试。</p>
 */
@Configuration
public class DesignGuidanceGeneratorConfig {

    @Bean
    public DesignGuidanceGenerator designGuidanceGenerator(
            @Qualifier("myQwenChatModel") ChatModel qwenChatModel
    ) {
        return AiServices.builder(DesignGuidanceGenerator.class)
                .chatModel(qwenChatModel)
                .build();
    }
}
