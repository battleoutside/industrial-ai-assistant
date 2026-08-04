package com.cetus.industrialai.ai;

import com.cetus.industrialai.ai.tools.InterviewQuestionTool;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndustrialAiAssistantServiceFactory {

    @Resource(name = "myQwenChatModel")
    private ChatModel qwenChatModel;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    // 构建AI Service
    @Bean   //此处可改为IndustrialAiAssistantService使用@AiService自动创建实例
    public IndustrialAiAssistantService industrialAiAssistantService() {
        // 会话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        // 构造 Service
        return AiServices.builder(IndustrialAiAssistantService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))//每个会话独立存储
                .contentRetriever(contentRetriever) // RAG 检索增强生成 Embedding
                .tools(new InterviewQuestionTool()) // 工具调用 Jsoup
                .toolProvider(mcpToolProvider) // MCP 工具调用 WebSearch
                .build();
    }

}
