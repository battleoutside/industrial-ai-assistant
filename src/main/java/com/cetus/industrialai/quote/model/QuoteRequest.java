package com.cetus.industrialai.quote.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * 智能报价决策的请求对象。
 *
 * <p>用于接收产品工程师提交的最小报价信息。
 * 报价Agent将根据产品型号、产品长度、产品状态和需求说明，
 * 查询历史报价案例，并评估产品变化程度。</p>
 *
 * <p>材料成本、生产效率和利润系数不由用户直接填写：
 * 材料成本由Agent结合历史案例进行模拟；
 * 生产效率根据产品状态和制造难度匹配；
 * 利润系数由三阶梯报价规则统一管理。</p>
 *
 * @param productModel            产品型号、内部料号或产品描述
 * @param productLengthMm         产品长度，统一使用毫米
 * @param manufacturingDifficulty 制造难度，可选值为
 *                                AI_EVALUATION、EASY、MEDIUM、HARD；
 *                                为空时默认由AI进行评估
 * @param lossRate                损耗率，使用小数表示，例如5%传入0.05
 * @param requirementDescription  产品规格、材料变化、结构变化、
 *                                特殊工艺和其他报价说明
 */
public record QuoteRequest(
        @NotBlank(message = "产品型号不能为空")
        String productModel,

        @NotNull(message = "产品长度不能为空")
        @DecimalMin(
                value = "0",
                inclusive = false,
                message = "产品长度必须大于0"
        )
        BigDecimal productLengthMm,

        @Pattern(
                regexp = "^$|AI_EVALUATION|EASY|MEDIUM|HARD",
                message = "制造难度仅支持AI_EVALUATION、EASY、MEDIUM或HARD"
        )
        String manufacturingDifficulty,

        @NotNull(message = "损耗率不能为空")
        @DecimalMin(
                value = "0",
                message = "损耗率不能小于0"
        )
        @DecimalMax(
                value = "1",
                inclusive = false,
                message = "损耗率必须小于1，例如5%传入0.05"
        )
        BigDecimal lossRate,

        @NotBlank(message = "报价需求说明不能为空")
        String requirementDescription
) {
}