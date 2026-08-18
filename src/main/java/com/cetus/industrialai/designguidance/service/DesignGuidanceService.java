package com.cetus.industrialai.designguidance.service;

import com.cetus.industrialai.designguidance.generation.DesignGuidanceGenerator;
import com.cetus.industrialai.designguidance.generation.DesignGuidanceGenerator.GuidanceDraft;
import com.cetus.industrialai.designguidance.generation.DesignGuidanceGenerator.RecommendationDraft;
import com.cetus.industrialai.designguidance.model.DesignGuidanceRequest;
import com.cetus.industrialai.designguidance.model.DesignGuidanceResponse;
import com.cetus.industrialai.designguidance.model.DesignGuidanceResponse.Recommendation;
import com.cetus.industrialai.designguidance.model.DesignGuidanceResponse.Source;
import com.cetus.industrialai.designguidance.model.DesignGuidanceResponse.Status;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 工程设计指导模块的 RAG 编排服务。
 *
 * <p>负责组合检索问题、调用知识库、处理无证据降级、
 * 调用千问生成建议，并校验模型引用的来源编号。</p>
 */
@Service
@Slf4j
public class DesignGuidanceService {

    private static final int MAX_EXCERPT_LENGTH = 240;

    private final ContentRetriever contentRetriever;
    private final DesignGuidanceGenerator designGuidanceGenerator;

    public DesignGuidanceService(
            ContentRetriever contentRetriever,
            DesignGuidanceGenerator designGuidanceGenerator
    ) {
        this.contentRetriever = contentRetriever;
        this.designGuidanceGenerator = designGuidanceGenerator;
    }

    /**
     * 执行工程指导完整链路。
     *
     * <p>检索不到达到阈值的知识片段时直接返回，
     * 不调用千问，避免模型在没有企业资料依据时凭空给出建议。</p>
     */
    public DesignGuidanceResponse analyze(DesignGuidanceRequest request) {
        // -------- 第一阶段：组装检索词 --------
        // 将产品描述、阶段、问题、已知条件拼装成一个结构化的自然语言问题，
        // 用于后续向量检索，使语义匹配更准确。
        String retrievalQuestion = buildRetrievalQuestion(request);
        List<Content> contents;

        // -------- 第二阶段：执行向量检索 --------
        // 调用 ContentRetriever（检索/搜索引擎），内部将检索词转为向量，
        // 与内存中的知识库向量做相似度计算，并按 minScore(0.75) 过滤。
        // 仅保留最相似的 5 个片段（maxResults）。
        try {
            contents = contentRetriever.retrieve(new Query(retrievalQuestion));
        } catch (RuntimeException exception) {
            log.warn("工程指导知识检索失败", exception);
            return manualReview(
                    "知识检索服务暂时不可用，系统未生成自动工程建议。",
                    List.of(),
                    "当前无法取得可核验的知识库证据"
            );
        }

        // -------- 第三阶段：检查证据是否存在（成本控制关键点） --------
        // 如果没有查到任何达到阈值的片段，说明用户问题与企业知识库完全不相关。
        // 此处直接返回，绝不调用昂贵的千问 Chat 模型，省下 Token 消耗。
        if (contents == null || contents.isEmpty()) {
            return noRelevantEvidence();
        }

        // -------- 第四阶段：数据清洗与结构化（证据卡片化） --------
        // 将检索到的原始数据（Content）转换为前端展示用的 Source 卡片列表，
        // 并统一编号 S1、S2...，同时提取标题、章节和摘要预览。
        List<Source> sources = buildSources(contents);
        GuidanceDraft draft;

        // -------- 第五阶段：调用生成模型（LLM 推理） --------
        // 将用户问题与 S1、S2 等证据拼接成完整提示词，调用千问 Chat 模型。
        // 模型必须按 GuidanceDraft Record 的格式返回结构化的 JSON。
        try {
            draft = designGuidanceGenerator.generate(
                    buildEvidencePrompt(request, contents, sources)
            );
        } catch (RuntimeException exception) {
            log.warn("工程指导模型生成或结构化响应解析失败", exception);
            return manualReview(
                    "模型服务暂时不可用，已保留本次召回的工程资料。",
                    sources,
                    "自动建议未完成，请稍后重试或由工程师查看召回资料"
            );
        }

        // -------- 第六阶段：校验与最终组装 --------
        // 1. 校验模型返回的 basisSourceIds 是否都在有效的 S1~S3 范围内。
        // 2. 过滤掉没有真实证据支撑的虚假建议。
        // 3. 将模型草稿（GuidanceDraft）和来源卡片（sources）合并，
        // 生成最终的 DesignGuidanceResponse 返回给前端。
        return buildValidatedResponse(draft, sources);
    }

