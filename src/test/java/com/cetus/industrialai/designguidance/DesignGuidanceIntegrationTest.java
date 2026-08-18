package com.cetus.industrialai.designguidance;

import com.cetus.industrialai.designguidance.model.DesignGuidanceRequest;
import com.cetus.industrialai.designguidance.model.DesignGuidanceResponse;
import com.cetus.industrialai.designguidance.service.DesignGuidanceService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工程指导真实RAG集成测试。
 *
 * <p>该测试会调用千问Embedding，并在检索到证据时调用千问ChatModel，
 * 因此通过integration标签与默认单元测试隔离。</p>
 */
@Tag("integration")
@SpringBootTest
class DesignGuidanceIntegrationTest {

    @Resource
    private DesignGuidanceService designGuidanceService;

    @Test
    void shouldGenerateGuidanceFromEngineeringKnowledgeBase() {
        DesignGuidanceRequest request = new DesignGuidanceRequest(
                "高速线束组件，使用高速差分连接器，长度1米",
                DesignGuidanceRequest.EngineeringStage.TEST,
                "高频测试中插损、回损不达标，应如何排查？",
                "当前尚未确认测试治具、端接阻抗和屏蔽层处理情况"
        );

        DesignGuidanceResponse response = designGuidanceService.analyze(request);

        assertNotNull(response);
        assertNotNull(response.status());
        assertNotEquals(
                DesignGuidanceResponse.Status.NO_RELEVANT_EVIDENCE,
                response.status()
        );
        assertNotEquals(
                DesignGuidanceResponse.Status.MANUAL_REVIEW,
                response.status()
        );
        assertFalse(response.sources().isEmpty());
        assertTrue(response.evidenceCount() > 0);

        if (response.status() == DesignGuidanceResponse.Status.GUIDANCE_PROVIDED) {
            assertFalse(response.recommendations().isEmpty());
        }
    }
}
