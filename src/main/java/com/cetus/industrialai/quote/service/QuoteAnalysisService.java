package com.cetus.industrialai.quote.service;

import com.cetus.industrialai.quote.model.ExplicitCostVerification;
import com.cetus.industrialai.quote.model.HistoricalQuoteCase;
import com.cetus.industrialai.quote.model.MaterialChangeAssessment;
import com.cetus.industrialai.quote.model.MaterialChangeItem;
import com.cetus.industrialai.quote.model.QuoteRequest;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 报价需求的LLM结构化分析服务。
 *
 * <p>第一次调用千问，详细比较询价产品和K3历史报价快照，
 * 输出逐项物料变化；第二次独立调用只核验询价说明中明确声明的
 * 新增物料成本，作为防止第一次分析漏算的保底依据。</p>
 *
 * <p>该服务不计算最终成本、人工成本和报价。</p>
 */
@Service
public class QuoteAnalysisService {

    private static final Set<String> CHANGE_LEVELS =
            Set.of("NONE", "MINOR", "MEDIUM", "MAJOR");

    private static final Set<String> CHANGE_TYPES = Set.of(
            "UNCHANGED",
            "ADD",
            "REMOVE",
            "REPLACE",
            "QUANTITY",
            "SPEC_CHANGE",
            "UNKNOWN"
    );

    private static final Set<String> COST_SOURCES = Set.of(
            "HISTORICAL_CASE",
            "K3_CURRENT",
            "USER_EXPLICIT",
            "UNKNOWN"
    );

    private static final Set<String> CONFIDENCE_LEVELS =
            Set.of("HIGH", "MEDIUM", "LOW");

    private final ChatModel qwenChatModel;
    private final ObjectMapper objectMapper;
    private final String analysisSystemPrompt;
    private final String costGuardSystemPrompt;