    /**
     * 构建用于向量检索的结构化查询文本（语义探针）。
     *
     * <p>该方法将请求中的四个关键信息（产品描述、工程阶段、工程问题、已知条件）
     * 拼接为键值对格式的字符串。这种结构化的自然语言表述有助于 Embedding 模型
     * 更精准地捕获语义焦点，从而在知识库中检索出相关性更高的片段。</p>
     */
    private String buildRetrievalQuestion(DesignGuidanceRequest request) {
        String knownConditions = hasText(request.knownConditions())
                ? request.knownConditions().trim()
                : "未提供";

        return "产品描述：" + request.productDescription().trim()
                + "\n工程阶段：" + request.engineeringStage()
                + "\n工程问题：" + request.question().trim()
                + "\n已知条件：" + knownConditions;
    }

    /**
     * 将检索结果转换为前端展示用的来源卡片列表。
     *
     * <p>按检索顺序为每条片段编号 S1、S2…，提取文件名、章节标题，并生成摘要预览。</p>
     *
     * @param contents 检索返回的原始内容列表
     * @return 带编号和结构化信息的来源卡片列表
     */
    private List<Source> buildSources(List<Content> contents) {
        List<Source> sources = new ArrayList<>();

        for (int index = 0; index < contents.size(); index++) {
            // 拆包（contents）取货（textSegment）
            TextSegment segment = contents.get(index).textSegment();
            // 给检索到的每条资料打上 S1、S2……的标签
            String sourceId = "S" + (index + 1);
            // 看标签（元数据K-V）: 看文件名，锁定参考资料
            String title = readMetadata(segment.metadata(), "file_name", "工程知识资料");
            // 看商品（正文）: 看标题，锁定引用正文，extractSection 会去扫描文本中以 # 开头的行
            String section = extractSection(segment.text());
            // 截取摘要：去除标签，将引用资料压缩，设定输出格式
            String excerpt = abbreviate(removeLeadingFileName(segment.text(), title));
            // 加载处理过的新数据
            sources.add(new Source(sourceId, title, section, excerpt));
        }

        return List.copyOf(sources);
    }

    /**
     * 构建发送给 LLM 的 "用户提示词""。
     *
     * <p>此处 "用户提示词" 包含两部分：
     * 用户工程问题（结构化查询文本）+ 知识库证据（编号 S1..S3，含来源标题）。
     * 后续还会结合编排的 "系统提示词" ，一并给 LLM 处理。
     * </p>
     *
     * @param request  原始用户请求
     * @param contents 检索返回的原始内容列表
     * @param sources  来源卡片列表（含编号和标题）
     * @return 拼接好的 "用户提示词"
     */
    private String buildEvidencePrompt(
            DesignGuidanceRequest request,
            List<Content> contents,
            List<Source> sources
    ) {
        // 第一部分：用户问题
        StringBuilder prompt = new StringBuilder();
        prompt.append("【用户工程问题】\n")
                .append(buildRetrievalQuestion(request))
                .append("\n\n【知识库证据】\n");

        // 第二部分：证据片段（编号 + 来源 + 正文）
        for (int index = 0; index < contents.size(); index++) {
            prompt.append('[')
                    .append(sources.get(index).sourceId())
                    .append("] 来源：")
                    .append(sources.get(index).title())
                    .append('\n')
                    .append(contents.get(index).textSegment().text())
                    .append("\n\n");
        }

        return prompt.toString();
    }

    /**
     * 校验模型输出并组装最终响应。
     *
     * <p>核心逻辑：以 sources 的编号为白名单，过滤掉模型中引用无效编号的建议。
     * 若过滤后无有效建议，状态自动降级为 NEED_MORE_INFO 并追加提示信息。</p>
     *
     * @param draft   大模型返回的结构化草稿（可能为 null）
     * @param sources 有效来源卡片列表（编号 S1..S5）
     * @return 校验合格且结构完整的最终响应
     */
    private DesignGuidanceResponse buildValidatedResponse(
            GuidanceDraft draft,
            List<Source> sources
    ) {
        // 构建有效编号白名单
        Set<String> validSourceIds = new LinkedHashSet<>();
        sources.forEach(source -> validSourceIds.add(source.sourceId()));

        // 过滤掉引用无效编号的建议
        List<Recommendation> recommendations = validateRecommendations(
                draft == null ? null : draft.recommendations(),
                validSourceIds
        );

        // 复制缺失信息列表（可变）
        List<String> missingInformation = mutableCopy(
                draft == null ? null : draft.missingInformation()
        );

        // 解析状态，若无有效建议则降级
        Status status = parseStatus(draft == null ? null : draft.status());
        if (recommendations.isEmpty()) {
            status = Status.NEED_MORE_INFO;
            missingInformation.add("未生成具有有效知识库引用的建议，需要补充条件或人工复核");
        }

        // 摘要有则用，无则取默认值
        String summary = draft != null && hasText(draft.summary())
                ? draft.summary().trim()
                : "现有证据不足以形成可靠的工程指导。";

        return new DesignGuidanceResponse(
                status,
                summary,
                recommendations,
                immutableCopy(draft == null ? null : draft.risks()),
                List.copyOf(missingInformation),
                sources,
                sources.size()
        );
    }

