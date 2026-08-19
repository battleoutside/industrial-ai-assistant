package com.cetus.industrialai.designguidance.service;

import com.cetus.industrialai.designguidance.generation.DesignGuidanceGenerator;
import com.cetus.industrialai.designguidance.generation.DesignGuidanceGenerator.GuidanceDraft;
import com.cetus.industrialai.designguidance.generation.DesignGuidanceGenerator.RecommendationDraft;
import com.cetus.industrialai.designguidance.model.DesignGuidanceRequest;
import com.cetus.industrialai.designguidance.model.DesignGuidanceResponse;
import com.cetus.industrialai.designguidance.review.GuidanceGroundednessReviewer;
import com.cetus.industrialai.designguidance.review.GuidanceGroundednessReviewer.ReviewResult;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 工程设计指导编排服务单元测试。
 *
 * <p>使用 Mock 替代 Embedding、千问生成模型和 Groundedness Reviewer，
 * 默认 Maven 测试不会产生任何外部模型调用和 Token 费用。</p>
 */
class DesignGuidanceServiceTest {

    private ContentRetriever contentRetriever;
    private DesignGuidanceGenerator designGuidanceGenerator;
    private GuidanceGroundednessReviewer groundednessReviewer;
    private DesignGuidanceService designGuidanceService;

    @BeforeEach
    void setUp() {
        contentRetriever = mock(ContentRetriever.class);
        designGuidanceGenerator = mock(DesignGuidanceGenerator.class);
        groundednessReviewer = mock(GuidanceGroundednessReviewer.class);

        designGuidanceService = new DesignGuidanceService(
                contentRetriever,
                designGuidanceGenerator,
                groundednessReviewer
        );
    }

    @Test
    void shouldStopBeforeCallingModelsWhenNoEvidenceIsRetrieved() {
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of());

        DesignGuidanceResponse response = designGuidanceService.analyze(sampleRequest());

        assertEquals(
                DesignGuidanceResponse.Status.NO_RELEVANT_EVIDENCE,
                response.status()
        );
        assertEquals(0, response.evidenceCount());
        assertTrue(response.recommendations().isEmpty());

