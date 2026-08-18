package com.cetus.industrialai.designguidance.model;

import java.util.List;

/**
 * 工程设计指导模块的最终响应。
 *
 * <p>响应同时返回工程建议、风险、缺失信息和知识库来源，
 * 便于前端展示RAG证据链，而不是只展示一段无法核验的模型回答。</p>
 *
 * @param status             本次指导状态
 * @param summary            工程问题结论摘要
 * @param recommendations    带知识来源的排查或设计建议
 * @param risks              执行建议前需要关注的风险
 * @param missingInformation 继续判断所需的缺失信息
 * @param sources            本次实际召回的知识片段
 * @param evidenceCount      有效知识片段数量
 */
public record DesignGuidanceResponse(
        Status status,
        String summary,
        List<Recommendation> recommendations,
        List<String> risks,
        List<String> missingInformation,
        List<Source> sources,
        int evidenceCount
) {

    /**
     * 响应状态枚举，表示本次指导的最终结论性质。
     */
    public enum Status {
        /**
         * 有足够证据且模型生成了可核验的建议
         */
        GUIDANCE_PROVIDED,
        /**
         * 证据不足以形成建议，需用户补充更多信息
         */
        NEED_MORE_INFO,
        /**
         * 检索未找到任何达到相似度阈值的知识片段
         */
        NO_RELEVANT_EVIDENCE,
        /**
         * 检索或模型服务异常，转为人工审核
         */
        MANUAL_REVIEW
    }

    /**
     * 单条工程建议及其知识库依据。
     *
     * @param step           建议执行顺序（从1开始递增）
     * @param action         具体的操作或设计建议
     * @param basisSourceIds 支撑本条建议的知识片段编号（如 "S1", "S3"），
     *                       已由服务层校验确保均在本次召回结果中
     */
    public record Recommendation(
            int step,
            String action,
            List<String> basisSourceIds
    ) {
    }

    /**
     * 返回给前端的知识来源摘要，用于展示证据链。
     *
     * @param sourceId 本次召回时赋予的编号（如 "S1"），与建议中的引用对应
     * @param title    来源文档名称（从元数据中读取的 file_name）
     * @param section  该片段所在的章节标题（从 Markdown 标题提取）
     * @param excerpt  经过压缩和格式化的片段预览（最多240字符）
     */
    public record Source(
            String sourceId,
            String title,
            String section,
            String excerpt
    ) {
    }
}