    /**
     * 校验并过滤模型生成的建议草稿。
     *
     * <p>核心逻辑：丢弃没有有效来源编号（不在 validSourceIds 内）的建议，
     * 并对 step 字段进行补全或修正（无效或空时自动递增补位）。</p>
     *
     * @param drafts         模型生成的原始建议草稿列表
     * @param validSourceIds 本次召回的有效来源编号集合（如 S1, S2...）
     * @return 校验通过且包含有效引用的建议列表
     */
    private List<Recommendation> validateRecommendations(
            List<RecommendationDraft> drafts,
            Set<String> validSourceIds
    ) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }

        List<Recommendation> validated = new ArrayList<>();
        int fallbackStep = 1;

        for (RecommendationDraft draft : drafts) {
            if (draft == null || !hasText(draft.action())) {
                continue;
            }

            // 过滤出存在于白名单中的来源编号（去重）
            List<String> validIds = draft.basisSourceIds() == null
                    ? List.of()
                    : draft.basisSourceIds().stream()
                    .filter(validSourceIds::contains)
                    .distinct()
                    .toList();

            // 没有真实来源支撑的无效建议，直接丢弃
            if (validIds.isEmpty()) {
                continue;
            }

            // step 有效则用原值，无效则用 fallbackStep
            int step = draft.step() != null && draft.step() > 0
                    ? draft.step()
                    : fallbackStep;
            validated.add(new Recommendation(step, draft.action().trim(), validIds));
            fallbackStep++;
        }

        return List.copyOf(validated);
    }

    /**
     * 解析模型输出的状态字符串。
     *
     * <p>只有明确返回 GUIDANCE_PROVIDED 时才保留，
     * 其余情况（包括 null 或非法值）统一降级为 NEED_MORE_INFO。</p>
     *
     * @param status 模型返回的状态字符串
     * @return 解析后的状态枚举
     */
    private Status parseStatus(String status) {
        if (Status.GUIDANCE_PROVIDED.name().equals(status)) {
            return Status.GUIDANCE_PROVIDED;
        }
        return Status.NEED_MORE_INFO;
    }

    /**
     * 检索无结果时直接返回，不调用大模型。
     *
     * <p>避免模型在无事实依据时凭空生成建议，同时给出明确的改进指引。</p>
     *
     * @return 标记为 NO_RELEVANT_EVIDENCE 的响应
     */
    private DesignGuidanceResponse noRelevantEvidence() {
        return new DesignGuidanceResponse(
                Status.NO_RELEVANT_EVIDENCE,
                "知识库中未检索到足够相关的工程资料，系统未调用大模型生成建议。",
                List.of(),
                List.of("缺少可核验的企业知识依据，不建议直接采用通用模型经验回答"),
                List.of("请补充更具体的产品结构、测试现象或对应工程资料"),
                List.of(),
                0
        );
    }

    /**
     * 检索或模型服务异常时保留证据，转人工审核。
     *
     * @param summary 异常情况摘要
     * @param sources 已召回的知识片段（可能为空）
     * @param risk    提示用户注意的安全风险或限制
     * @return 标记为 MANUAL_REVIEW 状态的响应
     */
    private DesignGuidanceResponse manualReview(
            String summary,
            List<Source> sources,
            String risk
    ) {
        return new DesignGuidanceResponse(
                Status.MANUAL_REVIEW, // 标记需人工介入
                summary,
                List.of(),
                List.of(risk),
                List.of(),
                sources,
                sources.size()
        );
    }

    /**
     * 从元数据中安全读取指定键的值，若不存在则返回默认值。
     */
    private String readMetadata(Metadata metadata, String key, String fallback) {
        if (metadata == null) {
            return fallback;
        }
        String value = metadata.getString(key);
        return hasText(value) ? value : fallback;
    }

    /**
     * 从正文中提取首个 Markdown 标题（以 # 开头的行），去除 # 前缀后返回。
     * 若找不到标题，返回默认文本。
     */
    private String extractSection(String text) {
        if (!hasText(text)) {
            return "相关内容";
        }
        return text.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("#"))
                .map(line -> line.replaceFirst("^#+\\s*", ""))
                .filter(this::hasText)
                .findFirst()
                .orElse("相关内容");
    }

    /**
     * 移除文本开头的文件名前缀（构建知识库时拼接的内容）。
     * 若文本不以指定标题开头，则原样返回。
     */
    private String removeLeadingFileName(String text, String title) {
        if (!hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (hasText(title) && trimmed.startsWith(title)) {
            return trimmed.substring(title.length()).stripLeading();
        }
        return trimmed;
    }

    /**
     * 将文本中所有连续空白符压缩为单个空格，并截断至指定长度。
     */
    private String abbreviate(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_EXCERPT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_EXCERPT_LENGTH) + "…";
    }

    private List<String> mutableCopy(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values.stream().filter(this::hasText).map(String::trim).toList());
    }

    private List<String> immutableCopy(List<String> values) {
        return List.copyOf(mutableCopy(values));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
