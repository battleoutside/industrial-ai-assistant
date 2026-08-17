package com.cetus.industrialai.quote.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * LLM对询价产品与历史产品的结构化差异分析结果。
 *
 * <p>LLM负责识别每项物料属于新增、删除、替换还是保持不变，
 * 并标记成本来源和证据；Java根据这些明细计算最终成本和报价。</p>
 *
 * @param changeLevel                    产品总体变化程度
 * @param materialChanges                逐项物料变化
 * @param explicitAdditionalMaterialCost 用户明确声明的新增物料成本合计
 * @param additionalUnitAmount           非物料类的单件固定费用
 * @param changeReasons                  产品变化原因
 * @param confidence                     分析可信度
 * @param requiresManualReview           是否需要人工复核
 * @param missingInformation             当前缺失的信息
 */
public record MaterialChangeAssessment(
        String changeLevel,
        List<MaterialChangeItem> materialChanges,
        BigDecimal explicitAdditionalMaterialCost,
        BigDecimal additionalUnitAmount,
        List<String> changeReasons,
        String confidence,
        boolean requiresManualReview,
        List<String> missingInformation
) {
}
