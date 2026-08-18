package com.cetus.industrialai.quote.repository;

import com.cetus.industrialai.quote.model.HistoricalMaterialItem;
import com.cetus.industrialai.quote.model.HistoricalQuoteCase;
import com.cetus.industrialai.quote.model.QuoteRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 历史报价案例检索工具。
 *
 * <p>MVP阶段使用内置的完整报价快照模拟K3查询结果。
 * 每条案例包含历史物料明细和K3保存的物料总成本，
 * 后续接入数据库时只需替换数据来源和检索实现。</p>
 */
@Component
public class HistoricalQuoteRepository {

    /**
     * MVP模拟历史案例。
     *
     * <p>以下数据仅用于演示流程，不代表真实企业报价。
     * materialItems的totalCost合计必须等于totalMaterialCost。</p>
     */
    private static final List<HistoricalQuoteCase> HISTORICAL_CASES = List.of(
            new HistoricalQuoteCase(
                    "HQ-001",
                    "高速互连线缆A型",
                    List.of("高速互连", "屏蔽线缆", "连接器A"),
                    new BigDecimal("500"),
                    "STABLE_MASS_PRODUCTION",
                    "MEDIUM",
                    new BigDecimal("1.60"),
                    new BigDecimal("30.00"),
                    new BigDecimal("18.75"),
                    new BigDecimal("0.05"),
                    new BigDecimal("36.00"),
                    List.of(
                            material(
                                    "MAT-A-CABLE",
                                    "高速线材",
                                    "500mm",
                                    "0.5",
                                    "米",
                                    "20.00",
                                    "10.00"
                            ),
                            material(
                                    "MAT-A-CONN",
                                    "标准连接器A",
                                    "连接器A",
                                    "2",
                                    "个",
                                    "8.00",
                                    "16.00"
                            ),
                            material(
                                    "MAT-A-SHIELD",
                                    "普通屏蔽材料",
                                    "单层屏蔽",
                                    "1",
                                    "套",
                                    "10.00",
                                    "10.00"
                            )
                    ),
                    new BigDecimal("120.00"),
                    new BigDecimal("82.00"),
                    new BigDecimal("76.00"),
                    "长度500mm，标准连接器A，普通屏蔽结构，已稳定量产",
                    "V3",
                    LocalDateTime.of(2026, 6, 18, 10, 30)
            ),
            new HistoricalQuoteCase(
                    "HQ-002",
                    "高屏蔽线缆B型",
                    List.of("高屏蔽", "连接器B", "双层屏蔽"),
                    new BigDecimal("1000"),
                    "SAMPLED",
                    "HARD",
                    new BigDecimal("0.75"),
                    new BigDecimal("30.00"),
                    new BigDecimal("40.00"),
                    new BigDecimal("0.08"),
                    new BigDecimal("61.00"),
                    List.of(
                            material(
                                    "MAT-B-CABLE",
                                    "高屏蔽线材",
                                    "1000mm",
                                    "1",
                                    "米",
                                    "20.00",
                                    "20.00"
                            ),
                            material(
                                    "MAT-B-CONN",
                                    "连接器B",
                                    "高可靠连接器B",
                                    "2",
                                    "个",
                                    "12.00",
                                    "24.00"
                            ),
                            material(
                                    "MAT-B-SHIELD",
                                    "双层屏蔽组件",
                                    "双层屏蔽",
                                    "1",
                                    "套",
                                    "17.00",
                                    "17.00"
                            )
                    ),
                    new BigDecimal("210.00"),
                    new BigDecimal("155.00"),
                    new BigDecimal("142.00"),
                    "长度1000mm，连接器B，双层屏蔽结构，已完成样品验证",
                    "V2",
                    LocalDateTime.of(2026, 5, 8, 14, 15)
            ),
            new HistoricalQuoteCase(
                    "HQ-003",
                    "标准信号线缆C型",
                    List.of("标准信号", "普通线缆", "连接器C"),
                    new BigDecimal("300"),
                    "STABLE_MASS_PRODUCTION",
                    "EASY",
                    new BigDecimal("3.50"),
                    new BigDecimal("30.00"),
                    new BigDecimal("8.57"),
                    new BigDecimal("0.03"),
                    new BigDecimal("22.10"),
                    List.of(
                            material(
                                    "MAT-C-CABLE",
                                    "标准信号线材",
                                    "300mm",
                                    "0.3",
                                    "米",
                                    "20.00",
                                    "6.00"
                            ),
                            material(
                                    "MAT-C-CONN",
                                    "连接器C",
                                    "标准连接器C",
                                    "2",
                                    "个",
                                    "6.00",
                                    "12.00"
                            ),
                            material(
                                    "MAT-C-TERM",
                                    "端子",
                                    "标准端子",
                                    "2",
                                    "个",
                                    "1.05",
                                    "2.10"
                            ),
                            material(
                                    "MAT-C-SLEEVE",
                                    "保护套管",
                                    "普通套管",
                                    "1",
                                    "套",
                                    "2.00",
                                    "2.00"
                            )
                    ),
                    new BigDecimal("68.00"),
                    new BigDecimal("45.00"),
                    new BigDecimal("40.00"),
                    "长度300mm，连接器C，普通线材，无特殊加工要求，已稳定量产",
                    "V5",
                    LocalDateTime.of(2026, 7, 2, 9, 0)
            )
    );