    public QuoteAnalysisService(
            @Qualifier("myQwenChatModel") ChatModel qwenChatModel,
            ObjectMapper objectMapper,
            @Value("classpath:prompts/quote-analysis-system-prompt.txt")
            Resource analysisPromptResource,
            @Value("classpath:prompts/quote-cost-guard-system-prompt.txt")
            Resource costGuardPromptResource
    ) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
        this.analysisSystemPrompt = readSystemPrompt(
                analysisPromptResource,
                "报价差异分析"
        );
        this.costGuardSystemPrompt = readSystemPrompt(
                costGuardPromptResource,
                "新增物料成本核验"
        );
    }

    /**
     * 详细比较当前询价产品和历史报价快照。
     *
     * @param request        当前询价请求
     * @param historicalCase K3历史报价完整快照
     * @return 通过Java基础校验的物料差异分析
     */
    public MaterialChangeAssessment analyze(
            QuoteRequest request,
            HistoricalQuoteCase historicalCase
    ) {
        if (request == null) {
            throw new IllegalArgumentException("当前报价请求不能为空");
        }

        if (historicalCase == null) {
            throw new IllegalArgumentException("历史报价案例不能为空");
        }

        try {
            String userPrompt = """
                    请详细比较以下当前询价请求和K3历史报价快照。
                    必须检查历史物料明细，并逐项说明变化和成本来源。
                    
                    当前询价请求：
                    %s
                    
                    K3历史报价快照：
                    %s
                    """.formatted(
                    objectMapper.writeValueAsString(request),
                    objectMapper.writeValueAsString(historicalCase)
            );

            MaterialChangeAssessment assessment =
                    callAndParse(
                            analysisSystemPrompt,
                            userPrompt,
                            MaterialChangeAssessment.class
                    );

            validateAssessment(request, assessment);
            return assessment;

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "千问返回的物料差异分析无法解析，需要人工审核",
                    exception
            );
        }
    }

    /**
     * 独立核验询价说明中明确新增的物料成本。
     *
     * <p>此调用不接收第一次分析结果，避免直接复述第一次结论。
     * Java随后会用核验金额构造最低材料成本。</p>
     *
     * @param request 当前询价请求
     * @return 独立成本核验结果
     */
    public ExplicitCostVerification verifyExplicitMaterialCosts(
            QuoteRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("当前报价请求不能为空");
        }

        try {
            String userPrompt = """
                    请只核验以下询价说明中的明确新增物料成本。
                    
                    询价说明原文：
                    %s
                    """.formatted(request.requirementDescription());

            ExplicitCostVerification verification =
                    callAndParse(
                            costGuardSystemPrompt,
                            userPrompt,
                            ExplicitCostVerification.class
                    );

            validateCostVerification(request, verification);
            return verification;

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "千问返回的新增物料成本核验结果无法解析，需要人工审核",
                    exception
            );
        }
    }

    /**
     * 调用千问并把JSON结果转换为指定类型。
     */
    private <T> T callAndParse(
            String systemPrompt,
            String userPrompt,
            Class<T> resultType
    ) throws JacksonException {
        String response = qwenChatModel.chat(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
        ).aiMessage().text();

        return objectMapper.readValue(
                extractJson(response),
                resultType
        );
    }

    /**
     * 校验第一次LLM物料差异分析的结构和金额汇总。
     */
    private void validateAssessment(
            QuoteRequest request,
            MaterialChangeAssessment assessment
    ) {
        if (assessment == null) {
            throw new IllegalStateException("报价分析结果不能为空");
        }

        if (!CHANGE_LEVELS.contains(assessment.changeLevel())) {
            throw new IllegalStateException("changeLevel 不合法");
        }

        if (!CONFIDENCE_LEVELS.contains(assessment.confidence())) {
            throw new IllegalStateException("confidence 不合法");
        }

        if (assessment.materialChanges() == null
                || assessment.changeReasons() == null
                || assessment.missingInformation() == null) {
            throw new IllegalStateException(
                    "物料变化、变化原因和缺失信息必须使用数组表示"
            );
        }

        validateNonNegative(
                assessment.explicitAdditionalMaterialCost(),
                "explicitAdditionalMaterialCost"
        );
        validateNonNegative(
                assessment.additionalUnitAmount(),
                "additionalUnitAmount"
        );

        BigDecimal explicitAddedTotal = BigDecimal.ZERO;

        for (MaterialChangeItem item : assessment.materialChanges()) {
            validateMaterialChangeItem(
                    request,
                    item,
                    assessment
            );

            if ("ADD".equals(item.changeType())
                    && "USER_EXPLICIT".equals(item.costSource())
                    && item.costConfirmed()) {
                explicitAddedTotal = explicitAddedTotal.add(
                        item.currentCost()
                );
            }
        }

        if (explicitAddedTotal.compareTo(
                assessment.explicitAdditionalMaterialCost()
        ) != 0) {
            throw new IllegalStateException(
                    "明确新增物料成本合计与物料变化明细不一致"
            );
        }
    }

    /**
     * 校验单项物料变化，阻止无依据的成本降低进入计算工具。
     */
    private void validateMaterialChangeItem(
            QuoteRequest request,
            MaterialChangeItem item,
            MaterialChangeAssessment assessment
    ) {
        if (item == null) {
            throw new IllegalStateException("materialChanges 不能包含null");
        }

        if (!CHANGE_TYPES.contains(item.changeType())) {
            throw new IllegalStateException(
                    "不支持的物料变化类型：" + item.changeType()
            );
        }

        if (!COST_SOURCES.contains(item.costSource())) {
            throw new IllegalStateException(
                    "不支持的物料成本来源：" + item.costSource()
            );
        }

        validateNullableNonNegative(
                item.historicalCost(),
                "historicalCost"
        );
        validateNullableNonNegative(
                item.currentCost(),
                "currentCost"
        );

        if (!"UNCHANGED".equals(item.changeType())
                && (item.evidence() == null
                || item.evidence().isBlank())) {
            throw new IllegalStateException(
                    "发生变化的物料必须提供判断依据"
            );
        }

        if ("USER_EXPLICIT".equals(item.costSource())) {
            if (item.sourceExcerpt() == null
                    || item.sourceExcerpt().isBlank()
                    || !request.requirementDescription().contains(
                    item.sourceExcerpt()
            )) {
                throw new IllegalStateException(
                        "USER_EXPLICIT物料成本必须提供逐字引用的询价原文片段"
                );
            }
        }

        if (item.costConfirmed()
                && item.currentCost() == null) {
            throw new IllegalStateException(
                    "已确认物料成本时currentCost不能为空"
            );
        }

        if (!item.costConfirmed()
                && !assessment.requiresManualReview()) {
            throw new IllegalStateException(
                    "存在未确认物料成本时必须转人工复核"
            );
        }

        switch (item.changeType()) {
            case "UNCHANGED" -> validateUnchangedItem(item);
            case "ADD" -> validateAddedItem(item);
            case "REMOVE" -> validateRemovedItem(item);
            case "REPLACE", "QUANTITY", "SPEC_CHANGE" -> {
                if (item.historicalCost() == null) {
                    throw new IllegalStateException(
                            item.changeType()
                                    + "必须提供历史物料成本"
                    );
                }
            }
            case "UNKNOWN" -> {
                if (!assessment.requiresManualReview()) {
                    throw new IllegalStateException(
                            "存在UNKNOWN变化时必须转人工复核"
                    );
                }
            }
            default -> throw new IllegalStateException(
                    "未处理的物料变化类型：" + item.changeType()
            );
        }
    }

    private void validateUnchangedItem(MaterialChangeItem item) {
        if (item.historicalCost() == null
                || item.currentCost() == null
                || item.historicalCost().compareTo(
                item.currentCost()
        ) != 0
                || !"HISTORICAL_CASE".equals(item.costSource())) {
            throw new IllegalStateException(
                    "UNCHANGED物料必须沿用历史成本"
            );
        }
    }

    private void validateAddedItem(MaterialChangeItem item) {
        if (item.historicalCost() == null
                || item.historicalCost().signum() != 0) {
            throw new IllegalStateException(
                    "ADD物料的historicalCost必须为0"
            );
        }
    }

    private void validateRemovedItem(MaterialChangeItem item) {
        if (item.materialCode() == null
                || item.materialCode().isBlank()
                || item.historicalCost() == null
                || item.currentCost() == null
                || item.currentCost().signum() != 0
                || !"HISTORICAL_CASE".equals(item.costSource())) {
            throw new IllegalStateException(
                    "REMOVE物料必须引用历史物料编码和历史成本"
            );
        }
    }

    /**
     * 校验独立成本核验结果及其原文证据。
     */
    private void validateCostVerification(
            QuoteRequest request,
            ExplicitCostVerification verification
    ) {
        if (verification == null) {
            throw new IllegalStateException("新增物料成本核验结果不能为空");
        }

        validateNonNegative(
                verification.minimumExplicitAddedMaterialCost(),
                "minimumExplicitAddedMaterialCost"
        );

        if (verification.evidence() == null
                || verification.missingInformation() == null) {
            throw new IllegalStateException(
                    "成本核验依据和缺失信息必须使用数组表示"
            );
        }

        if (verification.minimumExplicitAddedMaterialCost().signum() > 0
                && verification.evidence().isEmpty()) {
            throw new IllegalStateException(
                    "核验出新增物料成本时必须提供询价原文依据"
            );
        }

        for (String evidence : verification.evidence()) {
            if (evidence == null
                    || evidence.isBlank()
                    || !request.requirementDescription().contains(evidence)) {
                throw new IllegalStateException(
                        "新增物料成本核验依据不是询价说明原文"
                );
            }
        }

        if (verification.ambiguous()
                && verification.missingInformation().isEmpty()) {
            throw new IllegalStateException(
                    "成本信息存在歧义时必须说明缺失信息"
            );
        }
    }

    private void validateNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null || value.signum() < 0) {
            throw new IllegalStateException(
                    fieldName + "必须是不小于0的金额"
            );
        }
    }

    private void validateNullableNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value != null && value.signum() < 0) {
            throw new IllegalStateException(
                    fieldName + "不能小于0"
            );
        }
    }

    /**
     * 从模型回复中提取JSON对象。
     */
    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("千问没有返回分析结果");
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new IllegalStateException("千问未返回有效JSON对象");
        }

        return response.substring(start, end + 1);
    }

    /**
     * 从resources目录读取系统提示词。
     */
    private String readSystemPrompt(
            Resource resource,
            String promptName
    ) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "无法读取" + promptName + "系统提示词",
                    exception
            );
        }
    }
}
