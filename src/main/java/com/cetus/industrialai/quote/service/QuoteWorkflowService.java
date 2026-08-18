package com.cetus.industrialai.quote.service;

import com.cetus.industrialai.quote.model.ExplicitCostVerification;
import com.cetus.industrialai.quote.model.HistoricalQuoteCase;
import com.cetus.industrialai.quote.model.MaterialChangeAssessment;
import com.cetus.industrialai.quote.model.MaterialChangeItem;
import com.cetus.industrialai.quote.model.QuoteRequest;
import com.cetus.industrialai.quote.model.QuoteResult;
import com.cetus.industrialai.quote.repository.HistoricalQuoteRepository;
import com.cetus.industrialai.quote.calculator.QuoteCalculator;
import com.cetus.industrialai.quote.calculator.QuoteCalculator.CalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 智能报价业务编排服务。
 *
 * <p>负责串联K3历史报价检索、LLM产品差异分析、
 * 独立新增物料成本核验、Java确定性计算和保底校验。</p>
 */
@Service
public class QuoteWorkflowService {

    private final HistoricalQuoteRepository historicalQuoteTool;
    private final QuoteAnalysisService quoteAnalysisService;
    private final QuoteCalculator quoteCalculationTool;

    public QuoteWorkflowService(
            HistoricalQuoteRepository historicalQuoteTool,
            QuoteAnalysisService quoteAnalysisService,
            QuoteCalculator quoteCalculationTool
    ) {
        this.historicalQuoteTool = historicalQuoteTool;
        this.quoteAnalysisService = quoteAnalysisService;
        this.quoteCalculationTool = quoteCalculationTool;
    }

    /**
     * 执行一次完整智能报价流程。
     *
     * @param request 当前询价请求
     * @return 报价、物料差异、保底结果和审计步骤
     */
    public QuoteResult generateQuote(QuoteRequest request) {
        List<String> agentSteps = new ArrayList<>();

        List<HistoricalQuoteCase> similarCases =
                historicalQuoteTool.searchSimilarCases(request);

        agentSteps.add(
                "检索K3历史报价快照，共找到 "
                        + similarCases.size()
                        + " 个候选案例"
        );

        if (similarCases.isEmpty()) {
            return buildNoReferenceResult(request, agentSteps);
        }

        HistoricalQuoteCase referenceCase = similarCases.get(0);

        agentSteps.add(
                "选择历史案例 "
                        + referenceCase.caseId()
                        + "，直接读取K3物料成本 "
                        + referenceCase.totalMaterialCost()
        );

        MaterialChangeAssessment assessment = null;
        ExplicitCostVerification verification = null;

        try {
            assessment = quoteAnalysisService.analyze(
                    request,
                    referenceCase
            );

            agentSteps.add(
                    "千问完成逐项物料差异分析，共识别 "
                            + assessment.materialChanges().size()
                            + " 项物料变化"
            );

            verification =
                    quoteAnalysisService.verifyExplicitMaterialCosts(
                            request
                    );

            agentSteps.add(
                    "千问独立核验明确新增物料成本，最低合计为 "
                            + verification
                            .minimumExplicitAddedMaterialCost()
            );

            CalculationResult calculation =
                    quoteCalculationTool.calculate(
                            request,
                            referenceCase,
                            assessment,
                            verification
                    );

            agentSteps.add(
                    calculation.materialCostFloorTriggered()
                            ? "Java保底规则已抬升材料成本，防止明确新增成本漏算"
                            : "Java完成历史物料核对和最低材料成本校验"
            );

            agentSteps.add(
                    "Java报价工具完成10PCS、1K、5K三阶梯计算"
            );

            return buildQuoteResult(
                    referenceCase,
                    assessment,
                    verification,
                    calculation,
                    agentSteps
            );

        } catch (IllegalArgumentException
                 | IllegalStateException exception) {
            agentSteps.add(
                    "自动报价中止，转入人工复核："
                            + exception.getMessage()
            );

            return buildFailureResult(
                    request,
                    referenceCase,
                    assessment,
                    verification,
                    exception.getMessage(),
                    agentSteps
            );
        }
    }

