package com.cetus.industrialai.quote.service;

import com.cetus.industrialai.quote.model.ExplicitCostVerification;
import com.cetus.industrialai.quote.model.HistoricalMaterialItem;
import com.cetus.industrialai.quote.model.HistoricalQuoteCase;
import com.cetus.industrialai.quote.model.MaterialChangeAssessment;
import com.cetus.industrialai.quote.model.MaterialChangeItem;
import com.cetus.industrialai.quote.model.QuoteRequest;
import com.cetus.industrialai.quote.model.QuoteResult;
import com.cetus.industrialai.quote.calculator.QuoteCalculator;
import com.cetus.industrialai.quote.repository.HistoricalQuoteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 报价Agent人工复核分支的纯Java单元测试。
 *
 * <p>所有外部依赖均使用Mock，不启动Spring、不调用千问，
 * 用于保证自动报价中止时不会丢失已经得到的分析结果。</p>
 */
class QuoteAgentServiceTest {

    @Test
    void shouldKeepPartialAnalysisWhenQuoteFallsBackToManualReview() {
        QuoteRequest request = new QuoteRequest(
                "高速互连线缆A型",
                money("500"),
                "MEDIUM",
                money("0.05"),
                "新增耐高温保护套管，但尚未取得单件价格"
        );

        HistoricalQuoteCase referenceCase = historicalCase();

        MaterialChangeItem addedMaterial = new MaterialChangeItem(
                "ADD",
                null,
                "耐高温保护套管",
                "新增物料但缺少当前价格",
                BigDecimal.ZERO,
                null,
                "UNKNOWN",
                false,
                "询价明确新增耐高温保护套管",
                null
        );

        MaterialChangeAssessment assessment = new MaterialChangeAssessment(
                "MINOR",
                List.of(addedMaterial),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of("新增耐高温保护套管"),
                "MEDIUM",
                true,
                List.of("耐高温保护套管单件价格")
        );

        ExplicitCostVerification verification =
                new ExplicitCostVerification(
                        BigDecimal.ZERO,
                        List.of(),
                        true,
                        List.of("新增耐高温保护套管，但未提供其单件价格")
                );

        HistoricalQuoteRepository historicalQuoteRepository =
                mock(HistoricalQuoteRepository.class);
        QuoteAnalysisService quoteAnalysisService =
                mock(QuoteAnalysisService.class);

        when(historicalQuoteRepository.searchSimilarCases(request))
                .thenReturn(List.of(referenceCase));
        when(quoteAnalysisService.analyze(request, referenceCase))
                .thenReturn(assessment);
        when(quoteAnalysisService.verifyExplicitMaterialCosts(request))
                .thenReturn(verification);

        QuoteWorkflowService service = new QuoteWorkflowService(
                historicalQuoteRepository,
                quoteAnalysisService,
                new QuoteCalculator()
        );

        QuoteResult result = service.generateQuote(request);

        assertEquals("MANUAL_REVIEW", result.decision());
        assertEquals("MINOR", result.changeLevel());
        assertEquals("MEDIUM", result.confidence());
        assertEquals("MEDIUM", result.manufacturingDifficulty());
        assertTrue(result.requiresManualReview());
        assertEquals(assessment.materialChanges(), result.materialChanges());
        assertEquals(assessment.changeReasons(), result.changeReasons());
        assertEquals(money("36.00"), result.historicalMaterialCost());

        assertTrue(
                result.missingInformation().contains(
                        "耐高温保护套管单件价格"
                )
        );
        assertTrue(
                result.missingInformation().contains(
                        "新增耐高温保护套管，但未提供其单件价格"
                )
        );

        assertNull(result.currentMaterialCost());
        assertNull(result.materialCostFloor());
        assertNull(result.confirmedAddedMaterialCost());
        assertNull(result.samplePrice());
        assertFalse(result.materialCostFloorTriggered());
        assertTrue(result.analysisReport().contains("已识别物料变化数量：1"));
    }

    private HistoricalQuoteCase historicalCase() {
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
                money("36.00"),
                List.of(
                        new HistoricalMaterialItem(
                                "MAT-A-CABLE",
                                "高速线材",
                                "500mm",
                                money("0.5"),
                                "米",
                                money("20.00"),
                                money("10.00")
                        ),
                        new HistoricalMaterialItem(
                                "MAT-A-CONN",
                                "标准连接器A",
                                "连接器A",
                                money("2"),
                                "个",
                                money("8.00"),
                                money("16.00")
                        ),
                        new HistoricalMaterialItem(
                                "MAT-A-SHIELD",
                                "普通屏蔽材料",
                                "单层屏蔽",
                                money("1"),
                                "套",
                                money("10.00"),
                                money("10.00")
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

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
