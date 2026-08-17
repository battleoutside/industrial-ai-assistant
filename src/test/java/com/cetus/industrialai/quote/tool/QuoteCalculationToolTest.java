package com.cetus.industrialai.quote.tool;

import com.cetus.industrialai.quote.model.ExplicitCostVerification;
import com.cetus.industrialai.quote.model.HistoricalMaterialItem;
import com.cetus.industrialai.quote.model.HistoricalQuoteCase;
import com.cetus.industrialai.quote.model.MaterialChangeAssessment;
import com.cetus.industrialai.quote.model.MaterialChangeItem;
import com.cetus.industrialai.quote.model.QuoteRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 报价计算工具的纯Java单元测试。
 *
 * <p>测试不启动Spring、不连接千问、不加载MCP，也不会消耗Token。
 * 重点验证材料成本保底、样品单值报价和异常拦截。</p>
 */
class QuoteCalculationToolTest {

    private final QuoteCalculationTool calculationTool =
            new QuoteCalculationTool();

    /**
     * 第一次LLM只识别出5元新增成本，独立核验识别出8元时，
     * Java必须将材料成本从41元抬升至44元。
     */
    @Test
    void shouldTriggerMaterialCostFloorWhenFirstAnalysisMissesCost() {
        MaterialChangeAssessment assessment = new MaterialChangeAssessment(
                "MINOR",
                List.of(
                        unchanged("MAT-A-CABLE", "高速线材", "10.00"),
                        unchanged("MAT-A-CONN", "标准连接器A", "16.00"),
                        unchanged("MAT-A-SHIELD", "普通屏蔽材料", "10.00"),
                        added("新增物料A", "5.00", "新增物料A每件5元")
                ),
                money("5.00"),
                BigDecimal.ZERO,
                List.of("新增物料A"),
                "HIGH",
                false,
                List.of()
        );

        ExplicitCostVerification verification =
                new ExplicitCostVerification(
                        money("8.00"),
                        List.of(
                                "新增物料A每件5元",
                                "新增物料B每件3元"
                        ),
                        false,
                        List.of()
                );

        QuoteCalculationTool.CalculationResult result =
                calculationTool.calculate(
                        request(),
                        historicalCase(money("36.00")),
                        assessment,
                        verification
                );

        assertEquals(money("36.00"), result.historicalMaterialCost());
        assertEquals(money("44.00"), result.materialCostFloor());
        assertEquals(money("44.00"), result.currentMaterialCost());
        assertEquals(money("8.00"), result.confirmedAddedMaterialCost());
        assertTrue(result.materialCostFloorTriggered());

        // 样品利润系数固定，只返回一个样品单价。
        assertEquals(money("123.90"), result.samplePrice());
        assertEquals(money("86.73"), result.smallBatchPriceLow());
        assertEquals(money("89.83"), result.smallBatchPriceHigh());
        assertEquals(money("83.63"), result.massProductionPriceLow());
        assertEquals(money("86.73"), result.massProductionPriceHigh());
    }

    /**
     * 新增物料缺少当前成本时，计算工具必须拒绝自动报价。
     */
    @Test
    void shouldRejectUnconfirmedAddedMaterialCost() {
        MaterialChangeItem unconfirmedItem = new MaterialChangeItem(
                "ADD",
                null,
                "耐高温保护套管",
                "新增物料但没有价格",
                BigDecimal.ZERO,
                null,
                "UNKNOWN",
                false,
                "询价提到新增耐高温保护套管",
                null
        );

        MaterialChangeAssessment assessment = new MaterialChangeAssessment(
                "MINOR",
                List.of(unconfirmedItem),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of("新增物料缺少价格"),
                "MEDIUM",
                true,
                List.of("耐高温保护套管单件价格")
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculationTool.calculate(
                        request(),
                        historicalCase(money("36.00")),
                        assessment,
                        emptyVerification()
                )
        );

        assertTrue(
                exception.getMessage().contains("缺少当前版本可核验的用户明确成本")
        );
    }

    /**
     * K3物料明细合计与物料总成本不一致时，不允许继续报价。
     */
    @Test
    void shouldRejectInconsistentHistoricalMaterialSnapshot() {
        MaterialChangeAssessment assessment = new MaterialChangeAssessment(
                "NONE",
                List.of(
                        unchanged("MAT-A-CABLE", "高速线材", "10.00"),
                        unchanged("MAT-A-CONN", "标准连接器A", "16.00"),
                        unchanged("MAT-A-SHIELD", "普通屏蔽材料", "10.00")
                ),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                "HIGH",
                false,
                List.of()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> calculationTool.calculate(
                        request(),
                        historicalCase(money("35.00")),
                        assessment,
                        emptyVerification()
                )
        );

        assertEquals(
                "K3历史物料总成本与物料明细合计不一致",
                exception.getMessage()
        );
    }

    private QuoteRequest request() {
        return new QuoteRequest(
                "高速互连线缆A型",
                money("500"),
                "MEDIUM",
                money("0.05"),
                "新增物料A每件5元，新增物料B每件3元"
        );
    }

    private HistoricalQuoteCase historicalCase(BigDecimal totalMaterialCost) {
        return new HistoricalQuoteCase(
                "HQ-001",
                "高速互连线缆A型",
                List.of("高速互连", "屏蔽线缆", "连接器A"),
                money("500"),
                "STABLE_MASS_PRODUCTION",
                "MEDIUM",
                money("1.60"),
                money("30.00"),
                money("18.75"),
                money("0.05"),
                totalMaterialCost,
                List.of(
                        historicalMaterial(
                                "MAT-A-CABLE",
                                "高速线材",
                                "500mm",
                                "0.5",
                                "米",
                                "20.00",
                                "10.00"
                        ),
                        historicalMaterial(
                                "MAT-A-CONN",
                                "标准连接器A",
                                "连接器A",
                                "2",
                                "个",
                                "8.00",
                                "16.00"
                        ),
                        historicalMaterial(
                                "MAT-A-SHIELD",
                                "普通屏蔽材料",
                                "单层屏蔽",
                                "1",
                                "套",
                                "10.00",
                                "10.00"
                        )
                ),
                money("120.00"),
                money("82.00"),
                money("76.00"),
                "长度500mm，标准连接器A，普通屏蔽结构，已稳定量产",
                "V3",
                LocalDateTime.of(2026, 6, 18, 10, 30)
        );
    }

    private HistoricalMaterialItem historicalMaterial(
            String code,
            String name,
            String specification,
            String quantity,
            String unit,
            String unitPrice,
            String totalCost
    ) {
        return new HistoricalMaterialItem(
                code,
                name,
                specification,
                money(quantity),
                unit,
                money(unitPrice),
                money(totalCost)
        );
    }

    private MaterialChangeItem unchanged(
            String code,
            String name,
            String cost
    ) {
        return new MaterialChangeItem(
                "UNCHANGED",
                code,
                name,
                "历史物料保持不变",
                money(cost),
                money(cost),
                "HISTORICAL_CASE",
                true,
                "引用K3历史物料成本",
                null
        );
    }

    private MaterialChangeItem added(
            String name,
            String currentCost,
            String sourceExcerpt
    ) {
        return new MaterialChangeItem(
                "ADD",
                null,
                name,
                "用户明确新增物料",
                BigDecimal.ZERO,
                money(currentCost),
                "USER_EXPLICIT",
                true,
                "询价明确给出新增物料成本",
                sourceExcerpt
        );
    }

    private ExplicitCostVerification emptyVerification() {
        return new ExplicitCostVerification(
                BigDecimal.ZERO,
                List.of(),
                false,
                List.of()
        );
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
