package com.cetus.industrialai.quote.model;

import java.math.BigDecimal;

/**
 * 历史报价中的单项物料成本快照。
 *
 * <p>该数据应来自K3报价系统或报价数据库，并保留报价发生时的
 * 物料数量、单价和单件产品分摊成本，避免使用当前价格覆盖历史价格。</p>
 *
 * @param materialCode  K3物料编码
 * @param materialName  物料名称
 * @param specification 物料规格
 * @param quantity      单件产品使用数量
 * @param unit          计量单位
 * @param unitPrice     历史报价时的物料单价
 * @param totalCost     该物料在单件产品中的历史成本
 */
public record HistoricalMaterialItem(
        String materialCode,
        String materialName,
        String specification,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal totalCost
) {
}
