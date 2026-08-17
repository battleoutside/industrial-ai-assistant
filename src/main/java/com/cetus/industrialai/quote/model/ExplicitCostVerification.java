package com.cetus.industrialai.quote.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 独立LLM核验得到的明确新增物料成本。
 *
 * <p>该对象不判断完整产品差异，只负责再次检查询价说明中
 * 是否存在“新增材料每件增加多少元”等明确成本信息。</p>
 *
 * @param minimumExplicitAddedMaterialCost 明确新增物料成本合计
 * @param evidence                         来自询价说明的原文证据
 * @param ambiguous                        金额含义是否存在歧义
 * @param missingInformation               需要人工补充的信息
 */
public record ExplicitCostVerification(
        BigDecimal minimumExplicitAddedMaterialCost,
        List<String> evidence,
        boolean ambiguous,
        List<String> missingInformation
) {
}
