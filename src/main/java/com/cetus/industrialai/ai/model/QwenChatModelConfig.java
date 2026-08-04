package com.cetus.industrialai.ai.model;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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

    @Bean
    public ChatModel myQwenChatModel() {
        //        System.out.println("我的模型:" + model);
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .listeners(List.of(chatModelListener))
                .build();
    }
}
