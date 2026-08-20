package com.cetus.industrialai.industryintelligence.controller;

import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceRequest;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceResponse;
import com.cetus.industrialai.industryintelligence.service.IndustryIntelligenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行业信息咨询模块的 HTTP 接口。
 *
 * <p>负责接收开放式行业问题并执行参数校验，
 * 具体的 Agent 推理、MCP Tool Calling、来源校验和异常降级
 * 由 IndustryIntelligenceService 与 IndustryIntelligenceAgent 完成。</p>
 */
@RestController
@RequestMapping("/industry-intelligence")
public class IndustryIntelligenceController {

    private final IndustryIntelligenceService industryIntelligenceService;

    public IndustryIntelligenceController(
            IndustryIntelligenceService industryIntelligenceService
    ) {
        this.industryIntelligenceService = industryIntelligenceService;
    }

    /**
     * 根据实时公开信息生成带来源的行业咨询结果。
     */
    @PostMapping("/analyze")
    public IndustryIntelligenceResponse analyze(
            @Valid @RequestBody IndustryIntelligenceRequest request
    ) {
        return industryIntelligenceService.analyze(request);
    }
}
