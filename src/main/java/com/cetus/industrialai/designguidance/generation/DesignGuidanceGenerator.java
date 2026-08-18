package com.cetus.industrialai.designguidance.generation;

import dev.langchain4j.service.SystemMessage;

import java.util.List;

/**
 * 工程设计指导模块的LLM接口。
 *
 * <p>只负责根据Java提供的检索证据生成结构化建议，
 * 不负责知识检索、引用真实性校验和无证据降级。</p>
 */
public interface DesignGuidanceGenerator {

    /**
     * 根据已经编号的知识片段生成工程建议草稿。
     *
     * @param userPrompt 用户提示词（问题+佐证）
     * @return 供Java校验和组装的结构化草稿
     */
    @SystemMessage(fromResource = "prompts/design-guidance-system-prompt.txt")
    GuidanceDraft generate(String userPrompt);

    /**
     * 大模型返回的结构化草稿，对应系统提示词中要求的 JSON 格式。
     *
     * @param status             指导状态（仅允许 GUIDANCE_PROVIDED 或 NEED_MORE_INFO）
     * @param summary            工程问题结论摘要
     * @param recommendations    模型生成的建议草稿列表
     * @param risks              潜在风险提示
     * @param missingInformation 模型认为缺失的补充信息
     */
    record GuidanceDraft(
            String status,
            String summary,
            List<RecommendationDraft> recommendations,
            List<String> risks,
            List<String> missingInformation
    ) {
    }

    /**
     * 模型生成的单条建议草稿，引用编号将在 Java 层再次校验。
     *
     * @param step           建议执行顺序（从 1 开始）
     * @param action         具体操作或设计方案
     * @param basisSourceIds 引用的知识片段编号列表（如 ["S1", "S3"]），需校验是否存在于本次召回的 sources 中
     */
    record RecommendationDraft(
            Integer step,
            String action,
            List<String> basisSourceIds
    ) {
    }
}
