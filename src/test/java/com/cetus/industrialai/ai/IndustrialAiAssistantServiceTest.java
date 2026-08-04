package com.cetus.industrialai.ai;

import dev.langchain4j.model.chat.ChatModel;
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
        String userMessage = "你好，我是线束产品工程师Cetus，请帮我制定学习报告（研发、测试、打样）";
        IndustrialAiAssistantService.Report report = industrialAiAssistantService.chatForReport(userMessage);
        System.out.println(report);
    }

    @Test
    void chatWithRag() {
        Result<String> result =
                industrialAiAssistantService.chatWithRag(
                        "Java 的推荐学习路线是什么？" //高频测试中，插损、回损不达标该如何排查？
                );

        System.out.println("AI回答：" + result.content());
        System.out.println("召回片段数量：" + result.sources().size());
    }

    //Jsoup 爬虫工具
    @Test
    void chatWithTools() {
        String result = industrialAiAssistantService.chat("线束有哪些种类？有哪些常见高速线束结构？");
        System.out.println(result);
    }

    //Mcp WebSearch
    @Test
    void chatWithMcp() {
        String result = industrialAiAssistantService.chat("怎么学习PCB知识？有哪些常见板材结构？");
        System.out.println(result);
    }

    @Test
    void chatWithGuardrail() {
//        String result = industrialAiAssistantService.chat("kill the game"); // 拦截带敏感词汇的对话
        String result = industrialAiAssistantService.chat("play the game");
        System.out.println(result);
    }

    @Resource(name = "qwenChatModel")
    private ChatModel qwenChatModel;

    @Test
    void testListenerDirectly() {

        System.out.println(qwenChatModel.getClass());

        String result = qwenChatModel.chat("你好");

        System.out.println(result);
    }


}