    /**
     * 查询最相似的历史报价案例。
     *
     * @param request 当前报价请求
     * @return 按匹配分数从高到低排列的历史案例，最多返回3条
     */
    public List<HistoricalQuoteCase> searchSimilarCases(QuoteRequest request) {
        return HISTORICAL_CASES.stream()
                .filter(quoteCase -> calculateScore(request, quoteCase) > 0)
                .sorted(
                        Comparator.comparingInt(
                                (HistoricalQuoteCase quoteCase) ->
                                        calculateScore(request, quoteCase)
                        ).reversed()
                )
                .limit(3)
                .toList();
    }

    /**
     * 计算当前需求与历史案例的匹配分数。
     *
     * <p>型号匹配权重最高，其次为关键词和长度。
     * 历史产品状态只作为报价快照背景，不参与匹配。</p>
     */
    private int calculateScore(
            QuoteRequest request,
            HistoricalQuoteCase quoteCase
    ) {
        int score = 0;

        String currentModel = normalize(request.productModel());
        String historicalModel = normalize(quoteCase.productModel());

        String searchText = normalize(
                request.productModel() + " "
                        + request.requirementDescription()
        );

        if (!currentModel.isBlank() && currentModel.equals(historicalModel)) {
            score += 6;
        } else if (!currentModel.isBlank()
                && (currentModel.contains(historicalModel)
                || historicalModel.contains(currentModel))) {
            score += 4;
        }

        for (String keyword : quoteCase.keywords()) {
            if (searchText.contains(normalize(keyword))) {
                score += 2;
            }
        }

        score += calculateLengthScore(
                request.productLengthMm(),
                quoteCase.productLengthMm()
        );

        return score;
    }

    /**
     * 根据长度差异计算匹配分数。
     */
    private int calculateLengthScore(
            BigDecimal currentLength,
            BigDecimal historicalLength
    ) {
        if (currentLength == null
                || historicalLength == null
                || historicalLength.signum() <= 0) {
            return 0;
        }

        BigDecimal difference = currentLength
                .subtract(historicalLength)
                .abs();

        if (difference.compareTo(
                historicalLength.multiply(new BigDecimal("0.10"))
        ) <= 0) {
            return 3;
        }

        if (difference.compareTo(
                historicalLength.multiply(new BigDecimal("0.30"))
        ) <= 0) {
            return 2;
        }

        if (difference.compareTo(
                historicalLength.multiply(new BigDecimal("0.50"))
        ) <= 0) {
            return 1;
        }

        return 0;
    }

    /**
     * 创建MVP历史物料明细，减少模拟数据中的重复代码。
     */
    private static HistoricalMaterialItem material(
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
                new BigDecimal(quantity),
                unit,
                new BigDecimal(unitPrice),
                new BigDecimal(totalCost)
        );
    }

    /**
     * 统一处理空值、大小写和首尾空格。
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
