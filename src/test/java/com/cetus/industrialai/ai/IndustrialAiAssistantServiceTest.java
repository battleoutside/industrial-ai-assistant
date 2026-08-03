package com.cetus.industrialai.ai;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class IndustrialAiAssistantServiceTest {

    @Resource
    private IndustrialAiAssistantService industrialAiAssistantService;

    @Test
    void chat() {
        String result = industrialAiAssistantService.chat("你好，我是徐工");
        System.out.println(result);
    }

    @Test
    void chatWithMemory() {
        String result = industrialAiAssistantService.chat("你好，我是Cetus");
        System.out.println(result);
        result = industrialAiAssistantService.chat("你好，我是谁来着？");
        System.out.println(result);
    }

    @Test
    void chatForReport() {
        String userMessage = "你好，我是程序员Cetus，学编程两年半，请帮我制定学习报告";
        IndustrialAiAssistantService.Report report = industrialAiAssistantService.chatForReport(userMessage);
        System.out.println(report);
    }

    @Test
    void chatWithRag() {
        Result<String> result = industrialAiAssistantService.chatWithRag("怎么学习 Java？有哪些常见面试题？");
        String content = result.content();
        List<Content> sources = result.sources();
        System.out.println(content);
        System.out.println(sources);
    }

    @Test
    void chatWithTools() {
        String result = industrialAiAssistantService.chat("有哪些常见的计算机网络面试题？");
        System.out.println(result);
    }

    @Test
    void chatWithMcp() {
        String result = industrialAiAssistantService.chat("什么是博客园？");
        System.out.println(result);
    }
}
