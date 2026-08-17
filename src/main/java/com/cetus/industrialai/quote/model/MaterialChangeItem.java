package com.cetus.industrialai.quote.model;

import java.math.BigDecimal;

/**
 * LLM识别出的单项物料变化。
 *
 * <p>changeType允许值：UNCHANGED、ADD、REMOVE、REPLACE、
 * QUANTITY、SPEC_CHANGE、UNKNOWN。</p>
 *
 * <p>costSource允许值：HISTORICAL_CASE、K3_CURRENT、
 * USER_EXPLICIT、UNKNOWN。</p>
 *
 * @param changeType     变化类型
 * @param materialCode   历史物料编码；新增且无编码时可以为null
 * @param materialName   物料名称
 * @param description    变化说明
 * @param historicalCost 变化前的单件物料成本
 * @param currentCost    变化后的单件物料成本
 * @param costSource     当前成本的数据来源
 * @param costConfirmed  当前成本是否已经获得可靠依据
 * @param evidence       支撑变化判断的解释性依据
 * @param sourceExcerpt  USER_EXPLICIT成本对应的询价原文片段；
 *                       其他成本来源可以为null
 */
public record MaterialChangeItem(
        String changeType,
        String materialCode,
        String materialName,
        String description,
        BigDecimal historicalCost,
        BigDecimal currentCost,
        String costSource,
        boolean costConfirmed,
        String evidence,
        String sourceExcerpt
) {
}
