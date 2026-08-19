package com.cetus.industrialai.designguidance.review;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工程设计指导 Groundedness Reviewer 配置。
 *
 * <p>使用现有 ChatModel 创建第二个受控 AI Service，
 * 专门负责审核候选工程建议与知识库证据之间的语义一致性。</p>
 */
@Configuration
public class GuidanceGroundednessReviewerConfig {

    /**
     * 创建 Groundedness Reviewer。
     *
     * <p>Reviewer 与主生成器共用项目现有 ChatModel，
     * 但使用独立 Prompt 和独立职责，避免生成与审核逻辑混在一起。</p>
     */
    @Bean
    public GuidanceGroundednessReviewer guidanceGroundednessReviewer(
            @Qualifier("myQwenChatModel") ChatModel chatModel) {
        return AiServices.builder(GuidanceGroundednessReviewer.class)
                .chatModel(chatModel)
                .build();
    }
}