    /**
     * 组装成功执行后的完整报价结果。
     */
    private QuoteResult buildQuoteResult(
            HistoricalQuoteCase referenceCase,
            MaterialChangeAssessment assessment,
            ExplicitCostVerification verification,
            CalculationResult calculation,
            List<String> agentSteps
    ) {
        String decision = resolveDecision(assessment);
        List<String> risks = buildRisks(
                assessment,
                calculation
        );

        List<String> missingInformation = new ArrayList<>();
        missingInformation.addAll(assessment.missingInformation());
        missingInformation.addAll(verification.missingInformation());

        String summary = switch (decision) {
            case "DIRECT_REFERENCE" -> "询价产品与历史案例差异较小，已使用K3真实物料成本生成报价。";
            case "ADJUSTED_REFERENCE" -> "已根据逐项物料变化调整K3历史成本，并生成三阶梯报价。";
            default -> "已生成初步报价，但仍需人工确认物料变化和成本依据。";
        };

        String reviewSuggestion =
                assessment.requiresManualReview()
                        ? "请核对物料变化、当前物料价格和缺失信息后再使用报价。"
                        : "当前物料成本已经通过历史快照及保底规则校验。";

        String analysisReport = """
                参考K3历史案例：%s
                K3报价版本：%s
                产品变化程度：%s
                制造难度：%s
                标准生产效率：%s PCS/小时
                历史真实物料成本：%s
                已确认新增物料成本：%s
                最低物料成本：%s
                当前采用物料成本：%s
                是否触发保底：%s
                单件人工成本：%s
                10PCS样品单价：%s
                1K小批量单价：%s ～ %s
                5K量产单价：%s ～ %s
                非物料类单件固定费用：%s
                """.formatted(
                referenceCase.caseId(),
                referenceCase.quoteVersion(),
                assessment.changeLevel(),
                calculation.manufacturingDifficulty(),
                calculation.productionEfficiency(),
                calculation.historicalMaterialCost(),
                calculation.confirmedAddedMaterialCost(),
                calculation.materialCostFloor(),
                calculation.currentMaterialCost(),
                calculation.materialCostFloorTriggered(),
                calculation.laborCost(),
                calculation.samplePrice(),
                calculation.smallBatchPriceLow(),
                calculation.smallBatchPriceHigh(),
                calculation.massProductionPriceLow(),
                calculation.massProductionPriceHigh(),
                calculation.additionalUnitAmount()
        );

        return new QuoteResult(
                decision,
                summary,
                referenceCase,
                assessment.changeLevel(),
                assessment.changeReasons(),
                assessment.materialChanges(),
                calculation.manufacturingDifficulty(),
                assessment.confidence(),
                assessment.requiresManualReview(),
                calculation.historicalMaterialCost(),
                calculation.currentMaterialCost(),
                calculation.materialCostFloor(),
                calculation.confirmedAddedMaterialCost(),
                calculation.materialCostFloorTriggered(),
                calculation.additionalUnitAmount(),
                calculation.productionEfficiency(),
                calculation.laborCost(),
                calculation.samplePrice(),
                calculation.smallBatchPriceLow(),
                calculation.smallBatchPriceHigh(),
                calculation.massProductionPriceLow(),
                calculation.massProductionPriceHigh(),
                risks,
                List.copyOf(missingInformation),
                reviewSuggestion,
                List.copyOf(agentSteps),
                analysisReport
        );
    }

    private String resolveDecision(
            MaterialChangeAssessment assessment
    ) {
        if (assessment.requiresManualReview()) {
            return "MANUAL_REVIEW";
        }

        if ("NONE".equals(assessment.changeLevel())) {
            return "DIRECT_REFERENCE";
        }

        return "ADJUSTED_REFERENCE";
    }

    /**
     * 根据LLM判断和Java保底结果生成风险提示。
     */
    private List<String> buildRisks(
            MaterialChangeAssessment assessment,
            CalculationResult calculation
    ) {
        List<String> risks = new ArrayList<>();

        if (assessment.requiresManualReview()) {
            risks.add("当前物料差异分析需要人工复核");
        }

        if ("LOW".equals(assessment.confidence())) {
            risks.add("千问对当前物料差异判断的可信度较低");
        }

        if ("MAJOR".equals(assessment.changeLevel())) {
            risks.add("询价产品与历史产品存在较大差异");
        }

        if (calculation.confirmedAddedMaterialCost()
                .compareTo(BigDecimal.ZERO) > 0) {
            risks.add(
                    "当前材料成本已包含新增物料成本 "
                            + calculation.confirmedAddedMaterialCost()
            );
        }

        if (calculation.materialCostFloorTriggered()) {
            risks.add("第一次差异分析存在漏算，已触发材料成本保底");
        }

        if (calculation.additionalUnitAmount()
                .compareTo(BigDecimal.ZERO) > 0) {
            risks.add(
                    "最终报价已包含非物料类单件固定费用 "
                            + calculation.additionalUnitAmount()
            );
        }

        return List.copyOf(risks);
    }

