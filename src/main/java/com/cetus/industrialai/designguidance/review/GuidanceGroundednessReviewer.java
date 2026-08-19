package com.cetus.industrialai.designguidance.review;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 工程设计指导模块的 Groundedness 审核器。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>接收一条候选工程建议及其对应的知识库证据；</li>
 *     <li>判断该建议是否能够被证据明确支持；</li>
 *     <li>只返回结构化审核结果，不负责修改原建议。</li>
 * </ul>
 *
 * <p>MVP阶段将整条 recommendation.action 作为一个待审核 Claim。
 * 后续如需更细粒度，可继续扩展为真正的 Claim 拆分与逐条审核。</p>
 */
public interface GuidanceGroundednessReviewer {

    /**
     * 审核候选工程建议是否被给定证据支持。
     *
     * @param reviewInput 已组装的“候选建议 + 对应证据”文本
     * @return 结构化审核结果
     */
    @SystemMessage(fromResource = "prompts/design-guidance-groundedness-review-prompt.txt")
    ReviewResult review(@UserMessage String reviewInput);

    /**
     * Groundedness 审核结果。
     *
     * @param supported true表示证据足以支持该建议；false表示存在无依据扩展
     * @param reason    简要说明通过或拒绝原因，便于测试与排查
     */
    record ReviewResult(
            boolean supported,
            String reason
    ) {
    }
}
