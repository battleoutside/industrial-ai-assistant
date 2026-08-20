package com.cetus.industrialai.industryintelligence.agent;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 行业信息咨询 Agent 的装配配置。
 *
 * <p>复用项目现有的千问 ChatModel 与全局 McpToolProvider，
 * 不重新创建模型客户端或 MCP 连接，保持第三模块只负责业务能力装配。</p>
 */
@Configuration
public class IndustryIntelligenceAgentConfig {

    /**
     * 创建具备 MCP Tool Calling 能力的行业信息咨询 Agent。
     *
     * <p>LangChain4j AI Services 会负责基础 Agent Loop：
     * 模型决定调用工具 -> 执行 MCP Tool -> 将结果回填模型 -> 继续生成最终结构化结果。</p>
     *
     * @param qwenChatModel   项目已有的千问模型 Bean
     * @param mcpToolProvider 项目已有的 MCP Web Search Tool Provider
     * @return 可调用 MCP 工具的行业信息咨询 Agent
     */
    @Bean
    public IndustryIntelligenceAgent industryIntelligenceAgent(
            @Qualifier("myQwenChatModel") ChatModel qwenChatModel,
            McpToolProvider mcpToolProvider
    ) {
        return AiServices.builder(IndustryIntelligenceAgent.class)
                .chatModel(qwenChatModel)
                .toolProvider(mcpToolProvider)
                .build();
    }
}
