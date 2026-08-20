package com.cetus.industrialai.industryintelligence.agent;

import dev.langchain4j.service.SystemMessage;

import java.util.List;

/**
 * 行业信息咨询模块的 Agent 接口。
 *
 * <p>该接口由 LangChain4j AI Services 生成实现。
 * Agent 可以通过 MCP Tool Provider 自主调用 Web Search 工具，
 * 并将搜索结果整理为结构化草稿。</p>
 */
public interface IndustryIntelligenceAgent {

    /**
     * 对开放式行业问题执行一次受限的外部信息调研。
     *
     * @param question 用户原始行业咨询问题
     * @return Agent 基于 MCP 搜索结果生成的结构化草稿
     */
    @SystemMessage(fromResource = "prompts/industry-intelligence-system-prompt.txt")
    ResearchDraft research(String question);

    /**
     * Agent 返回的结构化草稿。
     *
     * @param status   仅允许 SUCCESS 或 NO_RELEVANT_INFORMATION
     * @param summary  行业信息整体摘要
     * @param findings 关键发现列表
     * @param sources  Agent 从工具结果中整理出的公开来源
     */
    record ResearchDraft(
            String status,
            String summary,
            List<FindingDraft> findings,
            List<SourceDraft> sources
    ) {
    }

    /**
     * Agent 生成的单条行业发现草稿。
     *
     * @param title     发现标题
     * @param content   对公开信息的归纳内容
     * @param sourceIds Agent 引用的来源编号，Java层会再次校验
     */
    record FindingDraft(
            String title,
            String content,
            List<String> sourceIds
    ) {
    }

    /**
     * Agent 从 MCP Web Search 结果中整理出的来源草稿。
     *
     * @param sourceId    Agent 临时来源编号（如 S1、S2）
     * @param title       搜索结果标题
     * @param url         搜索结果返回的公开URL
     * @param publishedAt 搜索结果明确提供的发布时间；无法确认时返回空字符串
     */
    record SourceDraft(
            String sourceId,
            String title,
            String url,
            String publishedAt
    ) {
    }
}
