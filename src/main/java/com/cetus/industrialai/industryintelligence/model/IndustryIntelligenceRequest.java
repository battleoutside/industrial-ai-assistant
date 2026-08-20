package com.cetus.industrialai.industryintelligence.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 行业信息咨询模块的请求。
 *
 * <p>MVP阶段仅保留一个开放式 question 字段，
 * 让 Agent 自主理解用户意图并决定如何使用外部信息工具，
 * 避免像工程设计指导模块一样提前固定过多业务参数。</p>
 *
 * @param question 用户需要调研、对比或归纳的行业信息问题
 */
public record IndustryIntelligenceRequest(
        @NotBlank(message = "行业咨询问题不能为空")
        @Size(max = 1500, message = "行业咨询问题不能超过1500个字符")
        String question
) {
}
