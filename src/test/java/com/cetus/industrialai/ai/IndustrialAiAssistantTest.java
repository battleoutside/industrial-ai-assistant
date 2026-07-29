package com.cetus.industrialai.ai;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IndustrialAiAssistantTest {

    @Resource
    private IndustrialAiAssistant industrialAiAssistant;

    @Test
    void chat() {
        industrialAiAssistant.chat("你好，我是格雷福斯");
    }

    @Test
    void chatWithMessage() {
        UserMessage userMessage = UserMessage.from(
                TextContent.from("描述图片"),
                ImageContent.from("https://www.codefather.cn/logo.png")
        );
        industrialAiAssistant.chatWithMessage(userMessage);
    }

}
