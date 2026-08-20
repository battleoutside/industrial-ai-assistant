package com.cetus.industrialai.industryintelligence.service;

import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent;
import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent.FindingDraft;
import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent.ResearchDraft;
import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent.SourceDraft;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceRequest;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceResponse;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceResponse.Finding;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceResponse.Source;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceResponse.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 行业信息咨询模块的业务编排服务。
 *
 * <p>负责调用具备 MCP Tool Calling 能力的 Agent，
 * 并在 Agent 返回后执行确定性的 Java 来源校验、引用校验、摘要一致性处理和异常降级。
 * MVP阶段不增加数据库、Memory、多Agent或二次Reviewer。</p>
 */
@Service
@Slf4j
public class IndustryIntelligenceService {

    private final IndustryIntelligenceAgent industryIntelligenceAgent;

    public IndustryIntelligenceService(
            IndustryIntelligenceAgent industryIntelligenceAgent
    ) {
        this.industryIntelligenceAgent = industryIntelligenceAgent;
    }

    /**
     * 执行一次完整的行业信息咨询链路。
     *
     * <p>主链路保持为“开放问题 -> Agent -> MCP Web Search -> 结构化草稿 -> Java校验”。
     * Java不替模型决定搜索关键词；MVP阶段负责校验URL格式、Finding引用编号，
     * 并在后处理改变有效事实集合时重建安全摘要，避免摘要继续引用已被过滤的信息。</p>
     *
     * @param request 用户行业咨询请求
     * @return 经来源与引用校验后的行业信息咨询响应
     */
    public IndustryIntelligenceResponse analyze(
            IndustryIntelligenceRequest request
    ) {
        // -------- 【一】整理开放式行业问题 --------
        // 保留自然语言表达，只进行空白清理，不提前做固定分类或拆参数，
        // 让 Agent 根据问题本身决定搜索方向和工具调用方式。
        String question = request.question().trim();
        ResearchDraft draft;

        // -------- 【二】执行 Agent + MCP Tool Calling --------
        // IndustryIntelligenceAgent 已绑定 McpToolProvider。
        // LangChain4j 负责模型工具调用意图、MCP执行和结果回填的基础循环。
        try {
            draft = industryIntelligenceAgent.research(question);
        } catch (RuntimeException exception) {
            log.warn("行业信息咨询 Agent 或 MCP 搜索链路执行失败", exception);
            return searchFailed();
        }

        // -------- 【三】检查 Agent 是否返回有效草稿 --------
        // null 不视为“没有行业信息”，而视为本次自动研究没有形成有效结果。
        if (draft == null) {
            log.warn("行业信息咨询 Agent 未返回有效结构化结果");
            return searchFailed();
        }

        // -------- 【四】Java 校验并重建来源 --------
        // 只保留 http/https 公开URL，按URL去重，并由Java重新编号为 S1、S2...。
        // 同时记录是否发生来源过滤，供最终摘要一致性判断使用。
        ValidatedSources validatedSources = validateSources(draft.sources());

        if (validatedSources.sources().isEmpty()) {
            return noRelevantInformation(
                    "未获得可核验的公开来源，系统未返回行业结论。"
            );
        }

        // -------- 【五】校验 Finding -> Source 引用关系 --------
        // Finding 中的模型来源编号必须能映射到第四阶段保留下来的真实URL。
        // 无效引用会被剔除；若整条 Finding 已无有效引用，则整条 Finding 被过滤。
        // 同样记录是否发生调整，避免最终摘要继续描述已被删除的发现或引用。
        ValidatedFindings validatedFindings = validateFindings(
                draft.findings(),
                validatedSources.sourceIdMapping()
        );
        List<Finding> findings = validatedFindings.findings();

        // -------- 【六】组装最终响应或执行安全降级 --------
        // 即使模型声称 SUCCESS，只要没有一条带有效来源的 Finding，
        // Java仍会降级为 NO_RELEVANT_INFORMATION。
        Status draftStatus = parseStatus(draft.status());
        if (draftStatus == Status.NO_RELEVANT_INFORMATION || findings.isEmpty()) {
            return noRelevantInformation(
                    hasText(draft.summary())
                            ? draft.summary().trim()
                            : "未找到足以形成可核验结论的相关公开信息。"
            );
        }

        // -------- 【七】确保 Summary 与最终事实集合保持一致 --------
        // 正常情况下保留 LLM 的归纳能力；但如果 Java 后处理删除/合并了来源，
        // 或过滤了 Finding/引用，则不再直接沿用 Draft.summary。
        // 此时仅根据最终保留下来的 Finding 标题生成安全摘要，避免出现
        // “摘要提到了某事实，但最终 findings/sources 已没有对应证据”的不一致。
        boolean resultAdjusted = validatedSources.adjusted()
                || validatedFindings.adjusted();

        String summary = resultAdjusted
                ? buildSafeSummary(findings)
                : hasText(draft.summary())
                ? draft.summary().trim()
                : "已根据公开信息整理出可追溯的行业发现。";

        return new IndustryIntelligenceResponse(
                Status.SUCCESS,
                summary,
                findings,
                validatedSources.sources(),
                validatedSources.sources().size()
        );
    }

