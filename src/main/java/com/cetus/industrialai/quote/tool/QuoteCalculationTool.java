package com.cetus.industrialai.quote.tool;

import com.cetus.industrialai.quote.model.ExplicitCostVerification;
import com.cetus.industrialai.quote.model.HistoricalMaterialItem;
import com.cetus.industrialai.quote.model.HistoricalQuoteCase;
import com.cetus.industrialai.quote.model.MaterialChangeAssessment;
import com.cetus.industrialai.quote.model.MaterialChangeItem;
import com.cetus.industrialai.quote.model.QuoteRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

/**
 * 确定性报价计算和材料成本保底工具。
 *
 * <p>历史物料成本直接读取K3报价快照，不再通过历史售价反推。
 * LLM负责输出物料变化，Java逐项核对历史物料、计算新成本，
 * 并使用独立成本核验结果构造最低材料成本。</p>
 *
 * <p>该工具不理解自然语言，也不调用LLM。</p>
 */
@Component
public class QuoteCalculationTool {

    private static final BigDecimal HOURLY_LABOR_COST =
            new BigDecimal("30");

    private static final BigDecimal SAMPLE_MARKUP =
            new BigDecimal("2.00");

    private static final BigDecimal SMALL_BATCH_MARKUP_LOW =
            new BigDecimal("1.40");

    private static final BigDecimal SMALL_BATCH_MARKUP_HIGH =
            new BigDecimal("1.45");

    private static final BigDecimal MASS_PRODUCTION_MARKUP_LOW =
            new BigDecimal("1.35");

    private static final BigDecimal MASS_PRODUCTION_MARKUP_HIGH =
            new BigDecimal("1.40");

    /**
     * 执行一次确定性报价计算。
     *
     * @param request       当前询价请求
     * @param referenceCase K3历史报价快照
     * @param assessment    LLM物料差异分析
     * @param verification  独立新增物料成本核验结果
     * @return 包含成本保底和三阶梯报价的计算结果
     */
    public CalculationResult calculate(
            QuoteRequest request,
            HistoricalQuoteCase referenceCase,
            MaterialChangeAssessment assessment,
            ExplicitCostVerification verification
    ) {
        validateInput(
                request,
                referenceCase,
                assessment,
                verification
        );

        validateHistoricalSnapshot(referenceCase);

        String difficulty = resolveDifficulty(request);
        BigDecimal productionEfficiency =
                resolveEfficiency(difficulty);

        BigDecimal historicalMaterialCost =
                referenceCase.totalMaterialCost();

        MaterialCostBreakdown materialCostBreakdown =
                calculateMaterialCost(
                        referenceCase,
                        assessment,
                        verification
                );

        BigDecimal currentMaterialCost =
                materialCostBreakdown.calculatedMaterialCost().max(
                        materialCostBreakdown.materialCostFloor()
                );

        boolean materialCostFloorTriggered =
                currentMaterialCost.compareTo(
                        materialCostBreakdown.calculatedMaterialCost()
                ) > 0;

        BigDecimal additionalUnitAmount =
                assessment.additionalUnitAmount();

        BigDecimal lossFactor =
                BigDecimal.ONE.add(request.lossRate());

        BigDecimal laborCost = HOURLY_LABOR_COST.divide(
                productionEfficiency,
                8,
                RoundingMode.HALF_UP
        );

        BigDecimal samplePrice = calculatePrice(
                currentMaterialCost,
                laborCost,
                lossFactor,
                SAMPLE_MARKUP,
                additionalUnitAmount
        );

        BigDecimal smallBatchPriceLow = calculatePrice(
                currentMaterialCost,
                laborCost,
                lossFactor,
                SMALL_BATCH_MARKUP_LOW,
                additionalUnitAmount
        );

        BigDecimal smallBatchPriceHigh = calculatePrice(
                currentMaterialCost,
                laborCost,
                lossFactor,
                SMALL_BATCH_MARKUP_HIGH,
                additionalUnitAmount
        );

        BigDecimal massProductionPriceLow = calculatePrice(
                currentMaterialCost,
                laborCost,
                lossFactor,
                MASS_PRODUCTION_MARKUP_LOW,
                additionalUnitAmount
        );

        BigDecimal massProductionPriceHigh = calculatePrice(
                currentMaterialCost,
                laborCost,
                lossFactor,
                MASS_PRODUCTION_MARKUP_HIGH,
                additionalUnitAmount
        );

        return new CalculationResult(
                round(historicalMaterialCost),
                round(currentMaterialCost),
                round(materialCostBreakdown.materialCostFloor()),
                round(materialCostBreakdown.confirmedAddedMaterialCost()),
                materialCostFloorTriggered,
                difficulty,
                round(productionEfficiency),
                round(laborCost),
                round(additionalUnitAmount),
                samplePrice,
                smallBatchPriceLow,
                smallBatchPriceHigh,
                massProductionPriceLow,
                massProductionPriceHigh
        );
    }

