package com.cetus.industrialai.industryintelligence.model;

import java.util.List;

/**
 * 行业信息咨询模块的最终响应。
 *
 * <p>响应将行业结论拆分为摘要、关键发现和公开来源，
 * 便于前端展示“结论 -> 来源”的可追溯关系，
 * 同时避免只返回一段无法核验的聊天文本。</p>
 *
 * @param status      本次行业咨询的最终状态
 * @param summary     对公开信息的整体归纳
 * @param findings    带来源编号的关键发现
 * @param sources     本次保留下来的有效公开来源
 * @param sourceCount 有效来源数量
 */
public record IndustryIntelligenceResponse(
        Status status,
        String summary,
        List<Finding> findings,
        List<Source> sources,
        int sourceCount
) {

    /**
     * 行业咨询状态枚举。
     */
    public enum Status {
        /**
         * 已获取公开信息并形成至少一条可追溯发现
         */
        SUCCESS,
        /**
         * 外部搜索完成，但没有形成可核验的相关发现
         */
        NO_RELEVANT_INFORMATION,
        /**
         * Agent、模型或MCP搜索链路异常，未返回自动分析结果
         */
        SEARCH_FAILED
    }

    /**
     * 单条行业发现及其公开来源。
     *
     * @param title     发现标题
     * @param content   基于公开来源归纳出的结论
     * @param sourceIds 支撑该发现的有效来源编号（如 S1、S2）
     */
    public record Finding(
            String title,
            String content,
            List<String> sourceIds
    ) {
    }

    /**
     * 返回给前端的公开来源卡片。
     *
     * @param sourceId   Java重新分配的稳定来源编号
     * @param title      来源标题
     * @param url        公开网页地址
     * @param publishedAt 页面明确提供的发布时间；无法确认时为空字符串
     */
    public record Source(
            String sourceId,
            String title,
            String url,
            String publishedAt
    ) {
    }
}
