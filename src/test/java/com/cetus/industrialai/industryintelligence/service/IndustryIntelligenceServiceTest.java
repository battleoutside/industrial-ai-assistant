package com.cetus.industrialai.industryintelligence.service;

import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent;
import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent.FindingDraft;
import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent.ResearchDraft;
import com.cetus.industrialai.industryintelligence.agent.IndustryIntelligenceAgent.SourceDraft;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceRequest;
import com.cetus.industrialai.industryintelligence.model.IndustryIntelligenceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 行业信息咨询编排服务单元测试。
 *
 * <p>使用 Mock 替代真实千问和 MCP Web Search，
 * 默认 Maven 测试不会发起外部网络请求，也不会产生 Token 费用。</p>
 */
class IndustryIntelligenceServiceTest {

    private IndustryIntelligenceAgent industryIntelligenceAgent;
    private IndustryIntelligenceService industryIntelligenceService;

    @BeforeEach
    void setUp() {
        industryIntelligenceAgent = mock(IndustryIntelligenceAgent.class);
        industryIntelligenceService = new IndustryIntelligenceService(
                industryIntelligenceAgent
        );
    }

    @Test
    void shouldReturnValidatedFindingsAndRenumberSources() {
        ResearchDraft draft = new ResearchDraft(
                "SUCCESS",
                "近期高速互连公开信息主要集中在更高速率和高密度方向。",
                List.of(
                        new FindingDraft(
                                "高速互连产品持续更新",
                                "多家厂商近期仍在发布高速互连相关产品。",
                                List.of("TEMP-A", "FAKE")
                        )
                ),
                List.of(
                        new SourceDraft(
                                "TEMP-A",
                                "Vendor product update",
                                "https://example.com/product-update",
                                "2026-08-01"
                        )
                )
        );
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenReturn(draft);

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(IndustryIntelligenceResponse.Status.SUCCESS, response.status());
        assertEquals(1, response.sourceCount());
        assertEquals("S1", response.sources().getFirst().sourceId());
        assertEquals(List.of("S1"), response.findings().getFirst().sourceIds());
        assertEquals(
                "本次公开信息调研形成1条可核验发现，主要涉及：高速互连产品持续更新。",
                response.summary()
        );
    }

    @Test
    void shouldKeepDraftSummaryWhenNoPostValidationAdjustmentOccurs() {
        String originalSummary = "近期公开信息显示高速互连产品持续演进。";
        ResearchDraft draft = new ResearchDraft(
                "SUCCESS",
                originalSummary,
                List.of(
                        new FindingDraft(
                                "高速互连产品更新",
                                "公开资料显示相关产品持续更新。",
                                List.of("S1")
                        )
                ),
                List.of(
                        new SourceDraft(
                                "S1",
                                "Vendor product update",
                                "https://example.com/product-update",
                                ""
                        )
                )
        );
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenReturn(draft);

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(IndustryIntelligenceResponse.Status.SUCCESS, response.status());
        assertEquals(originalSummary, response.summary());
    }

    @Test
    void shouldRebuildSummaryWhenInvalidSourceIsFiltered() {
        ResearchDraft draft = new ResearchDraft(
                "SUCCESS",
                "摘要中提到了IEC 63066和高速互连新品。",
                List.of(
                        new FindingDraft(
                                "IEC标准动态",
                                "该发现对应的搜索结果没有可用URL。",
                                List.of("TEMP-IEC")
                        ),
                        new FindingDraft(
                                "高速互连新品",
                                "厂商公开页面展示了高速互连产品。",
                                List.of("TEMP-VALID")
                        )
                ),
                List.of(
                        new SourceDraft(
                                "TEMP-IEC",
                                "IEC 63066相关动态",
                                "",
                                "2026-07-24"
                        ),
                        new SourceDraft(
                                "TEMP-VALID",
                                "High-speed connector product",
                                "https://example.com/high-speed-connector",
                                ""
                        )
                )
        );
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenReturn(draft);

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(IndustryIntelligenceResponse.Status.SUCCESS, response.status());
        assertEquals(1, response.findings().size());
        assertEquals(1, response.sourceCount());
        assertEquals(
                "本次公开信息调研形成1条可核验发现，主要涉及：高速互连新品。",
                response.summary()
        );
        assertFalse(response.summary().contains("IEC 63066"));
    }