    /**
     * 校验 Agent 返回的公开来源，并由 Java 重新生成稳定编号。
     *
     * <p>校验规则：</p>
     * <p>1. title、url 必须存在；</p>
     * <p>2. URL 仅允许 http/https；</p>
     * <p>3. 按 URL 去重；</p>
     * <p>4. 保留“模型临时sourceId -> Java最终sourceId”映射，供 Finding 校验；</p>
     * <p>5. 记录是否发生过滤或去重，供最终摘要一致性判断。</p>
     */
    private ValidatedSources validateSources(List<SourceDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return new ValidatedSources(List.of(), Map.of(), false);
        }

        List<Source> sources = new ArrayList<>();
        Map<String, String> sourceIdMapping = new LinkedHashMap<>();
        boolean adjusted = false;

        for (SourceDraft draft : drafts) {
            if (draft == null
                    || !hasText(draft.title())
                    || !hasText(draft.url())
                    || !isValidPublicUrl(draft.url())) {
                adjusted = true;
                continue;
            }

            String normalizedUrl = draft.url().trim();

            // 同一 URL 如果被模型重复列出，不重复生成 Source，
            // 但仍将新的临时 sourceId 映射到已经存在的最终编号。
            String existingSourceId = findSourceIdByUrl(sources, normalizedUrl);
            if (existingSourceId != null) {
                adjusted = true;
                if (hasText(draft.sourceId())) {
                    sourceIdMapping.putIfAbsent(
                            draft.sourceId().trim(),
                            existingSourceId
                    );
                }
                continue;
            }

            String sourceId = "S" + (sources.size() + 1);
            sources.add(new Source(
                    sourceId,
                    draft.title().trim(),
                    normalizedUrl,
                    hasText(draft.publishedAt())
                            ? draft.publishedAt().trim()
                            : ""
            ));

            if (hasText(draft.sourceId())) {
                sourceIdMapping.putIfAbsent(draft.sourceId().trim(), sourceId);
            }
        }