    /**
     * 构造没有K3历史报价快照时的结果。
     */
    private QuoteResult buildNoReferenceResult(
            QuoteRequest request,
            List<String> agentSteps
    ) {
        agentSteps.add("没有历史报价快照，停止自动金额计算");

        return new QuoteResult(
                "NEED_MORE_INFO",
                "没有找到可参考的K3历史报价，暂时无法自动报价。",
                null,
                null,
                List.of(),
                List.of(),
                request.manufacturingDifficulty(),
                "LOW",
                true,
                null,
                null,
                null,
                null,
                false,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("缺少可审计的历史物料成本依据"),
                List.of("K3历史报价、物料明细和物料总成本"),
                "请补充K3历史报价快照，或者由业务人员人工报价。",
                List.copyOf(agentSteps),
                "产品“%s”未找到K3历史报价快照，未执行自动报价。"
                        .formatted(request.productModel())
        );
    }

    /**
     * 构造分析、核验或计算失败时的人工复核结果。
     *
     * <p>如果异常发生前已经取得LLM差异分析或成本核验结果，
     * 继续保留这些中间数据，供业务人员定位缺失物料和成本依据；
     * 未安全完成的成本、人工和最终价格仍保持为空。</p>
     */
    private QuoteResult buildFailureResult(
            QuoteRequest request,
            HistoricalQuoteCase referenceCase,
            MaterialChangeAssessment assessment,
            ExplicitCostVerification verification,
            String errorMessage,
            List<String> agentSteps
    ) {
        String changeLevel = assessment == null
                ? null
                : assessment.changeLevel();

        List<String> changeReasons = assessment == null
                ? List.of()
                : assessment.changeReasons();

        List<MaterialChangeItem> materialChanges = assessment == null
                ? List.of()
                : assessment.materialChanges();

        String confidence = assessment == null
                ? "LOW"
                : assessment.confidence();

        BigDecimal confirmedAddedMaterialCost =
                verification != null && !verification.ambiguous()
                        ? verification.minimumExplicitAddedMaterialCost()
                        : null;

        BigDecimal additionalUnitAmount = assessment == null
                ? BigDecimal.ZERO
                : assessment.additionalUnitAmount();

        Set<String> missingInformation = new LinkedHashSet<>();
        if (assessment != null) {
            missingInformation.addAll(assessment.missingInformation());
        }
        if (verification != null) {
            missingInformation.addAll(verification.missingInformation());
        }

        List<String> risks = new ArrayList<>();
        risks.add(errorMessage);
        if (assessment != null && assessment.requiresManualReview()) {
            risks.add("当前物料差异分析需要人工复核");
        }
        if (verification != null && verification.ambiguous()) {
            risks.add("询价说明中的新增物料成本信息存在歧义");
        }

        String reviewSuggestion = missingInformation.isEmpty()
                ? "请核对K3物料快照和LLM分析结果后再继续报价。"
                : "请补充缺失的物料成本信息，核对无误后重新报价。";

        String analysisReport = """
                自动报价未能安全完成，已保留可用于人工复核的中间结果。
                参考K3历史案例：%s
                产品变化程度：%s
                已识别物料变化数量：%s
                已确认新增物料成本：%s
                缺失信息：%s
                中止原因：%s
                """.formatted(
                referenceCase.caseId(),
                changeLevel == null ? "未完成" : changeLevel,
                materialChanges.size(),
                confirmedAddedMaterialCost == null
                        ? "尚未确认"
                        : confirmedAddedMaterialCost,
                missingInformation.isEmpty()
                        ? "无"
                        : String.join("；", missingInformation),
                errorMessage
        );

        return new QuoteResult(
                "MANUAL_REVIEW",
                "自动报价未能安全完成，需要人工复核。",
                referenceCase,
                changeLevel,
                List.copyOf(changeReasons),
                List.copyOf(materialChanges),
                request.manufacturingDifficulty(),
                confidence,
                true,
                referenceCase.totalMaterialCost(),
                null,
                null,
                confirmedAddedMaterialCost,
                false,
                additionalUnitAmount,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.copyOf(risks),
                List.copyOf(missingInformation),
                reviewSuggestion,
                List.copyOf(agentSteps),
                analysisReport
        );
    }
}