    @Test
    void shouldFilterInvalidAndDuplicateSources() {
        ResearchDraft draft = new ResearchDraft(
                "SUCCESS",
                "公开信息已整理。",
                List.of(
                        new FindingDraft(
                                "有效发现",
                                "该发现引用两个临时编号，但二者对应同一公开网页。",
                                List.of("A", "B")
                        )
                ),
                List.of(
                        new SourceDraft("A", "有效来源", "https://example.com/a", ""),
                        new SourceDraft("B", "重复来源", "https://example.com/a", ""),
                        new SourceDraft("C", "非法来源", "javascript:alert(1)", "")
                )
        );
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenReturn(draft);

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(IndustryIntelligenceResponse.Status.SUCCESS, response.status());
        assertEquals(1, response.sourceCount());
        assertEquals("https://example.com/a", response.sources().getFirst().url());
        assertEquals(List.of("S1"), response.findings().getFirst().sourceIds());
        assertEquals(
                "本次公开信息调研形成1条可核验发现，主要涉及：有效发现。",
                response.summary()
        );
    }

    @Test
    void shouldReturnNoRelevantInformationWhenNoValidSourceExists() {
        ResearchDraft draft = new ResearchDraft(
                "SUCCESS",
                "模型声称存在行业发现。",
                List.of(
                        new FindingDraft(
                                "无法核验的发现",
                                "没有合法公开URL支持。",
                                List.of("S1")
                        )
                ),
                List.of(
                        new SourceDraft("S1", "无效来源", "not-a-url", "")
                )
        );
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenReturn(draft);

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(
                IndustryIntelligenceResponse.Status.NO_RELEVANT_INFORMATION,
                response.status()
        );
        assertEquals(0, response.sourceCount());
        assertTrue(response.findings().isEmpty());
    }

    @Test
    void shouldReturnNoRelevantInformationWhenAllFindingCitationsAreInvalid() {
        ResearchDraft draft = new ResearchDraft(
                "SUCCESS",
                "搜索到了公开信息，但发现没有有效引用。",
                List.of(
                        new FindingDraft(
                                "错误引用",
                                "引用了不存在的来源编号。",
                                List.of("S9")
                        )
                ),
                List.of(
                        new SourceDraft(
                                "S1",
                                "有效来源",
                                "https://example.com/source",
                                ""
                        )
                )
        );
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenReturn(draft);

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(
                IndustryIntelligenceResponse.Status.NO_RELEVANT_INFORMATION,
                response.status()
        );
        assertTrue(response.findings().isEmpty());
        assertEquals(0, response.sourceCount());
    }

    @Test
    void shouldReturnSearchFailedWhenAgentThrowsException() {
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenThrow(new IllegalStateException("模拟MCP搜索异常"));

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(IndustryIntelligenceResponse.Status.SEARCH_FAILED, response.status());
        assertEquals(0, response.sourceCount());
        assertTrue(response.sources().isEmpty());
    }

    @Test
    void shouldReturnSearchFailedWhenAgentReturnsNull() {
        when(industryIntelligenceAgent.research(sampleRequest().question()))
                .thenReturn(null);

        IndustryIntelligenceResponse response = industryIntelligenceService.analyze(
                sampleRequest()
        );

        assertEquals(IndustryIntelligenceResponse.Status.SEARCH_FAILED, response.status());
        assertTrue(response.findings().isEmpty());
    }

    private IndustryIntelligenceRequest sampleRequest() {
        return new IndustryIntelligenceRequest(
                "最近高速连接器行业有哪些值得关注的公开产品和技术动态？"
        );
    }
}
