package com.cetus.industrialai.quote.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 从K3或报价数据库读取的历史报价完整快照。
 *
 * <p>历史物料成本、物料明细和历史报价均直接取自原报价记录，
 * 不再通过历史售价反推材料成本。</p>
 *
 * @param caseId                  历史案例编号
 * @param productModel            历史产品型号或内部料号
 * @param keywords                历史案例检索关键词
 * @param productLengthMm         历史产品长度，单位为毫米
 * @param productionStage         历史产品状态，仅作为背景数据
 * @param manufacturingDifficulty 历史制造难度
 * @param productionEfficiency    历史生产效率，单位为PCS/小时
 * @param hourlyLaborCost         历史小时人工成本
 * @param unitLaborCost           历史单件人工成本
 * @param lossRate                历史报价采用的损耗率
 * @param totalMaterialCost       K3保存的历史单件物料总成本
 * @param materialItems           历史物料成本明细
 * @param sampleUnitPrice         10PCS样品历史单价
 * @param smallBatchUnitPrice     1K小批量历史单价
 * @param massProductionUnitPrice 5K量产历史单价
 * @param requirementSummary      历史规格、材料和工艺摘要
 * @param quoteVersion            K3报价版本
 * @param quotedAt                历史报价时间
 */
public record HistoricalQuoteCase(
        String caseId,
        String productModel,
        List<String> keywords,
        BigDecimal productLengthMm,
        String productionStage,
        String manufacturingDifficulty,
        BigDecimal productionEfficiency,
        BigDecimal hourlyLaborCost,
        BigDecimal unitLaborCost,
        BigDecimal lossRate,
        BigDecimal totalMaterialCost,
        List<HistoricalMaterialItem> materialItems,
        BigDecimal sampleUnitPrice,
        BigDecimal smallBatchUnitPrice,
        BigDecimal massProductionUnitPrice,
        String requirementSummary,
        String quoteVersion,
        LocalDateTime quotedAt
) {
}