        return new ValidatedSources(
                List.copyOf(sources),
                Map.copyOf(sourceIdMapping),
                adjusted
        );
    }

    /**
     * 根据URL查找已经生成的最终来源编号。
     *
     * <p>MVP来源数量通常很小，直接线性扫描即可，
     * 不额外维护第二套URL索引结构，保持实现简单。</p>
     */
    private String findSourceIdByUrl(List<Source> sources, String url) {
        for (Source source : sources) {
            if (source.url().equals(url)) {
                return source.sourceId();
            }
        }
        return null;
    }

    /**
     * 过滤没有有效公开来源支撑的行业发现。
     *
     * <p>一条 Finding 可以引用多个来源；无效编号会单独剔除。
     * 若剔除后不再剩余任何有效来源，则整条 Finding 被丢弃。</p>
     *
     * <p>同时返回 adjusted 标记，用于告知最终组装阶段：
     * 当前 Finding 集合是否已经与模型原始 Draft 不同。</p>
     */
    private ValidatedFindings validateFindings(
            List<FindingDraft> drafts,
            Map<String, String> sourceIdMapping
    ) {
        if (drafts == null || drafts.isEmpty() || sourceIdMapping.isEmpty()) {
            return new ValidatedFindings(List.of(), false);
        }

        List<Finding> findings = new ArrayList<>();
        boolean adjusted = false;

        for (FindingDraft draft : drafts) {
            if (draft == null
                    || !hasText(draft.title())
                    || !hasText(draft.content())) {
                adjusted = true;
                continue;
            }

            List<String> originalSourceIds = draft.sourceIds() == null
                    ? List.of()
                    : draft.sourceIds().stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();

            List<String> validSourceIds = originalSourceIds.stream()
                    .map(sourceIdMapping::get)
                    .filter(mappedId -> mappedId != null)
                    .distinct()
                    .toList();

            // 只要某个引用无法映射，或多个临时来源被合并到同一最终来源，
            // 都说明最终引用集合已经与模型草稿发生变化。
            if (validSourceIds.size() != originalSourceIds.size()) {
                adjusted = true;
            }

            if (validSourceIds.isEmpty()) {
                adjusted = true;
                continue;
            }

            findings.add(new Finding(
                    draft.title().trim(),
                    draft.content().trim(),
                    validSourceIds
            ));
        }

        return new ValidatedFindings(
                List.copyOf(findings),
                adjusted
        );
    }

    /**
     * 当 Java 后处理改变了有效事实集合时，基于最终 Finding 生成安全摘要。
     *
     * <p>这里只复用最终保留下来的 Finding 标题，不再让模型重新生成摘要，
     * 避免额外 LLM 调用、延迟和 Token 成本，符合 MVP 原则。</p>
     */
    private String buildSafeSummary(List<Finding> findings) {
        String topics = findings.stream()
                .map(Finding::title)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + "；" + right)
                .orElse("可核验的行业公开信息");

        return "本次公开信息调研形成"
                + findings.size()
                + "条可核验发现，主要涉及："
                + topics
                + "。";
    }

    /**
     * 仅接受可以被 URI 解析且协议为 http/https 的公开网页地址。
     */
    private boolean isValidPublicUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    /**
     * 解析 Agent 返回的业务状态。
     */
    private Status parseStatus(String status) {
        if (Status.SUCCESS.name().equals(status)) {
            return Status.SUCCESS;
        }
        return Status.NO_RELEVANT_INFORMATION;
    }

    /**
     * 外部研究未形成可核验行业信息时的稳定返回。
     */
    private IndustryIntelligenceResponse noRelevantInformation(String summary) {
        return new IndustryIntelligenceResponse(
                Status.NO_RELEVANT_INFORMATION,
                summary,
                List.of(),
                List.of(),
                0
        );
    }

    /**
     * Agent、模型或 MCP 链路异常时的稳定返回。
     */
    private IndustryIntelligenceResponse searchFailed() {
        return new IndustryIntelligenceResponse(
                Status.SEARCH_FAILED,
                "实时行业信息检索暂时不可用，系统未生成自动行业结论。",
                List.of(),
                List.of(),
                0
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 来源校验的内部结果，只在服务层中流转。
     *
     * @param sources         Java校验后的最终来源
     * @param sourceIdMapping Agent临时编号到Java最终编号的映射
     * @param adjusted        是否发生来源过滤或去重
     */
    private record ValidatedSources(
            List<Source> sources,
            Map<String, String> sourceIdMapping,
            boolean adjusted
    ) {
    }

    /**
     * Finding校验的内部结果，只在服务层中流转。
     *
     * @param findings Java校验后的最终发现
     * @param adjusted 是否发生Finding或引用过滤/合并
     */
    private record ValidatedFindings(
            List<Finding> findings,
            boolean adjusted
    ) {
    }
}
