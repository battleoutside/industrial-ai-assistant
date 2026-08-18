package com.cetus.industrialai.designguidance.controller;

import com.cetus.industrialai.designguidance.model.DesignGuidanceRequest;
import com.cetus.industrialai.designguidance.model.DesignGuidanceResponse;
import com.cetus.industrialai.designguidance.service.DesignGuidanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工程设计指导模块的 HTTP 接口。
 *
 * <p>负责接收前端工程问题并执行参数校验，
 * 具体的知识检索、千问生成和引用校验由DesignGuidanceService完成。</p>
 */
@RestController
@RequestMapping("/design-guidance")
public class DesignGuidanceController {

    private final DesignGuidanceService designGuidanceService;

    public DesignGuidanceController(DesignGuidanceService designGuidanceService) {
        this.designGuidanceService = designGuidanceService;
    }

    /**
     * 根据企业知识库生成带来源的工程指导建议。
     */
    @PostMapping("/analyze")
    public DesignGuidanceResponse analyze(
            @Valid @RequestBody DesignGuidanceRequest request
    ) {
        return designGuidanceService.analyze(request);
    }
}
