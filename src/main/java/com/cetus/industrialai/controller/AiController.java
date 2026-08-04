package com.cetus.industrialai.controller;

import com.cetus.industrialai.ai.IndustrialAiAssistantService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * description: Flux流式生成 + SSE(ServerSentEvent)传输
 **/
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private IndustrialAiAssistantService industrialAiAssistantService;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message) {
        return industrialAiAssistantService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
}