    /**
     * 根据物料变化明细计算当前成本和最低成本。
     *
     * <p>删除、替换和用量变化只有在能够匹配历史物料编码及历史成本时
     * 才允许扣减；明确新增成本取第一次分析和独立核验中的较大值，
     * 防止第一次LLM分析漏掉用户明确声明的新增物料费用。</p>
     */
    private MaterialCostBreakdown calculateMaterialCost(
            HistoricalQuoteCase referenceCase,
            MaterialChangeAssessment assessment,
            ExplicitCostVerification verification
    ) {
        BigDecimal verifiedHistoricalReduction = BigDecimal.ZERO;
        BigDecimal confirmedChangedMaterialCost = BigDecimal.ZERO;
        BigDecimal reportedAddedMaterialCost = BigDecimal.ZERO;

        for (MaterialChangeItem item : assessment.materialChanges()) {
            switch (item.changeType()) {
                case "UNCHANGED" -> verifyHistoricalCost(
                        referenceCase,
                        item
                );

                case "ADD" -> {
                    requireConfirmedCurrentCost(item);
                    reportedAddedMaterialCost =
                            reportedAddedMaterialCost.add(
                                    item.currentCost()
                            );
                }

                case "REMOVE" -> {
                    BigDecimal historicalCost = verifyHistoricalCost(
                            referenceCase,
                            item
                    );
                    verifiedHistoricalReduction =
                            verifiedHistoricalReduction.add(
                                    historicalCost
                            );
                }

                case "REPLACE", "QUANTITY", "SPEC_CHANGE" -> {
                    BigDecimal historicalCost = verifyHistoricalCost(
                            referenceCase,
                            item
                    );
                    requireConfirmedCurrentCost(item);

                    verifiedHistoricalReduction =
                            verifiedHistoricalReduction.add(
                                    historicalCost
                            );
                    confirmedChangedMaterialCost =
                            confirmedChangedMaterialCost.add(
                                    item.currentCost()
                            );
                }

                case "UNKNOWN" -> throw new IllegalArgumentException(
                        "存在无法确认的物料变化，需要人工复核"
                );

                default -> throw new IllegalArgumentException(
                        "不支持的物料变化类型：" + item.changeType()
                );
            }
        }

        BigDecimal independentlyVerifiedAddedCost =
                verification.minimumExplicitAddedMaterialCost();

        BigDecimal confirmedAddedMaterialCost =
                reportedAddedMaterialCost.max(
                        independentlyVerifiedAddedCost
                );

        BigDecimal baseAfterVerifiedChanges =
                referenceCase.totalMaterialCost()
                        .subtract(verifiedHistoricalReduction)
                        .add(confirmedChangedMaterialCost);

        BigDecimal calculatedMaterialCost =
                baseAfterVerifiedChanges.add(
                        reportedAddedMaterialCost
                );

        BigDecimal materialCostFloor =
                baseAfterVerifiedChanges.add(
                        confirmedAddedMaterialCost
                );

        if (baseAfterVerifiedChanges.signum() < 0
                || calculatedMaterialCost.signum() < 0
                || materialCostFloor.signum() < 0) {
            throw new IllegalArgumentException(
                    "物料变化导致材料成本小于0，需要人工复核"
            );
        }

        return new MaterialCostBreakdown(
                calculatedMaterialCost,
                materialCostFloor,
                confirmedAddedMaterialCost
        );
    }

