package com.cetus.industrialai.quote.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能报价Agent的最终输出结果。
 *
 * <p>结果同时保留历史真实成本、LLM物料差异、Java成本计算、
 * 保底校验和三阶梯报价，便于前端展示和业务审计。</p>
 *
 * @param decision                   Agent最终决策
 * @param summary                    报价结论摘要
 * @param referenceCase              历史报价完整快照
 * @param changeLevel                产品变化程度
 * @param changeReasons              产品变化原因
 * @param materialChanges            逐项物料变化
 * @param manufacturingDifficulty    当前产品制造难度
 * @param confidence                 LLM分析可信度
 * @param requiresManualReview       是否需要人工复核
 * @param historicalMaterialCost     K3历史单件物料成本
 * @param currentMaterialCost        当前产品采用的单件物料成本
 * @param materialCostFloor          保底规则计算的最低物料成本
 * @param confirmedAddedMaterialCost 已确认的新增物料成本
 * @param materialCostFloorTriggered 是否触发材料成本保底
 * @param additionalUnitAmount       非物料类单件固定费用
 * @param productionEfficiency       标准生产效率，单位为PCS/小时
 * @param laborCost                  当前产品单件人工成本
 * @param samplePrice                10PCS样品单价；样品使用固定成本加成率
 * @param smallBatchPriceLow         1K小批量单价下限
 * @param smallBatchPriceHigh        1K小批量单价上限
 * @param massProductionPriceLow     5K量产单价下限
 * @param massProductionPriceHigh    5K量产单价上限
 * @param risks                      报价风险
 * @param missingInformation         缺失信息
 * @param reviewSuggestion           人工复核建议
 * @param agentSteps                 Agent执行步骤
 * @param analysisReport             面向前端的完整分析报告
 */
public record QuoteResult(
        String decision,
        String summary,
        HistoricalQuoteCase referenceCase,
        String changeLevel,
        List<String> changeReasons,
        List<MaterialChangeItem> materialChanges,
        String manufacturingDifficulty,
        String confidence,
        boolean requiresManualReview,
        BigDecimal historicalMaterialCost,
        BigDecimal currentMaterialCost,
        BigDecimal materialCostFloor,
        BigDecimal confirmedAddedMaterialCost,
        boolean materialCostFloorTriggered,
        BigDecimal additionalUnitAmount,
        BigDecimal productionEfficiency,
        BigDecimal laborCost,
        BigDecimal samplePrice,
        BigDecimal smallBatchPriceLow,
        BigDecimal smallBatchPriceHigh,
        BigDecimal massProductionPriceLow,
        BigDecimal massProductionPriceHigh,
        List<String> risks,
        List<String> missingInformation,
        String reviewSuggestion,
        List<String> agentSteps,
        String analysisReport
) {
}