        verifyNoInteractions(designGuidanceGenerator, groundednessReviewer);
    }

    @Test
    void shouldRequireManualReviewWhenKnowledgeRetrievalFails() {
        when(contentRetriever.retrieve(any(Query.class)))
                .thenThrow(new IllegalStateException("模拟知识检索异常"));

        DesignGuidanceResponse response = designGuidanceService.analyze(sampleRequest());

        assertEquals(DesignGuidanceResponse.Status.MANUAL_REVIEW, response.status());
        assertEquals(0, response.evidenceCount());
        assertTrue(response.sources().isEmpty());

        verifyNoInteractions(designGuidanceGenerator, groundednessReviewer);
    }

    @Test
    void shouldKeepRecommendationWhenCitationAndGroundednessBothPass() {
        Content content = mockContent(
                "high-speed-cable-test-troubleshooting.md",
                "# 插入损耗异常\n应先确认校准、参考平面、治具和连接状态。"
        );
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of(content));

        GuidanceDraft draft = new GuidanceDraft(
                "GUIDANCE_PROVIDED",
                "应先排除测试系统影响，再检查样品。",
                List.of(
                        new RecommendationDraft(
                                1,
                                "确认设备校准和测试参考平面。",
                                List.of("S1")
                        ),
                        new RecommendationDraft(
                                2,
                                "采用知识库中不存在的结论。",
                                List.of("S9")
                        )
                ),
                List.of("测试基准未确认时不能直接判定产品失效"),
                List.of("测试频率范围")
        );
        when(designGuidanceGenerator.generate(any(String.class))).thenReturn(draft);

        // S1 合法且建议内容被证据支持，因此 Groundedness 审核通过。
        when(groundednessReviewer.review(anyString()))
                .thenReturn(new ReviewResult(true, "建议内容可由S1直接支持"));

        DesignGuidanceResponse response = designGuidanceService.analyze(sampleRequest());

        assertEquals(
                DesignGuidanceResponse.Status.GUIDANCE_PROVIDED,
                response.status()
        );
        assertEquals(1, response.evidenceCount());
        assertEquals(1, response.recommendations().size());
        assertEquals(List.of("S1"), response.recommendations().getFirst().basisSourceIds());
        assertEquals(
                "high-speed-cable-test-troubleshooting.md",
                response.sources().getFirst().title()
        );
    }

    @Test
    void shouldRemoveRecommendationWhenGroundednessReviewRejectsIt() {
        Content content = mockContent(
                "high-speed-cable-test-troubleshooting.md",
                "# 插入损耗异常\n应先确认测试设备是否完成校准，并检查参考平面。"
        );
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of(content));

        GuidanceDraft draft = new GuidanceDraft(
                "GUIDANCE_PROVIDED",
                "建议先检查测试系统。",
                List.of(
                        new RecommendationDraft(
                                1,
                                "采用SOLT或TRL重新校准测试设备。",
                                List.of("S1")
                        )
                ),
                List.of(),
                List.of()
        );
        when(designGuidanceGenerator.generate(any(String.class))).thenReturn(draft);

        // Citation Validity 可以通过，因为 S1 真实存在；
        // 但 S1 没有支持 SOLT/TRL 具体方法，因此 Groundedness 审核拒绝。
        when(groundednessReviewer.review(anyString()))
                .thenReturn(new ReviewResult(
                        false,
                        "证据只支持确认校准状态，不支持SOLT或TRL具体方法"
                ));

        DesignGuidanceResponse response = designGuidanceService.analyze(sampleRequest());

        assertEquals(DesignGuidanceResponse.Status.NEED_MORE_INFO, response.status());
        assertTrue(response.recommendations().isEmpty());
        assertTrue(
                response.missingInformation().stream()
                        .anyMatch(item -> item.contains("证据一致性审核"))
        );
    }

    @Test
    void shouldRequireManualReviewWhenGroundednessReviewerFails() {
        Content content = mockContent(
                "high-speed-cable-test-troubleshooting.md",
                "# 回波损耗异常\n应检查测试参考平面和治具状态。"
        );
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of(content));

        GuidanceDraft draft = new GuidanceDraft(
                "GUIDANCE_PROVIDED",
                "先确认测试条件。",
                List.of(
                        new RecommendationDraft(
                                1,
                                "检查测试参考平面和治具状态。",
                                List.of("S1")
                        )
                ),
                List.of(),
                List.of()
        );
        when(designGuidanceGenerator.generate(any(String.class))).thenReturn(draft);
        when(groundednessReviewer.review(anyString()))
                .thenThrow(new IllegalStateException("模拟Groundedness审核异常"));

        DesignGuidanceResponse response = designGuidanceService.analyze(sampleRequest());

        assertEquals(DesignGuidanceResponse.Status.MANUAL_REVIEW, response.status());
        assertEquals(1, response.evidenceCount());
        assertEquals(1, response.sources().size());
        assertTrue(response.recommendations().isEmpty());
    }

    @Test
    void shouldRequireManualReviewWhenGroundednessReviewerReturnsNull() {
        Content content = mockContent(
                "high-speed-cable-test-troubleshooting.md",
                "# 回波损耗异常\n应检查测试参考平面和治具状态。"
        );
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of(content));

        GuidanceDraft draft = new GuidanceDraft(
                "GUIDANCE_PROVIDED",
                "先确认测试条件。",
                List.of(
                        new RecommendationDraft(
                                1,
                                "检查测试参考平面和治具状态。",
                                List.of("S1")
                        )
                ),
                List.of(),
                List.of()
        );
        when(designGuidanceGenerator.generate(any(String.class))).thenReturn(draft);
        when(groundednessReviewer.review(anyString())).thenReturn(null);

        DesignGuidanceResponse response = designGuidanceService.analyze(sampleRequest());

        assertEquals(DesignGuidanceResponse.Status.MANUAL_REVIEW, response.status());
        assertEquals(1, response.evidenceCount());
        assertTrue(response.recommendations().isEmpty());
    }

    @Test
    void shouldKeepSourcesAndRequireManualReviewWhenGenerationModelFails() {
        Content content = mockContent(
                "high-speed-cable-test-troubleshooting.md",
                "# 回波损耗异常\n应检查测试参考平面、治具和阻抗不连续位置。"
        );
        when(contentRetriever.retrieve(any(Query.class))).thenReturn(List.of(content));
        when(designGuidanceGenerator.generate(any(String.class)))
                .thenThrow(new IllegalStateException("模拟模型服务异常"));

        DesignGuidanceResponse response = designGuidanceService.analyze(sampleRequest());

        assertEquals(DesignGuidanceResponse.Status.MANUAL_REVIEW, response.status());
        assertEquals(1, response.evidenceCount());
        assertEquals(1, response.sources().size());
        assertTrue(response.recommendations().isEmpty());

        verifyNoInteractions(groundednessReviewer);
    }

    private DesignGuidanceRequest sampleRequest() {
        return new DesignGuidanceRequest(
                "高速差分线束，长度1米",
                DesignGuidanceRequest.EngineeringStage.TEST,
                "高频测试中插损和回损不达标，应如何排查？",
                "尚未确认端接阻抗和治具状态"
        );
    }

    private Content mockContent(String fileName, String text) {
        Content content = mock(Content.class);
        TextSegment textSegment = mock(TextSegment.class);
        Metadata metadata = mock(Metadata.class);

        when(content.textSegment()).thenReturn(textSegment);
        when(textSegment.text()).thenReturn(text);
        when(textSegment.metadata()).thenReturn(metadata);
        when(metadata.getString("file_name")).thenReturn(fileName);

        return content;
    }
}