    /**
     * 核对LLM引用的历史物料编码和历史成本。
     */
    private BigDecimal verifyHistoricalCost(
            HistoricalQuoteCase referenceCase,
            MaterialChangeItem item
    ) {
        if (item.materialCode() == null
                || item.materialCode().isBlank()) {
            throw new IllegalArgumentException(
                    item.changeType() + "缺少历史物料编码"
            );
        }

        HistoricalMaterialItem historicalItem =
                referenceCase.materialItems().stream()
                        .filter(candidate -> candidate.materialCode()
                                .equalsIgnoreCase(item.materialCode()))
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "历史报价中不存在物料："
                                                + item.materialCode()
                                )
                        );

        if (item.historicalCost() == null
                || item.historicalCost().compareTo(
                historicalItem.totalCost()
        ) != 0) {
            throw new IllegalArgumentException(
                    "物料 " + item.materialCode()
                            + " 的历史成本与K3快照不一致"
            );
        }

        return historicalItem.totalCost();
    }

    /**
     * 确认变化后的物料成本具有可靠来源。
     */
    private void requireConfirmedCurrentCost(
            MaterialChangeItem item
    ) {
        if (!item.costConfirmed()
                || item.currentCost() == null
                || item.currentCost().signum() < 0
                || !"USER_EXPLICIT".equals(item.costSource())) {
            throw new IllegalArgumentException(
                    "物料 " + item.materialName()
                            + " 缺少当前版本可核验的用户明确成本；"
                            + "接入K3当前价格查询后可扩展K3_CURRENT"
            );
        }
    }

    /**
     * 校验K3历史物料总成本等于物料明细合计。
     */
    private void validateHistoricalSnapshot(
            HistoricalQuoteCase referenceCase
    ) {
        if (referenceCase.materialItems() == null
                || referenceCase.materialItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "历史报价缺少物料明细"
            );
        }

        BigDecimal itemTotal = referenceCase.materialItems().stream()
                .map(HistoricalMaterialItem::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (itemTotal.compareTo(referenceCase.totalMaterialCost()) != 0) {
            throw new IllegalArgumentException(
                    "K3历史物料总成本与物料明细合计不一致"
            );
        }
    }

    /**
     * 最终价格计算。
     *
     * <pre>
     * （当前物料成本 + 单件人工成本）
     * × 损耗系数
     * × 加成系数
     * + 非物料类单件固定费用
     * </pre>
     */
    private BigDecimal calculatePrice(
            BigDecimal materialCost,
            BigDecimal laborCost,
            BigDecimal lossFactor,
            BigDecimal markupFactor,
            BigDecimal additionalUnitAmount
    ) {
        return materialCost
                .add(laborCost)
                .multiply(lossFactor)
                .multiply(markupFactor)
                .add(additionalUnitAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveDifficulty(QuoteRequest request) {
        String requestedDifficulty = request.manufacturingDifficulty();

        if (requestedDifficulty == null
                || requestedDifficulty.isBlank()) {
            throw new IllegalArgumentException("制造难度不能为空");
        }

        return requestedDifficulty.toUpperCase(Locale.ROOT);
    }

    private BigDecimal resolveEfficiency(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> new BigDecimal("3");
            case "MEDIUM" -> new BigDecimal("2");
            case "HARD" -> new BigDecimal("0.6");
            default -> throw new IllegalArgumentException(
                    "不支持的制造难度：" + difficulty
            );
        };
    }

    private void validateInput(
            QuoteRequest request,
            HistoricalQuoteCase referenceCase,
            MaterialChangeAssessment assessment,
            ExplicitCostVerification verification
    ) {
        Objects.requireNonNull(request, "报价请求不能为空");
        Objects.requireNonNull(referenceCase, "历史案例不能为空");
        Objects.requireNonNull(assessment, "LLM差异分析不能为空");
        Objects.requireNonNull(verification, "成本核验结果不能为空");
        Objects.requireNonNull(request.lossRate(), "损耗率不能为空");
        Objects.requireNonNull(
                referenceCase.totalMaterialCost(),
                "K3历史物料成本不能为空"
        );

        if (request.lossRate().signum() < 0
                || request.lossRate().compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException(
                    "损耗率必须是0到1之间的小数，例如5%传入0.05"
            );
        }

        if (referenceCase.totalMaterialCost().signum() < 0) {
            throw new IllegalArgumentException(
                    "K3历史物料成本不能小于0"
            );
        }

        if (assessment.additionalUnitAmount() == null
                || assessment.additionalUnitAmount().signum() < 0) {
            throw new IllegalArgumentException(
                    "非物料类单件固定费用不能小于0"
            );
        }

        if (verification.ambiguous()) {
            throw new IllegalArgumentException(
                    "询价说明中的新增物料成本存在歧义，需要人工复核："
                            + String.join(
                            "；",
                            verification.missingInformation()
                    )
            );
        }
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record MaterialCostBreakdown(
            BigDecimal calculatedMaterialCost,
            BigDecimal materialCostFloor,
            BigDecimal confirmedAddedMaterialCost
    ) {
    }

    /**
     * 报价计算工具的结构化输出。
     */
    public record CalculationResult(
            BigDecimal historicalMaterialCost,
            BigDecimal currentMaterialCost,
            BigDecimal materialCostFloor,
            BigDecimal confirmedAddedMaterialCost,
            boolean materialCostFloorTriggered,
            String manufacturingDifficulty,
            BigDecimal productionEfficiency,
            BigDecimal laborCost,
            BigDecimal additionalUnitAmount,
            BigDecimal samplePrice,
            BigDecimal smallBatchPriceLow,
            BigDecimal smallBatchPriceHigh,
            BigDecimal massProductionPriceLow,
            BigDecimal massProductionPriceHigh
    ) {
    }
}
